package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
			stage(1347, 7, 3, 7, "a7b3", 203966),
			stage(16837, 1, -1, 0, "k1", 806568),
			stage(16986, 1, -1, 0, "k1", 804864),
			new RewardCase(16988, List.of(804865), "k1", Map.of("var0", 1, "var1", 5),
				Map.of("var0", 0, "var1", 5), range(0, 5), false),
			counter(19631, 10, 798155, 800411),
			counter(19633, 10, 800411, 205304),
			counter(19638, 10, 799022),
			counter(19642, 10, 798926),
			stage(2569, 1, -1, 1, "s1", 204768),
			stage(28208, 7, -1, 7, "k7", 205320, 205321),
			counter(28743, 1, 206395, 206396, 206397, 804732),
			counter(28932, 1, 806261, 806260),
			counter(28951, 25, 209743, 804738),
			stage(28952, 4, -1, 4, "k4", 209743, 804738),
			counter(28972, 6, 804924, 805216),
			counter(28973, 6, 804924, 805217),
			counter(28974, 6, 804924, 805218),
			// 25640/25698 为“满计数恢复报告”形态的 A03 碎片任务，与 Quest25640/25698ClientDialogAlignmentTest 配对。
			// The 25640/25698 pair are full-count recovery-report owners; paired with their dialog alignment gates.
			counter(25640, 30, 806101),
			counter(25698, 5, 806804));
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
		CompiledQuestDefinition compiled = load(16988);
		Map<String, Integer> variables = Map.of("var0", 0, "var1", 0);
		QuestEvent event = new QuestEvent.KillNpc(233129);
		for (int count = 1; count <= 5; count++) {
			List<QuestMutationPlan> plans = plans(compiled, variables, event);
			assertEquals(1, plans.size(), "16988 kill " + count);
			QuestMutationPlan plan = plans.getFirst();
			assertEquals(QuestStatus.START, plan.nextStatus());
			variables = compiled.definition().progressLayout().unpack(plan.nextPackedVariables());
			assertEquals(Map.of("var0", count == 5 ? 1 : 0, "var1", count), variables);
			if (count < 5) {
				assertNoPrematureReward(compiled, variables);
			}
		}
	}

	@Test
	void alternate2569ReportStageRemainsUsable() {
		assertCompletedReport(load(2569), stage(2569, 2, -1, 2, "s2", 204768));
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 9})
	void gateRejectsRemovedOrWeakened19631CounterCondition(int weakenedMinimum) {
		CompiledQuestDefinition original = load(19631);
		QuestEvent event = new QuestEvent.TalkToNpc(800411, QuestDialogAction.SELECT_QUEST_REWARD.id());
		QuestTransition route = original.definition().transitions().stream()
			.filter(t -> "started".equals(t.sourceNode()) && t.event().equals(event))
			.findFirst().orElseThrow();
		assertEquals(List.of(new QuestCondition.VariableAtLeast("var1", 10)), route.conditions());
		List<QuestCondition> brokenConditions = weakenedMinimum == 0 ? List.of()
			: List.of(new QuestCondition.VariableAtLeast("var1", weakenedMinimum));
		QuestTransition broken = new QuestTransition(route.event(), brokenConditions, route.actions(),
			route.targetNode(), route.afterCommit(), route.priority(), route.sourceNode());
		CompiledQuestDefinition mutated = replaceTransition(original, route, broken);
		Map<String, Integer> incomplete = Map.of("var0", 0, "var1", weakenedMinimum);
		assertNoPrematureReward(original, incomplete);
		assertEquals(1, plans(mutated, incomplete, event).size(), "negative control must actually unlock");
		AssertionError failure = assertThrows(AssertionError.class,
			() -> assertNoPrematureReward(mutated, incomplete));
		assertTrue(failure.getMessage().contains("premature reward"));
	}

	@Test
	void gateRejects1347ReportWithOnlyOneCounterComplete() {
		CompiledQuestDefinition original = load(1347);
		QuestEvent event = new QuestEvent.TalkToNpc(203966, QuestDialogAction.SELECT_QUEST_REWARD.id());
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
		for (int npcId : contract.reportNpcs()) {
			QuestEvent event = new QuestEvent.TalkToNpc(npcId, QuestDialogAction.SELECT_QUEST_REWARD.id());
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
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id()));
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

	private static RewardCase counter(int questId, int required, Integer... npcs) {
		return new RewardCase(questId, List.of(npcs), "started", Map.of("var0", 0, "var1", required),
			Map.of("var0", 1, "var1", required), range(0, required - 1), true);
	}

	private static RewardCase stage(int questId, int var0, int var1, int rewardVar0, String source, Integer... npcs) {
		Map<String, Integer> full = var1 < 0 ? Map.of("var0", var0) : Map.of("var0", var0, "var1", var1);
		Map<String, Integer> reward = var1 < 0 ? Map.of("var0", rewardVar0)
			: Map.of("var0", rewardVar0, "var1", var1);
		List<Map<String, Integer>> incomplete = new ArrayList<>(range(var0, var1));
		incomplete.remove(full);
		return new RewardCase(questId, List.of(npcs), source, full, reward, List.copyOf(incomplete), false);
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
			Map<String, Integer> reward, List<Map<String, Integer>> incomplete, boolean recovery) {
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
			QuestPrematureRewardRouteExclusionTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
