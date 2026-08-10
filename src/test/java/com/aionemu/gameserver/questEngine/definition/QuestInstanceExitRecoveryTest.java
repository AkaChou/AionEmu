package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestInstanceExitRecoveryTest {
	@Test
	void incompleteInstanceExitRollsBackAndReentryDoesNotRequireDeliveredItemsAgain() throws Exception {
		for (RecoveryScenario scenario : recoveryScenarios()) {
			CompiledQuestDefinition definition = load(scenario.questId());
			for (String sourceNode : scenario.transientNodes()) {
				assertTrue(definition.definition().transitions().stream().anyMatch(transition ->
					transition.sourceNode().equals(sourceNode)
						&& transition.targetNode().equals(scenario.rollbackNode())
						&& transition.event() instanceof QuestEvent.EnterWorld
						&& transition.conditions().contains(
							new QuestCondition.WorldIs(scenario.instanceWorldId(), false))),
					() -> "quest " + scenario.questId() + " missing outside recovery from " + sourceNode);
			}
			assertFalse(definition.definition().transitions().stream().anyMatch(transition ->
				transition.sourceNode().equals(scenario.completedNode())
					&& transition.targetNode().equals(scenario.rollbackNode())
					&& transition.event() instanceof QuestEvent.EnterWorld
					&& transition.conditions().contains(
						new QuestCondition.WorldIs(scenario.instanceWorldId(), false))),
				() -> "quest " + scenario.questId() + " rolls back completed region stage "
					+ scenario.completedNode());

			QuestEvent.EnterWorld enterWorld = new QuestEvent.EnterWorld();
			QuestTransition recovery = definition.definition().transitions().stream()
				.filter(transition -> transition.sourceNode().equals(scenario.runtimeSourceNode())
					&& transition.targetNode().equals(scenario.rollbackNode())
					&& QuestEvent.matches(transition.event(), enterWorld)
					&& transition.conditions().contains(
						new QuestCondition.WorldIs(scenario.instanceWorldId(), false)))
				.findFirst().orElseThrow();
			QuestSnapshot outside = snapshot(definition, scenario.runtimeSourceVariables(),
				scenario.entryInventory(), 100000000);
			QuestMutationPlan recovered = QuestMutationPlanner.plan(definition, outside, enterWorld, recovery)
				.orElseThrow();
			Map<String, Integer> recoveredVariables = definition.definition().progressLayout()
				.unpack(recovered.nextPackedVariables());
			assertEquals(scenario.rollbackVariables(), recoveredVariables.entrySet().stream()
				.filter(entry -> scenario.rollbackVariables().containsKey(entry.getKey()))
				.collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
			assertFalse(recovered.requiredActions().stream().anyMatch(QuestAction.RemoveItem.class::isInstance));

			QuestSnapshot readyToReenter = new QuestSnapshot(7, scenario.questId(), QuestStatus.START,
				recovered.nextPackedVariables(), scenario.entryInventory(), Map.of(), true, true,
				0, 0, 100000000, 1, 0f, 0f, 0f, (byte) 0);
			QuestMutationPlan reentry = definition.definition().transitions().stream()
				.filter(transition -> transition.sourceNode().equals(scenario.rollbackNode())
					&& QuestEvent.matches(transition.event(), scenario.reentryEvent())
					&& transition.afterCommit().stream().anyMatch(action ->
						action instanceof AfterCommitAction.TeleportPlayer teleport
							&& teleport.worldId() == scenario.instanceWorldId()
							&& teleport.instanceTarget() instanceof QuestInstanceTarget.NextAvailable))
				.map(transition -> QuestMutationPlanner.plan(definition, readyToReenter,
					scenario.reentryEvent(), transition))
				.flatMap(result -> result.stream())
				.findFirst().orElseThrow();
			assertTrue(reentry.afterCommit().stream().anyMatch(action ->
				action instanceof AfterCommitAction.TeleportPlayer teleport
					&& teleport.worldId() == scenario.instanceWorldId()
					&& teleport.instanceTarget() instanceof QuestInstanceTarget.NextAvailable));
		}
	}

	@Test
	void deathAndLogoutRecoveryStayLimitedToIncompleteInstanceStages() throws Exception {
		for (FailureRecoveryScenario scenario : failureRecoveryScenarios()) {
			CompiledQuestDefinition definition = load(scenario.questId());
			assertEquals(scenario.transientNodes().size(), definition.definition().transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.Die).count());
			assertEquals(scenario.transientNodes().size(), definition.definition().transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.LogOut).count());
			for (String sourceNode : scenario.transientNodes()) {
				assertFailureRoute(definition, scenario, sourceNode, new QuestEvent.Die());
				assertFailureRoute(definition, scenario, sourceNode, new QuestEvent.LogOut());
			}
		}
	}

	private static void assertFailureRoute(CompiledQuestDefinition definition,
			FailureRecoveryScenario scenario, String sourceNode, QuestEvent event) {
		QuestTransition transition = definition.definition().transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(sourceNode)
				&& candidate.targetNode().equals(scenario.rollbackNode())
				&& QuestEvent.matches(candidate.event(), event))
			.findFirst().orElseThrow();
		Map<String, Integer> sourceVariables = new LinkedHashMap<>(scenario.runtimeSourceVariables());
		sourceVariables.put("var0", Integer.parseInt(sourceNode.substring(1)));
		QuestMutationPlan recovered = QuestMutationPlanner.plan(definition,
			snapshot(definition, sourceVariables, Map.of(), scenario.instanceWorldId()), event, transition)
			.orElseThrow();
		Map<String, Integer> unpacked = definition.definition().progressLayout()
			.unpack(recovered.nextPackedVariables());
		for (Map.Entry<String, Integer> expected : scenario.rollbackVariables().entrySet()) {
			assertEquals(expected.getValue(), unpacked.get(expected.getKey()));
		}
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, Map<String, Integer> variables,
			Map<Integer, Integer> inventory, int worldId) {
		Map<String, Integer> packedVariables = new LinkedHashMap<>(
			definition.definition().progressLayout().unpack(0));
		packedVariables.putAll(variables);
		return new QuestSnapshot(7, definition.id(), QuestStatus.START,
			definition.definition().progressLayout().pack(packedVariables), inventory, Map.of(), true, true,
			0, 0, worldId, 1, 0f, 0f, 0f, (byte) 0);
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestInstanceExitRecoveryTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			assertNotNull(input);
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private static List<RecoveryScenario> recoveryScenarios() {
		return List.of(
			new RecoveryScenario(10101, 301340000, "s1", Map.of("var0", 1), steps(2, 4), "s5",
				"s4", Map.of("var0", 4), new QuestEvent.TalkToNpc(731530, 10001), Map.of(182215521, 1)),
			new RecoveryScenario(20101, 301340000, "s1", Map.of("var0", 1), steps(2, 4), "s5",
				"s4", Map.of("var0", 4), new QuestEvent.TalkToNpc(731531, 10001), Map.of(182215523, 1)),
			new RecoveryScenario(10520, 301580000, "s4", Map.of("var0", 4), List.of("s5"), "reward",
				"s5", Map.of("var0", 5), new QuestEvent.MovieEnd(995), Map.of()),
			new RecoveryScenario(20520, 301580000, "s4", Map.of("var0", 4), List.of("s5"), "reward",
				"s5", Map.of("var0", 5), new QuestEvent.MovieEnd(867), Map.of()),
			new RecoveryScenario(10521, 301570000, "s2", Map.of("var0", 2), steps(3, 13), "reward",
				"s12", Map.of("var0", 12), new QuestEvent.MovieEnd(999), Map.of()),
			new RecoveryScenario(20521, 301570000, "s2", Map.of("var0", 2), steps(3, 13), "reward",
				"s12", Map.of("var0", 12), new QuestEvent.MovieEnd(871), Map.of()),
			new RecoveryScenario(10529, 301690000, "s3", Map.of("var0", 3, "var1", 0, "var2", 0),
				steps(4, 9), "reward", "s8", Map.of("var0", 8, "var1", 7, "var2", 3),
				new QuestEvent.TalkToNpc(731710, 10003), Map.of()),
			new RecoveryScenario(20529, 301690000, "s3", Map.of("var0", 3, "var1", 0, "var2", 0),
				steps(4, 9), "reward", "s8", Map.of("var0", 8, "var1", 7, "var2", 3),
				new QuestEvent.TalkToNpc(731716, 10003), Map.of()),
			new RecoveryScenario(15300, 301520000, "s2", Map.of("var0", 2), steps(3, 12), "s13",
				"s12", Map.of("var0", 12), new QuestEvent.TalkToNpc(805361, 10002), Map.of()),
			new RecoveryScenario(25300, 301520000, "s2", Map.of("var0", 2), steps(3, 12), "s13",
				"s12", Map.of("var0", 12), new QuestEvent.TalkToNpc(805364, 10002), Map.of()),
			new RecoveryScenario(3200, 300100000, "started", Map.of("var0", 0), steps(1, 2), "s3",
				"s2", Map.of("var0", 2), new QuestEvent.TalkToNpc(204658, 10000), Map.of(182209082, 1)),
			new RecoveryScenario(4200, 300100000, "started", Map.of("var0", 0), steps(1, 2), "s3",
				"s2", Map.of("var0", 2), new QuestEvent.TalkToNpc(204839, 10000), Map.of(182209097, 1)),
			new RecoveryScenario(20034, 300150000, "s2", Map.of("var0", 2, "var1", 0), steps(3, 5), "s6",
				"s5", Map.of("var0", 5, "var1", 3), new QuestEvent.TalkToNpc(730243, 10002), Map.of()),
			new RecoveryScenario(20038, 300150000, "s2", Map.of("var0", 2, "var1", 0), steps(3, 5), "s6",
				"s5", Map.of("var0", 5, "var1", 3), new QuestEvent.TalkToNpc(730243, 10002), Map.of()),
			new RecoveryScenario(2002, 320010000, "s12", Map.of("var0", 12, "var1", 7), List.of("s99"), "s13",
				"s99", Map.of("var0", 99, "var1", 7), new QuestEvent.TalkToNpc(790002, 10004), Map.of()));
	}

	private static List<FailureRecoveryScenario> failureRecoveryScenarios() {
		return List.of(
			new FailureRecoveryScenario(10101, 301340000, "s1", Map.of("var0", 1), steps(2, 4), Map.of()),
			new FailureRecoveryScenario(20101, 301340000, "s1", Map.of("var0", 1), steps(2, 4), Map.of()),
			new FailureRecoveryScenario(10521, 301570000, "s2", Map.of("var0", 2), steps(3, 13), Map.of()),
			new FailureRecoveryScenario(20521, 301570000, "s2", Map.of("var0", 2), steps(3, 13), Map.of()),
			new FailureRecoveryScenario(20034, 300150000, "s2", Map.of("var0", 2, "var1", 0),
				steps(3, 5), Map.of("var1", 3)),
			new FailureRecoveryScenario(20038, 300150000, "s2", Map.of("var0", 2, "var1", 0),
				steps(3, 5), Map.of("var1", 3)));
	}

	private static List<String> steps(int first, int last) {
		List<String> nodes = new ArrayList<>();
		for (int step = first; step <= last; step++) {
			nodes.add("s" + step);
		}
		return List.copyOf(nodes);
	}

	private record RecoveryScenario(int questId, int instanceWorldId, String rollbackNode,
			Map<String, Integer> rollbackVariables, List<String> transientNodes, String completedNode,
			String runtimeSourceNode, Map<String, Integer> runtimeSourceVariables,
			QuestEvent reentryEvent, Map<Integer, Integer> entryInventory) {
	}

	private record FailureRecoveryScenario(int questId, int instanceWorldId, String rollbackNode,
			Map<String, Integer> rollbackVariables, List<String> transientNodes,
			Map<String, Integer> runtimeSourceVariables) {
	}
}
