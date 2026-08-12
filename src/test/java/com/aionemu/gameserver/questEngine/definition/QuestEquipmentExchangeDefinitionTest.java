package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestEquipmentExchangeDefinitionTest {
	private static final int MATERIAL_ITEM_ID = 186000041;
	private static final List<ExpectedQuest> QUESTS = List.of(
		new ExpectedQuest(1993, 203753, 1, 5, List.of(5, 5, 5, 5, 5)),
		new ExpectedQuest(1994, 203754, 1, 6, List.of(6, 3, 2, 3, 3, 1)),
		new ExpectedQuest(2993, 204076, 1, 5, List.of(5, 5, 5, 5, 5)),
		new ExpectedQuest(2994, 204077, 1, 6, List.of(6, 3, 2, 3, 3, 1)),
		new ExpectedQuest(80292, 831384, 10, 5, List.of(5, 5, 5, 5, 5)),
		new ExpectedQuest(80293, 831384, 20, 6, List.of(6, 3, 2, 3, 3, 1)),
		new ExpectedQuest(80296, 831387, 10, 5, List.of(5, 5, 5, 5, 5)),
		new ExpectedQuest(80297, 831387, 20, 6, List.of(6, 3, 2, 3, 3, 1)));

	@Test
	void exchangeDefinitionsKeepTheClientActionAndRewardContracts() throws Exception {
		for (ExpectedQuest expected : QUESTS) {
			CompiledQuestDefinition compiled = compile(expected.questId());
			QuestDefinition definition = compiled.definition();
			assertEquals(expected.rewardGroups(), definition.metadata().rewardGroups().size(), expected.label());

			List<QuestTransition> talks = definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc).toList();
			assertTrue(talks.stream().allMatch(transition -> talk(transition).npcId() == expected.npcId()),
				expected.label());
			assertTrue(talks.stream().anyMatch(transition -> talk(transition).dialogId() == QuestDialogAction.EXCHANGE_COIN.id()
				&& "unaccepted".equals(transition.sourceNode()) && "started".equals(transition.targetNode())),
				expected.label());

			List<QuestTransition> completions = talks.stream()
				.filter(transition -> "reward".equals(transition.sourceNode()))
				.filter(transition -> "complete".equals(transition.targetNode())).toList();
			assertFalse(completions.isEmpty(), expected.label());
			assertTrue(completions.stream().allMatch(transition ->
				transition.actions().contains(new QuestAction.RemoveItem(MATERIAL_ITEM_ID, expected.materialCount()))),
				expected.label());
			assertTrue(completions.stream().noneMatch(transition -> {
				int action = talk(transition).dialogId();
				return action >= 24 && action <= 32;
			}), expected.label());

			for (int group = 0; group < expected.selectableCounts().size(); group++) {
				int groupValue = group + 1;
				Set<Integer> actualActions = completions.stream()
					.filter(transition -> hasVariable(transition, "rewardGroup", groupValue))
					.map(QuestEquipmentExchangeDefinitionTest::talk)
					.map(QuestEvent.TalkToNpc::dialogId)
					.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
				Set<Integer> expectedActions = new LinkedHashSet<>();
				for (int choice = 0; choice < expected.selectableCounts().get(group); choice++) {
					expectedActions.add(QuestDialogAction.SELECTED_QUEST_REWARD1.id() + choice);
				}
				assertEquals(expectedActions, actualActions, expected.label() + " reward group " + group);
			}
		}
	}

	@Test
	void alternateSwordCandidateRemovesOnlyTheOwnedItemAtFinalReward() throws Exception {
		CompiledQuestDefinition definition = compile(80293);
		int firstSword = 100001449;
		int ownedSword = 100001451;
		Map<Integer, Integer> inventory = Map.of(ownedSword, 1, MATERIAL_ITEM_ID, 20);

		QuestMutationPlan selected = route(definition,
			snapshot(definition, QuestStatus.START, 0, 0, inventory),
			new QuestEvent.TalkToNpc(831384, QuestDialogAction.SELECT1_1_1.id())).orElseThrow();
		assertEquals(Map.of("equipment", 1, "rewardGroup", 0),
			definition.definition().progressLayout().unpack(selected.nextPackedVariables()));
		assertTrue(selected.requiredActions().stream().noneMatch(QuestAction.RemoveItem.class::isInstance));

		QuestMutationPlan reward = route(definition,
			snapshot(definition, QuestStatus.START, 1, 0, inventory),
			new QuestEvent.TalkToNpc(831384, QuestDialogAction.SETPRO1.id())).orElseThrow();
		assertEquals(QuestStatus.REWARD, reward.nextStatus());
		assertTrue(reward.requiredActions().stream().noneMatch(QuestAction.RemoveItem.class::isInstance));

		QuestMutationPlan completed = route(definition,
			snapshot(definition, QuestStatus.REWARD, 1, 1, inventory),
			new QuestEvent.TalkToNpc(831384, QuestDialogAction.SELECTED_QUEST_REWARD1.id())).orElseThrow();
		List<QuestAction.RemoveItem> removals = completed.requiredActions().stream()
			.filter(QuestAction.RemoveItem.class::isInstance)
			.map(QuestAction.RemoveItem.class::cast).toList();
		assertTrue(removals.contains(new QuestAction.RemoveItem(ownedSword, 1)));
		assertTrue(removals.contains(new QuestAction.RemoveItem(MATERIAL_ITEM_ID, 20)));
		assertFalse(removals.contains(new QuestAction.RemoveItem(firstSword, 1)));
	}

	private static boolean hasVariable(QuestTransition transition, String field, int value) {
		return transition.conditions().stream().anyMatch(condition ->
			condition.equals(new QuestCondition.QuestVariableIs(field, value)));
	}

	private static QuestEvent.TalkToNpc talk(QuestTransition transition) {
		return assertInstanceOf(QuestEvent.TalkToNpc.class, transition.event());
	}

	private static Optional<QuestMutationPlan> route(CompiledQuestDefinition definition,
			QuestSnapshot snapshot, QuestEvent event) {
		List<QuestTransition> candidates = new ArrayList<>(definition.definition().transitions());
		candidates.sort((left, right) -> Integer.compare(
			left.priority() == null ? Integer.MAX_VALUE : left.priority(),
			right.priority() == null ? Integer.MAX_VALUE : right.priority()));
		return candidates.stream()
			.map(transition -> QuestMutationPlanner.plan(definition, snapshot, event, transition))
			.flatMap(Optional::stream).findFirst();
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status,
			int equipment, int rewardGroup, Map<Integer, Integer> inventory) {
		int packed = definition.definition().progressLayout().pack(Map.of(
			"equipment", equipment, "rewardGroup", rewardGroup));
		return new QuestSnapshot(7, definition.id(), status, packed, inventory)
			.withStartEligibility(com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility.allowed());
	}

	private static CompiledQuestDefinition compile(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = QuestEquipmentExchangeDefinitionTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing resource " + resource);
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}

	private record ExpectedQuest(int questId, int npcId, int materialCount, int rewardGroups,
			List<Integer> selectableCounts) {
		private String label() {
			return "quest " + questId;
		}
	}
}
