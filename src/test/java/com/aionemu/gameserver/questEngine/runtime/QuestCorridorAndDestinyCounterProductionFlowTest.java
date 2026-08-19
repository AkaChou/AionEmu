package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.AfterCommitAction;
import com.aionemu.gameserver.questEngine.definition.CompiledQuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinition;
import com.aionemu.gameserver.questEngine.definition.QuestDefinitionXmlCompiler;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestStateSyncMode;
import com.aionemu.gameserver.questEngine.definition.QuestTransition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 验证希埃尔之矛主线和命运对决主线保留实时击杀计数，
 * 并在最终击杀时按旧 handler 清零高位计数。
 * Verifies that the Siel's Spear and destiny missions retain live kill counters and clear the packed high counter on
 * the legacy handler's final kill.
 */
class QuestCorridorAndDestinyCounterProductionFlowTest {
	private static final int CORRIDOR_KILL_COUNT = 10;
	private static final Set<Integer> INGGISON_TARGETS = Set.of(216775, 220021, 220022);
	private static final Set<Integer> GELKMAROS_TARGETS = Set.of(
		216107, 216450, 216104, 216449, 216112, 216451, 216109, 216108, 216101, 216448);
	private static final Set<Integer> DESTINY_TARGETS = Set.of(798342, 798343, 798344, 798345, 798346);
	private static final List<CorridorContract> CORRIDOR_CONTRACTS = List.of(
		new CorridorContract(10035, INGGISON_TARGETS, 216775),
		new CorridorContract(20035, GELKMAROS_TARGETS, 216107));

	@TestFactory
	Stream<DynamicTest> completesCorridorHuntsOnTheTenthKillAndClearsCounter() {
		return CORRIDOR_CONTRACTS.stream().map(contract -> DynamicTest.dynamicTest(
			"quest " + contract.questId(), () -> assertCorridorContract(contract)));
	}

	@Test
	void completesDestinyMobHuntOnTheFiftiethKillAndSpawnsBoss() throws Exception {
		CompiledQuestDefinition compiled = load(24030);
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "s6", QuestStatus.START, Map.of("var0", 6));
		assertNode(definition, "s7", QuestStatus.START, Map.of("var0", 7, "var1", 0));
		assertNode(definition, "s8", QuestStatus.START, Map.of("var0", 8, "var1", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 8, "var1", 0));

		QuestEvent targets = new QuestEvent.KillNpcSet(DESTINY_TARGETS);
		QuestTransition continuing = transition(definition, "s6", "s6", targets);
		assertEquals(1, continuing.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 6),
			new QuestCondition.VariableBelow("var1", 49)), continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition completion = transition(definition, "s6", "s7", targets);
		assertEquals(0, completion.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 6),
			new QuestCondition.QuestVariableIs("var1", 49)), completion.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 7)), completion.actions());
		assertEquals(List.of(
			new AfterCommitAction.SpawnNpc("boss", 320140000, 798346,
				299.4378f, 289.15744f, 206.48138f, (byte) 75),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)), completion.afterCommit());

		QuestSnapshot snapshot = snapshot(24030, Map.of("var0", 6, "var1", 0), definition);
		for (int kills = 1; kills < 50; kills++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot, new QuestEvent.KillNpc(798342));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", 6, "var1", kills), unpack(definition, snapshot));
		}

		QuestMutationPlan finalMobKill = dispatch(compiled, snapshot, new QuestEvent.KillNpc(798342));
		assertEquals(QuestStatus.START, finalMobKill.nextStatus());
		assertEquals(Map.of("var0", 7, "var1", 0),
			definition.progressLayout().unpack(finalMobKill.nextPackedVariables()));
		assertEquals(completion.actions(), finalMobKill.requiredActions());

		QuestTransition bossKill = transition(definition, "s7", "s8", new QuestEvent.KillNpc(798346));
		assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 7)), bossKill.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 8)), bossKill.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			bossKill.afterCommit());

		QuestMutationPlan bossPlan = dispatch(compiled, nextSnapshot(snapshot, finalMobKill),
			new QuestEvent.KillNpc(798346));
		assertEquals(QuestStatus.START, bossPlan.nextStatus());
		assertEquals(Map.of("var0", 8, "var1", 0),
			definition.progressLayout().unpack(bossPlan.nextPackedVariables()));
		assertEquals(bossKill.actions(), bossPlan.requiredActions());
	}

	private static void assertCorridorContract(CorridorContract contract) throws Exception {
		CompiledQuestDefinition compiled = load(contract.questId());
		QuestDefinition definition = compiled.definition();
		assertNode(definition, "s5", QuestStatus.START, Map.of("var0", 5));
		assertNode(definition, "s6", QuestStatus.START, Map.of("var0", 6, "var1", 0));
		assertNode(definition, "s7", QuestStatus.START, Map.of("var0", 7, "var1", 0));
		assertNode(definition, "reward", QuestStatus.REWARD, Map.of("var0", 8, "var1", 0));

		QuestEvent targets = new QuestEvent.KillNpcSet(contract.targetNpcIds());
		QuestTransition continuing = transition(definition, "s5", "s5", targets);
		assertEquals(1, continuing.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 5),
			new QuestCondition.VariableBelow("var1", CORRIDOR_KILL_COUNT - 1)), continuing.conditions());
		assertEquals(List.of(new QuestAction.IncrementVariable("var1", 1)), continuing.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			continuing.afterCommit());

		QuestTransition completion = transition(definition, "s5", "s6", targets);
		assertEquals(0, completion.priority());
		assertEquals(List.of(
			new QuestCondition.QuestVariableIs("var0", 5),
			new QuestCondition.VariableAtLeast("var1", CORRIDOR_KILL_COUNT - 1)), completion.conditions());
		assertEquals(List.of(new QuestAction.SetVariable("var0", 6)), completion.actions());
		assertEquals(List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)),
			completion.afterCommit());

		QuestSnapshot snapshot = snapshot(contract.questId(), Map.of("var0", 5, "var1", 0), definition);
		for (int kills = 1; kills < CORRIDOR_KILL_COUNT; kills++) {
			QuestMutationPlan plan = dispatch(compiled, snapshot,
				new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
			snapshot = nextSnapshot(snapshot, plan);
			assertEquals(QuestStatus.START, snapshot.status());
			assertEquals(Map.of("var0", 5, "var1", kills), unpack(definition, snapshot));
		}

		QuestMutationPlan finalKill = dispatch(compiled, snapshot,
			new QuestEvent.KillNpc(contract.sampleTargetNpcId()));
		assertEquals(QuestStatus.START, finalKill.nextStatus());
		assertEquals(Map.of("var0", 6, "var1", 0),
			definition.progressLayout().unpack(finalKill.nextPackedVariables()));
		assertEquals(completion.actions(), finalKill.requiredActions());
	}

	private static QuestMutationPlan dispatch(CompiledQuestDefinition compiled, QuestSnapshot snapshot,
			QuestEvent event) {
		List<QuestMutationPlan> plans = compiled.definition().transitions().stream()
			.map(transition -> QuestMutationPlanner.plan(compiled, snapshot, event, transition).orElse(null))
			.filter(Objects::nonNull)
			.toList();
		assertEquals(1, plans.size(), () -> compiled.id() + " " + event + " "
			+ compiled.definition().progressLayout().unpack(snapshot.packedVariables()));
		return plans.getFirst();
	}

	private static QuestSnapshot nextSnapshot(QuestSnapshot snapshot, QuestMutationPlan plan) {
		return new QuestSnapshot(snapshot.playerId(), snapshot.questId(), plan.nextStatus(),
			plan.nextPackedVariables(), snapshot.inventory());
	}

	private static QuestSnapshot snapshot(int questId, Map<String, Integer> variables,
			QuestDefinition definition) {
		return new QuestSnapshot(7, questId, QuestStatus.START,
			definition.progressLayout().pack(variables), Map.of());
	}

	private static Map<String, Integer> unpack(QuestDefinition definition, QuestSnapshot snapshot) {
		return definition.progressLayout().unpack(snapshot.packedVariables());
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status,
			Map<String, Integer> variables) {
		var node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(variables, node.projection().variables());
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			QuestEvent event) {
		List<QuestTransition> matches = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.targetNode().equals(target))
			.filter(candidate -> candidate.event().equals(event))
			.toList();
		assertEquals(1, matches.size(), () -> source + " -> " + target + " " + event);
		return matches.getFirst();
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestCorridorAndDestinyCounterProductionFlowTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	/**
	 * 保存双阵营任务的目标集合和代表击杀模板。
	 * Holds each faction quest's target set and representative kill template.
	 */
	private record CorridorContract(int questId, Set<Integer> targetNpcIds, int sampleTargetNpcId) {
	}
}
