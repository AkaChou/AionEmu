package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression coverage for the migrated owners repaired in this batch. */
class MigratedQuestRepairDefinitionTest {
	@Test
	void diversionOwnersCompleteOnTheSixthKillAndProjectRewardState() {
		for (int questId : List.of(13955, 23955)) {
			CompiledQuestDefinition definition = load(questId);
			QuestNode reward = definition.definition().nodes().stream()
				.filter(node -> node.label().equals("reward")).findFirst().orElseThrow();
			assertEquals(QuestStatus.REWARD, reward.projection().status());
			assertEquals(1, reward.projection().variables().get("var0"));
			assertTrue(definition.definition().transitions().stream()
				.flatMap(transition -> transition.conditions().stream())
				.anyMatch(condition -> condition.equals(new QuestCondition.VariableSumIs(List.of("var1", "var2"), 5))));
			assertTrue(definition.definition().transitions().stream()
				.flatMap(transition -> transition.conditions().stream())
				.anyMatch(condition -> condition.equals(new QuestCondition.VariableSumBelow(List.of("var1", "var2"), 5))));
			assertFalse(definition.definition().transitions().stream()
				.flatMap(transition -> transition.conditions().stream())
				.anyMatch(QuestCondition.VariableAtLeast.class::isInstance));
		}
	}

	@Test
	void infiltrationOwnersResetTheCounterAfterEachTenKillStage() {
		for (int questId : List.of(15322, 25322)) {
			CompiledQuestDefinition definition = load(questId);
			List<QuestTransition> stageCompletions = definition.definition().transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.KillNpcSet
					&& transition.targetNode().matches("s[246]|reward"))
				.toList();
			assertEquals(4, stageCompletions.size());
			assertTrue(stageCompletions.stream().allMatch(transition ->
				transition.conditions().contains(new QuestCondition.VariableAtLeast("var1", 9))
					&& transition.actions().contains(new QuestAction.IncrementVariable("var1", 1))
					&& transition.actions().contains(new QuestAction.SetVariable("var1", 0))));
			for (QuestTransition transition : stageCompletions) {
				int sourceStep = Integer.parseInt(transition.sourceNode().substring(1));
				int packed = definition.definition().progressLayout().pack(Map.of("var0", sourceStep, "var1", 9));
				var plan = QuestMutationPlanner.plan(definition,
					new com.aionemu.gameserver.questEngine.runtime.QuestSnapshot(7, questId, QuestStatus.START,
						packed, Map.of()), transition).orElseThrow();
				assertEquals(0, definition.definition().progressLayout().unpack(plan.nextPackedVariables()).get("var1"));
			}
		}
	}

	@Test
	void groupOwnersDeclareDropsConsumeCollectedItemsAndExposeAllSelectableRewards() {
		assertGroupQuest(15602, 182215994, 1, 703136, 806159,
			Set.of(113601699, 113601700, 113501890, 113501891, 113501892,
				113301934, 113301935, 113101793, 113101794));
		assertGroupQuest(25602, 182216002, 4, 241201, 806171,
			Set.of(113601699, 113601700, 113501890, 113501891, 113501892,
				113301934, 113301935, 113101793, 113101794));
		assertGroupQuest(15604, 0, 0, 0, 806161,
			Set.of(112601697, 112601698, 112501807, 112501808, 112501809,
				112301840, 112301841, 112101727, 112101728));
	}

	private static void assertGroupQuest(int questId, int itemId, int itemCount, int dropNpcId, int rewardNpcId,
		Set<Integer> selectableItems) {
		CompiledQuestDefinition definition = load(questId);
		if (itemId > 0) {
			assertEquals(itemCount, definition.definition().metadata().itemRequirements().stream()
				.filter(item -> item.itemId() == itemId).findFirst().orElseThrow().count());
			assertEquals(1, definition.definition().metadata().drops().stream()
				.filter(drop -> drop.npcId() == dropNpcId && drop.itemId() == itemId
					&& drop.collectingStep() > 0 && drop.eachMember()).count());
			QuestTransition report = definition.definition().transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.dialogId() == 39).findFirst().orElseThrow();
			assertTrue(report.actions().contains(new QuestAction.RemoveItem(itemId, QuestAction.RemoveItem.ALL)));
		} else {
			assertTrue(definition.definition().metadata().drops().isEmpty());
		}

		Set<Integer> declaredSelectable = definition.definition().metadata().rewards().stream()
			.filter(reward -> reward.kind().equals("SELECTABLE_ITEM"))
			.map(QuestReward::id).collect(java.util.stream.Collectors.toSet());
		assertEquals(selectableItems, declaredSelectable);
		List<QuestTransition> completionRoutes = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("reward")
				&& transition.targetNode().equals("complete")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == rewardNpcId)
			.toList();
		assertEquals(9, completionRoutes.size());
		assertEquals(Set.of(8, 9, 10, 11, 12, 13, 14, 15, 16), completionRoutes.stream()
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).dialogId())
			.collect(java.util.stream.Collectors.toSet()));
		assertTrue(completionRoutes.stream().allMatch(transition ->
			transition.actions().stream().anyMatch(action -> action.equals(
				new QuestAction.GrantReward("ITEM", 188055318, 1)))
				&& transition.actions().stream().anyMatch(action -> action instanceof QuestAction.GrantReward reward
					&& reward.rewardKind() == QuestRewardKind.ITEM
					&& selectableItems.contains(reward.id()))));
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = Objects.requireNonNull(
			MigratedQuestRepairDefinitionTest.class.getResourceAsStream(resource), resource)) {
			return QuestDefinitionXmlCompiler.compile(input);
		} catch (Exception e) {
			if (e instanceof QuestCompilationException compilation) {
				throw compilation;
			}
			throw new AssertionError("unable to load " + resource, e);
		}
	}
}
