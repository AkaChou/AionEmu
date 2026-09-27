package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定 28932 的领奖行合同：客户端 quest_q28932.html 共 2 行（第 1 行消灭 Dreadgion 德拉克忍者、
 * 第 2 行向 DF6_Olivia_E 报告），`started` 必须显式声明第 1 行（var0=0），`reward` 投影领奖行
 * （var0=1、var1=1）。迁移把魔族侧 `started` 写成自闭合节点（无任何投影），审计因此判
 * INTERIOR_GAP/ROW_WITHOUT_STATE，与已对齐的天族镜像 18932（started 声明 var0=0）不对称。
 * 修复只声明行号 var0，不声明 var1 击杀计数，保证“满计数恢复路线”（var1&gt;=1 的 QUEST_SELECT /
 * SELECT_QUEST_REWARD 对话）继续可匹配；本测试同时把这条边界锁进回归。
 * Locks the 28932 reward-row contract: quest_q28932.html has two rows (kill the Dreadgion Drakan, then
 * report to DF6_Olivia_E) and the START state must declare row 1 (var0=0) while REWARD projects the reward
 * row (var0=1, var1=1). The migration left the Asmodian `started` node self-closing, so the audit reported
 * INTERIOR_GAP/ROW_WITHOUT_STATE unlike the aligned Elyos mirror 18932 (started declares var0=0). The fix
 * declares only the row number so the var1&gt;=1 saturated-recovery dialogs keep matching; this test pins
 * that boundary too.
 */
class Quest28932RewardRowContractTest {

	private static final int[] REPORT_NPCS = {806261, 806260};
	private static final int TARGET = 243953;

	/** 网格形判据：无 started 节点（真端击杀网格行，DD 车道）。 / Grid-shape discriminator. */
	private static boolean isGrid(QuestDefinition definition) {
		return definition.nodes().stream().noneMatch(candidate -> "started".equals(candidate.label()));
	}

	@Test
	void startStateDeclaresTheFirstJournalRowWithoutPinningTheKillCounter() throws Exception {
		/* P5-1：28932 已由真端击杀网格驱动（a0/a1，var0 = 计数），首行状态 = 网格零段；
		   reward 投影 = 饱和计数 1。18932 若仍为双变量形则保留原行号断言。 */
		QuestDefinition asmodian = definition(28932).definition();
		assertEquals(Map.of("var0", 0), node(asmodian, "a0").projection().variables());
		assertEquals(Map.of("var0", 1), node(asmodian, "reward").projection().variables());
		if (isGrid(definition(18932).definition())) {
			assertEquals(Map.of("var0", 0), node(definition(18932).definition(), "a0").projection().variables());
		} else {
			assertEquals(Map.of("var0", 0),
				node(definition(18932).definition(), "started").projection().variables());
		}
	}

	@Test
	void singleKillAdvancesToTheRewardRow() throws Exception {
		/* 网格形：击杀边 a0→a1 无条件（PACKET_ONLY），满段即报告门控。 */
		CompiledQuestDefinition compiled = definition(28932);
		List<QuestTransition> kills = compiled.definition().transitions().stream()
			.filter(candidate -> "a0".equals(candidate.sourceNode()) && "a1".equals(candidate.targetNode()))
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpcSet
				|| candidate.event() instanceof QuestEvent.KillNpc)
			.toList();
		assertEquals(1, kills.size(), "28932 kill edge a0->a1");
		assertEquals(List.of(), kills.getFirst().conditions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			kills.getFirst().afterCommit());

		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.START, Map.of("var0", 0)), kills.getFirst()).orElseThrow();
		assertEquals(QuestStatus.START, plan.nextStatus());
		assertEquals(Map.of("var0", 1), unpack(compiled, plan));
	}

	@Test
	void saturatedRecoveryDialogsStillMatchTheDeclaredRowState() throws Exception {
		/* P0-2 规范形交付协议：满段 a1 的 QUEST_SELECT 直翻领奖（LEVEL + 分档奖励窗）；
		   未满段 a0 无 QUEST_SELECT/1009 报告通道。交付 NPC = 生产定义挂满段交付边的 NPC。 */
		CompiledQuestDefinition compiled = definition(28932);
		QuestDefinition definition = compiled.definition();
		int reportNpc = definition.transitions().stream()
			.filter(candidate -> "a1".equals(candidate.sourceNode()) && "reward".equals(candidate.targetNode())
				&& candidate.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id())
			.mapToInt(candidate -> ((QuestEvent.TalkToNpc) candidate.event()).npcId())
			.findFirst().orElseThrow();
		QuestEvent event = new QuestEvent.TalkToNpc(reportNpc, QuestDialogAction.QUEST_SELECT.id());
		QuestTransition deliver = definition.transitions().stream()
			.filter(candidate -> "a1".equals(candidate.sourceNode()) && "reward".equals(candidate.targetNode()))
			.filter(candidate -> event.equals(candidate.event()))
			.findFirst().orElseThrow(
				() -> new AssertionError("missing saturated delivery route " + reportNpc));
		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.START, Map.of("var0", 1)), deliver).orElseThrow(
				() -> new AssertionError("unplannable saturated delivery route " + reportNpc));
		assertEquals(QuestStatus.REWARD, plan.nextStatus());
		assertEquals(Map.of("var0", 1), unpack(compiled, plan));
		assertTrue(definition.transitions().stream().noneMatch(candidate ->
			"a0".equals(candidate.sourceNode()) && candidate.event() instanceof QuestEvent.TalkToNpc talkRoute
				&& talkRoute.dialogId() != null
				&& (talkRoute.dialogId() == QuestDialogAction.QUEST_SELECT.id()
					|| talkRoute.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())),
			() -> "a0 不得保留报告通道路由");
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label())).findFirst().orElseThrow();
	}

	private static Map<String, Integer> unpack(CompiledQuestDefinition definition, QuestMutationPlan plan) {
		return definition.definition().progressLayout().unpack(plan.nextPackedVariables());
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			Map<String, Integer> variables) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), status,
			definition.definition().progressLayout().pack(packedVariables), Map.of(), Map.of(),
			true, true, 0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition definition(int questId) {
		// 28932 系已由真端表驱动（退役），改从生产视图取定义。
		// The 28932 family is retail-driven since retirement; load via the production view.
		return ProductionQuestDefinitions.definition(questId);
	}
}
