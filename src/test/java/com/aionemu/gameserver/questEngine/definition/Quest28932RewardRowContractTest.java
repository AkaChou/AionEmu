package com.aionemu.gameserver.questEngine.definition;

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

	@Test
	void startStateDeclaresTheFirstJournalRowWithoutPinningTheKillCounter() throws Exception {
		assertEquals(Map.of("var0", 0), node(definition(28932).definition(), "started").projection().variables());
		assertEquals(Map.of("var0", 1, "var1", 1),
			node(definition(28932).definition(), "reward").projection().variables());
		/* 天/魔镜像必须声明同一行状态。 / Both race sides must declare the same row state. */
		assertEquals(Map.of("var0", 0), node(definition(18932).definition(), "started").projection().variables());
	}

	@Test
	void singleKillAdvancesToTheRewardRow() throws Exception {
		CompiledQuestDefinition compiled = definition(28932);
		QuestTransition kill = compiled.definition().transitions().stream()
			.filter(candidate -> "started".equals(candidate.sourceNode()))
			.filter(candidate -> "reward".equals(candidate.targetNode()))
			/* 28932 用 &lt;kill-npc npc-ids&gt;（KillNpcSet），18932 用单 npc 形式（KillNpc）。 */
			/* 28932 declares &lt;kill-npc npc-ids&gt; (KillNpcSet) while 18932 uses the single-npc form. */
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpcSet
				|| candidate.event() instanceof QuestEvent.KillNpc)
			.findFirst().orElseThrow();
		assertEquals(List.of(new QuestAction.SetVariable("var0", 1), new QuestAction.SetVariable("var1", 1)),
			kill.actions());

		QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
			snapshot(compiled, QuestStatus.START, Map.of("var0", 0, "var1", 0)), kill).orElseThrow();
		assertEquals(QuestStatus.REWARD, plan.nextStatus());
		assertEquals(Map.of("var0", 1, "var1", 1), unpack(compiled, plan));
	}

	@Test
	void saturatedRecoveryDialogsStillMatchTheDeclaredRowState() throws Exception {
		CompiledQuestDefinition compiled = definition(28932);
		Map<String, Integer> saturated = Map.of("var0", 0, "var1", 1);
		for (int npcId : REPORT_NPCS) {
			for (QuestDialogAction action : List.of(QuestDialogAction.QUEST_SELECT,
					QuestDialogAction.SELECT_QUEST_REWARD)) {
				QuestEvent event = new QuestEvent.TalkToNpc(npcId, action.id());
				QuestTransition recovery = compiled.definition().transitions().stream()
					.filter(candidate -> "started".equals(candidate.sourceNode()))
					.filter(candidate -> "reward".equals(candidate.targetNode()))
					.filter(candidate -> event.equals(candidate.event()))
					.findFirst().orElseThrow(
						() -> new AssertionError("missing saturated recovery route " + npcId + " + " + action));
				QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
					snapshot(compiled, QuestStatus.START, saturated), recovery).orElseThrow(
						() -> new AssertionError("unplannable saturated recovery route " + npcId + " + " + action));
				assertEquals(QuestStatus.REWARD, plan.nextStatus());
				assertEquals(Map.of("var0", 1, "var1", 1), unpack(compiled, plan));
			}
		}
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

	private static CompiledQuestDefinition definition(int questId) throws Exception {
		try (InputStream input = Quest28932RewardRowContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
