package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 按独立计数合同验证编译 IR：未完成不能领奖，满计数可报告，包括 START 恢复路线。
 * Checks compiled IR against independent progress contracts: no early rewards, including guarded START recovery.
 */
class QuestPrematureRewardRouteExclusionTest {
	// 阈值来自逐项 XML/911440146 handler 对照，不从待测路由或节点反推。
	// Thresholds come from XML/911440146 handler review, never from the route under test.
	private static Stream<RewardCase> rewardCases() {
		return Stream.of(
			/* P0-2 规范形：1347 满段交付动作 = QUEST_SELECT（1009 中转删除）。
			   Canonical since P0-2: 1347's full-node delivery action is QUEST_SELECT (no 1009 hop). */
			stageOnQuestSelect(1347, 7, 3, 7, "a7b3", 203966),
			/* P5-1 网格族 + P0-2 DD 切片（2026-09-26）规范形：16837/16986/16988/19631/19633/19638/
			   19642/28932/28972-4/25640/25698/28743 由真端击杀网格驱动（var0 = 计数），满段 a<required>
			   的 QUEST_SELECT 直接翻 REWARD 并按档位查表下发奖励窗；未满段零对话路由，交付 NPC 唯一
			  （接取变体不承担交付）。
			   Grid family in the canonical shape since the P0-2 DD slice: the full segment's QUEST_SELECT
			   flips REWARD with the tiered reward window, incomplete segments carry no dialog routes, and
			   only the hand-in npc delivers. */
			grid(16837, 1, 806568),
			grid(16986, 1, 804864),
			grid(16988, 5, 804865),
			grid(19631, 10, 800411),
			grid(19633, 10, 205304),
			grid(19638, 10, 799022),
			grid(19642, 10, 798926),
			/* S2 链式规范形：2569 双块行由规范段接管，交付 = NPC_REPORT 块 source（s1）的
			   QUEST_SELECT(31) 空门直翻领奖态（无 item_check ⇒ 规范门为空）。
			   S2 canonical chain delivery: the report block source's empty-gated QUEST_SELECT(31). */
			stageOnQuestSelect(2569, 1, -1, 1, "s1", 204768),
			grid(28743, 1, 804732),
			grid(28932, 1, 806260),
			counter(28951, 25, 209743, 804738),
			stage(28952, 4, -1, 4, "k4", 209743, 804738),
			grid(28972, 6, 805216),
			grid(28973, 6, 805217),
			grid(28974, 6, 805218),
			// 25640/25698 为“满计数恢复报告”形态的 A03 碎片任务，与 Quest25640/25698ClientDialogAlignmentTest 配对。
			// The 25640/25698 pair are full-count recovery-report owners; paired with their dialog alignment gates.
			grid(25640, 30, 806101),
			grid(25698, 5, 806804));
	}

	@ParameterizedTest
	@MethodSource("rewardCases")
	void incompleteProgressCannotRewardAndCompletedProgressCanReport(RewardCase contract) {
		CompiledQuestDefinition compiled = load(contract.questId());
		for (Map<String, Integer> variables : contract.incomplete()) {
			assertNoPrematureReward(compiled, variables);
			// 历史脏阶段标记也不能绕过真实计数。 / A stale stage flag must not bypass the live counter.
			if (contract.recovery()) {
				assertNoPrematureReward(compiled, Map.of("var0", 1, "var1", variables.get("var1")));
			}
		}
		assertCompletedReport(compiled, contract);
	}

	@Test
	void fiveKillsNotOneUnlock16988ReportStage() {
		/* P5-1：16988 已是击杀网格（var0 = 计数 0..5），第 5 杀进满段 a5，报告（1009）才进领奖。
		   Grid since P5-1: var0 counts 0..5; the fifth kill lands on a5 and only the report rewards. */
		CompiledQuestDefinition compiled = load(16988);
		Map<String, Integer> variables = Map.of("var0", 0);
		QuestEvent event = new QuestEvent.KillNpc(233129);
		for (int count = 1; count <= 5; count++) {
			List<QuestMutationPlan> plans = plans(compiled, variables, event);
			assertEquals(1, plans.size(), "16988 kill " + count);
			QuestMutationPlan plan = plans.getFirst();
			assertEquals(QuestStatus.START, plan.nextStatus());
			variables = compiled.definition().progressLayout().unpack(plan.nextPackedVariables());
			assertEquals(Map.of("var0", count), variables);
			if (count < 5) {
				assertNoPrematureReward(compiled, variables);
			}
		}
	}

	@Test
	void quest2569RewardStateKeepsTheTurnInPreview() {
		/* 2569 的备选报告段 s2 已被 XML 侧修复移除（生产定义只剩单报告段 s1，由上方契约案例锁定）；
		   S2 之后 reward 态的页链入口（31 → SELECT5 页）与 1009 中转记录随规范段退场，领奖态重开
		   预览改由 NPC_COMPLETE 块的 preview 承担（USE_OBJECT / SELECT_QUEST_REWARD 同形，档位窗）。
		   2569's alternate report stage s2 was removed by an XML-side repair (a single report stage
		   s1 remains, locked by the contract case above). Since S2 the reward-state page-chain entry
		   (31 pushing SELECT5) retires with the canonical segments, and the completion block's preview
		   owns the reward-state re-open window (the USE_OBJECT / SELECT_QUEST_REWARD twin shape). */
		CompiledQuestDefinition compiled = load(2569);
		for (QuestDialogAction preview : List.of(QuestDialogAction.USE_OBJECT,
			QuestDialogAction.SELECT_QUEST_REWARD)) {
			QuestEvent event = new QuestEvent.TalkToNpc(204768, preview.id());
			List<QuestTransition> routes = compiled.definition().transitions().stream()
				.filter(t -> "reward".equals(t.sourceNode()) && "reward".equals(t.targetNode())
					&& t.event().equals(event))
				.toList();
			assertEquals(1, routes.size(), () -> "quest 2569 reward-state preview " + preview);
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), routes.getFirst().afterCommit(),
				() -> "quest 2569 reward-state preview " + preview);
		}
		assertTrue(compiled.definition().transitions().stream().noneMatch(t -> "reward".equals(t.sourceNode())
			&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.dialogId() != null
			&& talk.dialogId() == QuestDialogAction.QUEST_SELECT.id()),
			"quest 2569 reward-state SELECT5 page-chain entry must retire");
	}

	/**
	 * 竞技场两阶段任务（18208/18209/28208/28209）的领奖入口只在 REWARD 态。迁移期遗留的“对话即领奖”捷径
	 * （NPC_REPORT 生成的 started + 1009 无门禁路线）会让玩家跳过两行击杀直接进入 REWARD，旧 k7 阶段案例
	 * 已被批次 6 的“任务书行号”模型取代；这里对完整 START 变量域做 fail-closed 扫描，任何 START 侧对话路线
	 * 都不得进入 REWARD/奖励窗口。行推进与领奖态入口分别由 ArenaPhaseRowContractTest 与客户端契约门禁锁定。
	 * The arena quests expose their reward entrance in the REWARD state only; the migrated talk-to-report
	 * shortcut skipped both journal rows and the old k7 stage case was replaced by the batch-6 journal-row
	 * model. This sweeps the whole START variable space and fails closed.
	 */
	@ParameterizedTest
	@ValueSource(ints = {18208, 18209, 28208, 28209})
	void arenaJournalRowsKeepTheRewardEntranceInTheRewardState(int questId) {
		CompiledQuestDefinition compiled = load(questId);
		for (int var0 = 0; var0 <= 3; var0++) {
			for (int var1 = 0; var1 <= 4; var1++) {
				for (int var2 = 0; var2 <= 1; var2++) {
					assertNoPrematureReward(compiled, Map.of("var0", var0, "var1", var1, "var2", var2));
				}
			}
		}
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 9})
	void gateRejectsRelocated19631DeliveryRoute(int weakenedMinimum) {
		/* P0-2 DD 切片后 19631 网格形的守门对象 = 满段 a10 的 QUEST_SELECT 交付（无条件、分档奖励窗）；
		   负控把交付边原样搬到未满段 a<weakened>，零进度快照必须真的能上交，门禁必须拦下。
		   Since the P0-2 DD slice the gate target is the full-node QUEST_SELECT delivery on a10
		   (unconditional, tiered reward window); re-sourcing it verbatim to an incomplete segment
		   must let a zero-progress snapshot turn in, and the gate must catch exactly that. */
		CompiledQuestDefinition original = load(19631);
		QuestEvent event = new QuestEvent.TalkToNpc(800411, QuestDialogAction.QUEST_SELECT.id());
		QuestTransition route = original.definition().transitions().stream()
			.filter(t -> "a10".equals(t.sourceNode()) && "reward".equals(t.targetNode())
				&& t.event().equals(event))
			.findFirst().orElseThrow();
		assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.rewardWindowForTier(
					original.definition().metadata().rewardGroups().size() - 1).orElseThrow().id())),
			route.afterCommit());
		String weakenedSource = "a" + weakenedMinimum;
		QuestTransition broken = new QuestTransition(route.event(), route.conditions(), route.actions(),
			route.targetNode(), route.afterCommit(), route.priority(), weakenedSource);
		CompiledQuestDefinition mutated = replaceTransition(original, route, broken);
		Map<String, Integer> incomplete = Map.of("var0", weakenedMinimum);
		assertNoPrematureReward(original, incomplete);
		assertEquals(1, plans(mutated, incomplete, event).size(), "negative control must actually unlock");
		AssertionError failure = assertThrows(AssertionError.class,
			() -> assertNoPrematureReward(mutated, incomplete));
		assertTrue(failure.getMessage().contains("premature reward"));
	}

	@Test
	void gateRejects1347ReportWithOnlyOneCounterComplete() {
		CompiledQuestDefinition original = load(1347);
		// P0-2 规范形：交付边 = 满段 QUEST_SELECT（1009 中转删除）；负控把交付边搬到未满段节点，
		// 门禁必须仍然拦下"少一只就领奖"的旁路。
		// Canonical since P0-2: the delivery edge is the full node's QUEST_SELECT (no 1009 hop); the
		// negative control re-sources it to an incomplete node and the gate must still catch it.
		QuestEvent event = new QuestEvent.TalkToNpc(203966, QuestDialogAction.QUEST_SELECT.id());
		QuestTransition route = original.definition().transitions().stream()
			.filter(t -> "a7b3".equals(t.sourceNode()) && t.event().equals(event))
			.findFirst().orElseThrow();
		QuestTransition broken = new QuestTransition(route.event(), route.conditions(), route.actions(),
			route.targetNode(), route.afterCommit(), route.priority(), "a7b2");
		CompiledQuestDefinition mutated = replaceTransition(original, route, broken);
		Map<String, Integer> incomplete = Map.of("var0", 7, "var1", 2);
		assertNoPrematureReward(original, incomplete);
		assertEquals(1, plans(mutated, incomplete, event).size(), "negative control must actually unlock");
		AssertionError failure = assertThrows(AssertionError.class,
			() -> assertNoPrematureReward(mutated, incomplete));
		assertTrue(failure.getMessage().contains("premature reward"));
	}

	private static void assertNoPrematureReward(CompiledQuestDefinition compiled, Map<String, Integer> variables) {
		QuestSnapshot snapshot = snapshot(compiled, variables);
		// 检查所有 NPC/动作，不以 source/target 名称预筛，避免遗漏新旁路。
		// Probe every NPC/action without label filtering so new bypass routes remain visible.
		for (QuestTransition route : compiled.definition().transitions()) {
			if (!(route.event() instanceof QuestEvent.TalkToNpc)) {
				continue;
			}
			QuestMutationPlanner.plan(compiled, snapshot, route.event(), route).ifPresent(plan -> {
				boolean rewards = plan.nextStatus() == QuestStatus.REWARD || plan.nextStatus() == QuestStatus.COMPLETE
					|| plan.requiredActions().stream().anyMatch(a -> a instanceof QuestAction.GrantReward
						|| a instanceof QuestAction.GrantSelectedReward || a instanceof QuestAction.CompleteQuest)
					|| plan.afterCommit().contains(new AfterCommitAction.ShowQuestDialog(
						QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()));
				assertFalse(rewards, () -> "quest " + compiled.id() + " premature reward at " + variables
					+ " via " + route);
			});
		}
	}

	private static void assertCompletedReport(CompiledQuestDefinition compiled, RewardCase contract) {
		// 规范形（QUEST_SELECT 交付）按奖励组档位查表；遗留 1009 中转固定窗口 1（QE-028 禁写死档位）。
		// Canonical QUEST_SELECT delivery resolves the tiered window; the legacy 1009 hop keeps
		// the fixed first window (tier lookup is mandatory, never hard-coded).
		int rewardWindow = contract.reportAction() == QuestDialogAction.QUEST_SELECT
			? QuestDialogPage.rewardWindowForTier(
				compiled.definition().metadata().rewardGroups().size() - 1).orElseThrow().id()
			: QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id();
		for (int npcId : contract.reportNpcs()) {
			QuestEvent event = new QuestEvent.TalkToNpc(npcId, contract.reportAction().id());
			List<QuestTransition> routes = compiled.definition().transitions().stream()
				.filter(t -> QuestMutationPlanner.plan(compiled, snapshot(compiled, contract.full()), event, t).isPresent())
				.toList();
			assertEquals(1, routes.size(), "quest " + compiled.id() + " full progress report at " + npcId);
			QuestTransition route = routes.getFirst();
			assertEquals(contract.source(), route.sourceNode());
			assertEquals("reward", route.targetNode());
			assertEquals(event, route.event());
			assertEquals(contract.recovery()
				? List.of(new QuestCondition.VariableAtLeast("var1", contract.full().get("var1"))) : List.of(),
				route.conditions());
			List<QuestAction> actions = contract.recovery() ? List.of(new QuestAction.SetVariable("var0", 1)) : List.of();
			assertEquals(actions, route.actions());
			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled, snapshot(compiled, contract.full()), event, route)
				.orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus());
			assertEquals(contract.reward(), compiled.definition().progressLayout().unpack(plan.nextPackedVariables()));
			assertEquals(actions, plan.requiredActions());
			List<AfterCommitAction> effects = List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(rewardWindow));
			assertEquals(effects, route.afterCommit());
			assertEquals(effects, plan.afterCommit());
		}
	}

	private static List<QuestMutationPlan> plans(CompiledQuestDefinition compiled, Map<String, Integer> variables,
			QuestEvent event) {
		QuestSnapshot snapshot = snapshot(compiled, variables);
		return compiled.definition().transitions().stream()
			.flatMap(t -> QuestMutationPlanner.plan(compiled, snapshot, event, t).stream()).toList();
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition compiled, Map<String, Integer> variables) {
		return new QuestSnapshot(7, compiled.id(), QuestStatus.START,
			compiled.definition().progressLayout().pack(variables), Map.of());
	}

	private static CompiledQuestDefinition replaceTransition(CompiledQuestDefinition compiled,
			QuestTransition original, QuestTransition replacement) {
		QuestDefinition definition = compiled.definition();
		List<QuestTransition> transitions = definition.transitions().stream()
			.map(t -> t.equals(original) ? replacement : t).toList();
		return QuestDefinitionCompiler.compile(new QuestDefinition(definition.id(), definition.version(),
			definition.metadata(), definition.progressLayout(), definition.nodes(), transitions));
	}

	/**
	 * 真端击杀网格合同（P0-2 DD 切片规范形）：var0 = 击杀计数；未满段零对话路由，
	 * 满段 a<required> 的 QUEST_SELECT 直接进领奖（无条件、分档奖励窗），领奖投影 = 满段计数。
	 * Retail kill-grid contract in the canonical shape since the P0-2 DD slice: var0 counts kills,
	 * incomplete stages carry no dialog routes, the full segment's QUEST_SELECT reports to reward
	 * (unconditional, tiered window), and the reward projection equals the saturated count.
	 */
	private static RewardCase grid(int questId, int required, Integer... npcs) {
		List<Map<String, Integer>> incomplete = new ArrayList<>();
		for (int kills = 0; kills < required; kills++) {
			incomplete.add(Map.of("var0", kills));
		}
		return new RewardCase(questId, List.of(npcs), "a" + required, Map.of("var0", required),
			Map.of("var0", required), List.copyOf(incomplete), false, QuestDialogAction.QUEST_SELECT);
	}

	private static RewardCase counter(int questId, int required, Integer... npcs) {
		return new RewardCase(questId, List.of(npcs), "started", Map.of("var0", 0, "var1", required),
			Map.of("var0", 1, "var1", required), range(0, required - 1), true,
			QuestDialogAction.SELECT_QUEST_REWARD);
	}

	private static RewardCase stage(int questId, int var0, int var1, int rewardVar0, String source, Integer... npcs) {
		Map<String, Integer> full = var1 < 0 ? Map.of("var0", var0) : Map.of("var0", var0, "var1", var1);
		Map<String, Integer> reward = var1 < 0 ? Map.of("var0", rewardVar0)
			: Map.of("var0", rewardVar0, "var1", var1);
		List<Map<String, Integer>> incomplete = new ArrayList<>(range(var0, var1));
		incomplete.remove(full);
		return new RewardCase(questId, List.of(npcs), source, full, reward, List.copyOf(incomplete), false,
			QuestDialogAction.SELECT_QUEST_REWARD);
	}

	/** 规范形交付动作变体（P0-2：SimpleHunt 满段 QUEST_SELECT）。 / Canonical delivery-action variant. */
	private static RewardCase stageOnQuestSelect(int questId, int var0, int var1, int rewardVar0, String source,
			Integer... npcs) {
		Map<String, Integer> full = var1 < 0 ? Map.of("var0", var0) : Map.of("var0", var0, "var1", var1);
		Map<String, Integer> reward = var1 < 0 ? Map.of("var0", rewardVar0)
			: Map.of("var0", rewardVar0, "var1", var1);
		List<Map<String, Integer>> incomplete = new ArrayList<>(range(var0, var1));
		incomplete.remove(full);
		return new RewardCase(questId, List.of(npcs), source, full, reward, List.copyOf(incomplete), false,
			QuestDialogAction.QUEST_SELECT);
	}

	private static List<Map<String, Integer>> range(int maxVar0, int maxVar1) {
		List<Map<String, Integer>> values = new ArrayList<>();
		for (int var0 = 0; var0 <= maxVar0; var0++) {
			if (maxVar1 < 0) {
				values.add(Map.of("var0", var0));
			} else {
				for (int var1 = 0; var1 <= maxVar1; var1++) {
					values.add(Map.of("var0", var0, "var1", var1));
				}
			}
		}
		return List.copyOf(values);
	}

	private record RewardCase(int questId, List<Integer> reportNpcs, String source, Map<String, Integer> full,
			Map<String, Integer> reward, List<Map<String, Integer>> incomplete, boolean recovery,
			QuestDialogAction reportAction) {
	}

	private static CompiledQuestDefinition load(int questId) {
		// 退役任务的生产 XML 只在 git 历史里：统一取生产视图（XML 目录 + 真端 overlay）。
		// Retired quests keep their production XML in git history only, so use the production view.
		return ProductionQuestDefinitions.definition(questId);
	}
}
