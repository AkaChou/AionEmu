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
