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
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 锁定批次 6 的“竞技场两阶段行推进”合同：18208/18209（天族）与 28208/28209（魔族）的客户端
 * quest_summary 都是 3 行——“消灭第 2 神庙训练所的最终训练对象 (n/5)”“穿过…消灭 quest_18208a (n/1)”
 * “和领奖 NPC 对话”。客户端 quest_script 声明 `SECTION_0==0; SECTION_1<5` 与 `SECTION_0==1; SECTION_2<1`，
 * 旧 handler（`_18208IllusionOrInfiltration`/`_28208ARiftAdrift` 一类）同样用 var0 0 -&gt; 1、var1 0..4
 * （5 次 217819 击杀）、var2 0 -&gt; 1（精英击杀 218185/218200）并置 REWARD。
 * 迁移把天族侧的击杀路线整条丢掉（只剩“对话即领奖”），魔族侧改成每个击杀一个 var0 阶段（k1..k7），
 * 两边都与客户端声明冲突。本测试锁定两阶段模型、两条击杀路线、领奖行投影（var0=2，与 2620/4210
 * 的 QE-051 领奖行合同一致）、旧存档收敛边与天/魔镜像同形。
 * Locks the batch-6 “arena two-phase row advance” contract for 18208/18209 and their Asmodian mirrors
 * 28208/28209: the client journal has three rows (five 217819 kills counted in var1 while var0=0, the single
 * elite kill of 218185/218200 counted in var2 while var0=1, then the reward NPC). The migration dropped the
 * Elyos kill routes entirely and replaced the Asmodian side with a per-kill stage chain (k1..k7), both of
 * which conflict with the client script. The test pins the two-phase model, both kill routes, the reward row
 * projection (var0=2, the same QE-051 reward-row contract as 2620/4210), the legacy-save collapse edges, and
 * Elyos/Asmodian mirror parity.
 */
class ArenaPhaseRowContractTest {

	private static final int VANQ = 217819;
	private static final Set<Integer> ELITES = Set.of(218185, 218200);

	private static final List<Integer> QUESTS = List.of(18208, 18209, 28208, 28209);

	@Test
	void everyJournalRowHasItsOwnStage() throws Exception {
		for (int questId : QUESTS) {
			QuestDefinition definition = definition(questId).definition();
			assertEquals(0, row(definition, "started"), () -> "quest " + questId + " first stage");
			assertEquals(1, row(definition, "s1"), () -> "quest " + questId + " second stage");
			assertEquals(2, row(definition, "reward"),
				() -> "quest " + questId + " reward row (QE-051: reward projection = journal reward row)");
			BitField first = definition.progressLayout().field("var0");
			BitField killCounter = definition.progressLayout().field("var1");
			BitField eliteFlag = definition.progressLayout().field("var2");
			assertEquals(0, first.offset(), () -> "quest " + questId + " var0 owns SECTION_0");
			assertTrue(first.maxValue() >= 2, () -> "quest " + questId + " var0 max covers the reward row");
			assertEquals(6, killCounter.offset(), () -> "quest " + questId + " var1 must own SECTION_1");
			assertEquals(12, eliteFlag.offset(), () -> "quest " + questId + " var2 must own SECTION_2");
			assertEquals(4, killCounter.maxValue(), () -> "quest " + questId + " kill counter max");
			assertEquals(1, eliteFlag.maxValue(), () -> "quest " + questId + " elite flag max");
		}
	}

	@Test
	void fiveEliteTrainingKillsAdvanceTheStage() throws Exception {
		for (int questId : QUESTS) {
			CompiledQuestDefinition compiled = definition(questId);
			QuestTransition counter = route(compiled.definition(), "started", "started");
			assertEquals(new QuestEvent.KillNpc(VANQ), counter.event(),
				() -> "quest " + questId + " kill counter event");
			assertEquals(List.of(new QuestCondition.VariableBelow("var1", 4)), counter.conditions(),
				() -> "quest " + questId + " kill counter conditions");
			assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), counter.actions(),
				() -> "quest " + questId + " kill counter actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
				counter.afterCommit(), () -> "quest " + questId + " kill counter after-commit");

			QuestTransition advance = route(compiled.definition(), "started", "s1");
			assertEquals(new QuestEvent.KillNpc(VANQ), advance.event(),
				() -> "quest " + questId + " advance event");
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var1", 4)), advance.conditions(),
				() -> "quest " + questId + " advance conditions");

			Map<String, Integer> variables = Map.of("var0", 0, "var1", 0, "var2", 0);
			for (int kill = 1; kill <= 4; kill++) {
				int currentKill = kill;
				QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
					snapshot(compiled, QuestStatus.START, variables), counter).orElseThrow();
				variables = unpack(compiled, plan);
				assertEquals(currentKill, variables.get("var1"),
					() -> "quest " + questId + " kill " + currentKill);
				assertEquals(0, variables.get("var0"), () -> "quest " + questId + " first stage stays");
			}
			QuestMutationPlan advanced = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.START, variables), advance).orElseThrow();
			Map<String, Integer> afterAdvance = unpack(compiled, advanced);
			assertEquals(1, afterAdvance.get("var0"), () -> "quest " + questId + " second stage");
			assertEquals(4, afterAdvance.get("var1"), () -> "quest " + questId + " kill counter kept");
			assertEquals(QuestStatus.START, advanced.nextStatus());
		}
	}

	@Test
	void theEliteKillCommitsTheRewardStage() throws Exception {
		for (int questId : QUESTS) {
			CompiledQuestDefinition compiled = definition(questId);
			QuestTransition elite = route(compiled.definition(), "s1", "reward");
			assertEquals(new QuestEvent.KillNpcSet(ELITES), elite.event(),
				() -> "quest " + questId + " elite event");
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var2", 0)), elite.conditions(),
				() -> "quest " + questId + " elite conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var2", 1)), elite.actions(),
				() -> "quest " + questId + " elite actions");
			assertEquals(List.of(new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)), elite.afterCommit(),
				() -> "quest " + questId + " elite after-commit");

			QuestMutationPlan plan = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.START, Map.of("var0", 1, "var1", 4, "var2", 0)), elite)
				.orElseThrow();
			assertEquals(QuestStatus.REWARD, plan.nextStatus(), () -> "quest " + questId + " elite status");
			Map<String, Integer> variables = unpack(compiled, plan);
			assertEquals(2, variables.get("var0"), () -> "quest " + questId + " reward row");
			assertEquals(1, variables.get("var2"), () -> "quest " + questId + " elite flag");
		}
	}

	@Test
	void legacySavesAreCollapsedOntoTheJournalRows() throws Exception {
		for (int questId : QUESTS) {
			CompiledQuestDefinition compiled = definition(questId);
			List<QuestTransition> recoveries = compiled.definition().transitions().stream()
				.filter(candidate -> candidate.sourceNode() == null)
				.filter(candidate -> candidate.event().equals(new QuestEvent.EnterWorld()))
				.toList();
			/* 迁移版魔族的 k1..k7（var0=1..7）在新行号模型下不再匹配任何节点，这里锁定四条收敛边：
			   旧 k2..k4 回第 1 行、旧 k5..k7 进第 2 行、旧 REWARD 的低值与高值统一收敛到领奖行。
			   同型任务 2620/4210 的 QE-051 契约只有一条 REWARD 收敛边；本任务的旧值域同时覆盖 START 侧。 */
			/* The migrated Asmodian k1..k7 stages (var0=1..7) match no node under the row model, so four
			   collapse edges are pinned. The sibling 2620/4210 QE-051 contract only needs the REWARD edge
			   because its legacy START values never left the new row set. */
			assertEquals(4, recoveries.size(), () -> "quest " + questId + " enter-world repair edges");
			List<QuestTransition> toStarted = recoveries.stream()
				.filter(candidate -> "started".equals(candidate.targetNode())).toList();
			List<QuestTransition> toSecondRow = recoveries.stream()
				.filter(candidate -> "s1".equals(candidate.targetNode())).toList();
			List<QuestTransition> toReward = recoveries.stream()
				.filter(candidate -> "reward".equals(candidate.targetNode())).toList();
			assertEquals(1, toStarted.size(), () -> "quest " + questId + " first-row collapse edge");
			assertEquals(1, toSecondRow.size(), () -> "quest " + questId + " second-row collapse edge");
			assertEquals(2, toReward.size(), () -> "quest " + questId + " reward collapse edges");

			QuestTransition firstRow = toStarted.getFirst();
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.START),
				new QuestCondition.VariableAtLeast("var0", 1),
				new QuestCondition.VariableBelow("var0", 5),
				new QuestCondition.VariableBelow("var1", 4)), firstRow.conditions(),
				() -> "quest " + questId + " first-row collapse conditions");
			assertEquals(List.of(new QuestAction.SetVariable("var0", 0)), firstRow.actions(),
				() -> "quest " + questId + " first-row collapse actions");
			assertNull(firstRow.priority());

			QuestTransition secondRow = toSecondRow.getFirst();
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.START),
				new QuestCondition.VariableAtLeast("var0", 5)), secondRow.conditions(),
				() -> "quest " + questId + " second-row collapse conditions");
			assertEquals(List.of(
				new QuestAction.SetVariable("var0", 1),
				new QuestAction.SetVariable("var1", 4)), secondRow.actions(),
				() -> "quest " + questId + " second-row collapse actions");

			List<QuestTransition> lowRewards = toReward.stream()
				.filter(edge -> edge.conditions().contains(new QuestCondition.VariableBelow("var0", 2)))
				.toList();
			List<QuestTransition> highRewards = toReward.stream()
				.filter(edge -> edge.conditions().contains(new QuestCondition.VariableAtLeast("var0", 3)))
				.toList();
			assertEquals(1, lowRewards.size(), () -> "quest " + questId + " low reward collapse edge");
			assertEquals(1, highRewards.size(), () -> "quest " + questId + " high reward collapse edge");
			QuestTransition lowReward = lowRewards.getFirst();
			QuestTransition highReward = highRewards.getFirst();
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.VariableBelow("var0", 2)), lowReward.conditions(),
				() -> "quest " + questId + " low reward collapse conditions");
			assertEquals(List.of(
				new QuestCondition.StatusIs(QuestStatus.REWARD),
				new QuestCondition.VariableAtLeast("var0", 3)), highReward.conditions(),
				() -> "quest " + questId + " high reward collapse conditions");
			for (QuestTransition edge : toReward) {
				assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), edge.actions(),
					() -> "quest " + questId + " reward collapse actions");
			}

			/* 旧 k3 回第 1 行、旧 k6 进第 2 行、正常 s1（var1=4）不被误伤、旧 REWARD 收敛到领奖行。 */
			/* Legacy k3 falls back to row 1, legacy k6 advances to row 2, a settled s1 (var1=4) is never
			   dragged back, and legacy REWARD saves collapse onto the reward row. */
			QuestMutationPlan backToFirst = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.START, Map.of("var0", 3, "var1", 0, "var2", 0)),
				firstRow).orElseThrow();
			assertEquals(QuestStatus.START, backToFirst.nextStatus());
			assertEquals(0, unpack(compiled, backToFirst).get("var0"),
				() -> "quest " + questId + " legacy k3 collapses onto row 1");

			QuestMutationPlan forwardToSecond = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.START, Map.of("var0", 6, "var1", 0, "var2", 0)),
				secondRow).orElseThrow();
			assertEquals(1, unpack(compiled, forwardToSecond).get("var0"),
				() -> "quest " + questId + " legacy k6 collapses onto row 2");
			assertEquals(4, unpack(compiled, forwardToSecond).get("var1"),
				() -> "quest " + questId + " legacy k6 restores the kill counter");

			assertTrue(QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.START, Map.of("var0", 1, "var1", 4, "var2", 0)),
				firstRow).isEmpty(), () -> "quest " + questId + " settled s1 must not collapse");

			QuestMutationPlan repaired = QuestMutationPlanner.plan(compiled,
				snapshot(compiled, QuestStatus.REWARD, Map.of("var0", 7, "var1", 0, "var2", 1)),
				highReward).orElseThrow();
			assertEquals(QuestStatus.REWARD, repaired.nextStatus());
			assertEquals(2, unpack(compiled, repaired).get("var0"),
				() -> "quest " + questId + " legacy reward row");
		}
	}

	@Test
	void noRewardRouteWritesAForeignStage() throws Exception {
		for (int questId : QUESTS) {
			QuestDefinition definition = definition(questId).definition();
			List<QuestTransition> rewardRoutes = definition.transitions().stream()
				.filter(candidate -> "reward".equals(candidate.targetNode())).toList();
			assertFalse(rewardRoutes.isEmpty(), () -> "quest " + questId + " reward routes");
			boolean eliteRouteWired = false;
			for (QuestTransition route : rewardRoutes) {
				if (route.event() instanceof QuestEvent.KillNpcSet(Set<Integer> npcIds)
						&& npcIds.equals(ELITES)) {
					eliteRouteWired = true;
				}
				for (QuestAction action : route.actions()) {
					if (action instanceof QuestAction.SetVariable(String field, int value)
							&& "var0".equals(field)) {
						assertEquals(2, value, () -> "quest " + questId + " route writes a foreign row");
					}
				}
			}
			assertTrue(eliteRouteWired, () -> "quest " + questId + " elite kill route");
		}
	}

	@Test
	void mirrorPairsShareTheSamePhaseModel() throws Exception {
		for (int questId : List.of(18208, 18209)) {
			assertEquals(projections(definition(questId).definition()),
				projections(definition(questId + 10000).definition()),
				() -> "mirror " + (questId + 10000) + " node projections");
			assertEquals(killShapes(definition(questId).definition()),
				killShapes(definition(questId + 10000).definition()),
				() -> "mirror " + (questId + 10000) + " kill route shapes");
		}
	}

	private static Map<String, String> projections(QuestDefinition definition) {
		Map<String, String> projections = new TreeMap<>();
		for (QuestNode node : definition.nodes()) {
			projections.put(node.label(), node.projection().status() + "@" + node.projection().variables());
		}
		return projections;
	}

	private static List<String> killShapes(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpc
				|| candidate.event() instanceof QuestEvent.KillNpcSet)
			.map(candidate -> candidate.sourceNode() + "->" + candidate.targetNode() + ":"
				+ candidate.event())
			.sorted()
			.toList();
	}

	private static int row(QuestDefinition definition, String label) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> label.equals(candidate.label())).findFirst().orElseThrow();
		return node.projection().variables().get("var0");
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> !(candidate.event() instanceof QuestEvent.EnterWorld))
			/* 领奖捷径（NPC_REPORT）与领奖态自环也指向 reward，用事件类型区分击杀路线。 */
			/* The report shortcut and the reward self-loop also target reward, so filter by event type. */
			.filter(candidate -> candidate.event() instanceof QuestEvent.KillNpc
				|| candidate.event() instanceof QuestEvent.KillNpcSet)
			.toList();
		assertEquals(1, matches.size(), () -> "quest " + definition.id() + " kill route " + source
			+ "->" + target);
		return matches.getFirst();
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
		try (InputStream input = ArenaPhaseRowContractTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
