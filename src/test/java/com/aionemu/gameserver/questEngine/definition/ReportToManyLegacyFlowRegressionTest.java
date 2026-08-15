package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportToManyLegacyFlowRegressionTest {
	private static final Path CONTRACTS = Path.of(
		"docs/quest/client-dialog-mapping/legacy-quest-dialog-contracts.csv");
	private static final Path CLIENT_ACTIONS = Path.of(
		"docs/quest/client-dialog-mapping/quest-dialog-action-details.csv");
	private static final Set<Integer> QUEST_IDS = Set.of(
		1314, 1422, 1471, 1537, 1560, 1578, 1609, 1620, 1628, 1851, 1928, 1935, 2125,
		2222, 2383, 2486, 2488, 2501, 2514, 2539, 2583, 2630, 2651, 2693, 2721, 2725,
		2921, 2957, 3008, 3081, 3083, 3091, 3102, 3913, 3972, 4001, 4036, 4101, 4971,
		4972, 4973, 4974, 4976, 11000, 11005, 11228, 13900, 18917, 21066, 21068, 21071,
		21106, 21111, 21135, 23900, 28917, 29004, 30054, 30154);
	private static final Set<Integer> SELECTION_RESPONSE_QUESTS = Set.of(
		2222, 2486, 2488, 2651, 2921, 2957, 3972, 4971, 4972, 4973, 4974, 4976,
		21068, 21106, 21111, 21135, 29004, 30054, 30154);
	private static final Map<Integer, Integer> DIRECT_REWARD_OPEN_ACTIONS = Map.ofEntries(
		Map.entry(1851, 31),
		Map.entry(2486, -1),
		Map.entry(2488, -1),
		Map.entry(4971, -1),
		Map.entry(4972, -1),
		Map.entry(4973, -1),
		Map.entry(4974, -1),
		Map.entry(4976, -1),
		Map.entry(21068, -1),
		Map.entry(21071, 31),
		Map.entry(21106, -1),
		Map.entry(21111, -1),
		Map.entry(21135, -1),
		Map.entry(29004, -1));
	private static final Map<Integer, Set<Integer>> OBJECT_STEPS = Map.of(
		1537, Set.of(1, 2, 3),
		21111, Set.of(1));

	@Test
	void legacyHandlersDefineOrderedNpcStepsAndTerminalResponses() throws Exception {
		Map<Integer, Contract> contracts = contracts();
		assertEquals(QUEST_IDS, contracts.keySet());

		for (Contract contract : contracts.values()) {
			QuestDefinition definition = definition(contract.id());
			assertEquals(List.of(contract.startNpc()), dialogNpcs(definition, "unaccepted", 31),
				"quest " + contract.id() + " start NPCs");
			assertEquals(List.of(contract.endNpc()), completionNpcs(definition),
				"quest " + contract.id() + " completion NPCs");

			boolean directReward = DIRECT_REWARD_OPEN_ACTIONS.containsKey(contract.id());
			for (int index = 0; index < contract.progressNpcs().size(); index++) {
				int step = index + 1;
				int npcId = contract.progressNpcs().get(index);
				String source = index == 0 ? "started" : "k" + index;
				String target = directReward && step == contract.progressNpcs().size()
					? "reward" : "k" + step;
				int openAction = OBJECT_STEPS.getOrDefault(contract.id(), Set.of()).contains(step) ? -1 : 31;

				assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(contract.progressPages().get(index))),
					route(definition, source, source, npcId, openAction).afterCommit(),
					"quest " + contract.id() + " step " + step + " open page");
				QuestTransition progress = route(definition, source, target, npcId,
					contract.progressActions().get(index));
				List<AfterCommitAction> expectedResponse = SELECTION_RESPONSE_QUESTS.contains(contract.id())
					? List.of(
						new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
						new AfterCommitAction.ShowQuestSelectionDialog(10))
					: List.of(
						new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
						new AfterCommitAction.CloseDialog());
				assertEquals(expectedResponse, progress.afterCommit(),
					"quest " + contract.id() + " step " + step + " terminal response");
				assertEquals(step, node(definition, target).projection().variables().get("var0"),
					"quest " + contract.id() + " step " + step + " target var0");

				if (openAction == -1) {
					assertTrue(definition.transitions().stream().anyMatch(transition ->
						transition.sourceNode().equals(source)
							&& transition.targetNode().equals(source)
							&& transition.event().equals(new QuestEvent.CanAct(npcId, "ACTION_ITEM_USE"))),
						"quest " + contract.id() + " step " + step + " item-use gate");
				}
			}

			int finalVar = contract.progressNpcs().size();
			assertEquals(finalVar, node(definition, "reward").projection().variables().get("var0"),
				"quest " + contract.id() + " reward var0");
			if (directReward) {
				int openAction = DIRECT_REWARD_OPEN_ACTIONS.get(contract.id());
				assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2375)),
					route(definition, "reward", "reward", contract.endNpc(), openAction).afterCommit(),
					"quest " + contract.id() + " reward report page");
				assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(5)),
					route(definition, "reward", "reward", contract.endNpc(), 1009).afterCommit(),
					"quest " + contract.id() + " reward preview");
			} else {
				String reportSource = "k" + finalVar;
				assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2375)),
					route(definition, reportSource, reportSource, contract.endNpc(), 31).afterCommit(),
					"quest " + contract.id() + " report page");
				assertEquals(List.of(
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
					new AfterCommitAction.ShowQuestDialog(5)),
					route(definition, reportSource, "reward", contract.endNpc(), 1009).afterCommit(),
					"quest " + contract.id() + " report transition");
			}
		}
	}

	@Test
	void activeClientSelectSubpagesAreExecutable() throws Exception {
		Map<Integer, QuestDefinition> definitions = new HashMap<>();
		Map<String, Integer> columns;
		try (BufferedReader reader = Files.newBufferedReader(CLIENT_ACTIONS, StandardCharsets.UTF_8)) {
			columns = columns(reader.readLine());
			for (String line; (line = reader.readLine()) != null;) {
				String[] values = line.split(",", -1);
				int questId = Integer.parseInt(values[columns.get("quest_id")]);
				String action = values[columns.get("action_constant")];
				if (!QUEST_IDS.contains(questId) || !"active".equals(values[columns.get("source_variant")])
						|| !action.matches("HACTION_SELECT\\d+(?:_\\d+)+")) {
					continue;
				}
				int actionId = Integer.parseInt(values[columns.get("action_id")]);
				QuestDefinition definition = definitions.computeIfAbsent(questId,
					id -> ReportToManyLegacyFlowRegressionTest.definition(id));
				List<QuestTransition> routes = definition.transitions().stream()
					.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
						&& talk.dialogId() == actionId)
					.filter(transition -> transition.afterCommit()
						.equals(List.of(new AfterCommitAction.ShowQuestDialog(actionId))))
					.toList();
				assertEquals(1, routes.size(), "quest " + questId + " client action " + action);
			}
		}
	}

	private static Map<Integer, Contract> contracts() throws Exception {
		Map<Integer, Contract> contracts = new HashMap<>();
		try (BufferedReader reader = Files.newBufferedReader(CONTRACTS, StandardCharsets.UTF_8)) {
			Map<String, Integer> columns = columns(reader.readLine());
			for (String line; (line = reader.readLine()) != null;) {
				String[] values = line.split(",", -1);
				int questId = Integer.parseInt(values[columns.get("quest_id")]);
				if (!QUEST_IDS.contains(questId)) {
					continue;
				}
				contracts.put(questId, new Contract(questId,
					one(values[columns.get("start_npc_ids")]),
					one(values[columns.get("end_npc_ids")]),
					numbers(values[columns.get("progress_npc_ids")]),
					numbers(values[columns.get("progress_page_ids")]),
					numbers(values[columns.get("progress_action_ids")])));
			}
		}
		return contracts;
	}

	private static Map<String, Integer> columns(String header) {
		String[] names = header.replace("\uFEFF", "").split(",", -1);
		Map<String, Integer> columns = new HashMap<>();
		for (int index = 0; index < names.length; index++) {
			columns.put(names[index], index);
		}
		return columns;
	}

	private static int one(String value) {
		List<Integer> numbers = numbers(value);
		assertEquals(1, numbers.size(), "expected exactly one NPC in " + value);
		return numbers.getFirst();
	}

	private static List<Integer> numbers(String value) {
		List<Integer> numbers = new ArrayList<>();
		for (String token : value.split(" ")) {
			if (!token.isBlank()) {
				numbers.add(Integer.parseInt(token));
			}
		}
		return List.copyOf(numbers);
	}

	private static QuestTransition route(QuestDefinition definition, String source, String target,
			int npcId, int dialogId) {
		List<QuestTransition> routes = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source)
				&& transition.targetNode().equals(target))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId && talk.dialogId() == dialogId)
			.toList();
		assertEquals(1, routes.size(), "quest " + definition.id() + " " + source + " -> " + target
			+ " npc " + npcId + " dialog " + dialogId);
		return routes.getFirst();
	}

	private static List<Integer> dialogNpcs(QuestDefinition definition, String source, int dialogId) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals(source))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.dialogId() == dialogId)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.distinct().toList();
	}

	private static List<Integer> completionNpcs(QuestDefinition definition) {
		return definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("reward")
				&& transition.targetNode().equals("complete"))
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.distinct().toList();
	}

	private static QuestNode node(QuestDefinition definition, String label) {
		return definition.nodes().stream().filter(node -> node.label().equals(label)).findFirst().orElseThrow();
	}

	private static QuestDefinition definition(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = ReportToManyLegacyFlowRegressionTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new AssertionError("missing resource " + resource);
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		} catch (Exception e) {
			throw new AssertionError("failed to load quest " + questId, e);
		}
	}

	private record Contract(int id, int startNpc, int endNpc, List<Integer> progressNpcs,
			List<Integer> progressPages, List<Integer> progressActions) {
	}
}
