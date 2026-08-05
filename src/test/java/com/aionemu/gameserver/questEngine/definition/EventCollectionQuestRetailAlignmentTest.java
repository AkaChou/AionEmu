package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventCollectionQuestRetailAlignmentTest {
	private static final Map<Integer, Expected> EXPECTED = Map.ofEntries(
		Map.entry(80175, expected(true, 188051715, 1, 169610082, 1)),
		Map.entry(80179, expected(true, 188051715, 1, 169610082, 1)),
		Map.entry(80183, expected(false, 169610081, 1, 188051714, 1)),
		Map.entry(80187, expected(false, 169610081, 1, 188051714, 1)),
		Map.entry(80191, expected(false, 169610082, 1, 188051761, 1)),
		Map.entry(80195, expected(false, 169610082, 1, 188051761, 1)),
		Map.entry(80196, expected(true, 182215185, 1)),
		Map.entry(80200, expected(true, 182215187, 1)),
		Map.entry(80206, expected(false, 121000401, 1)),
		Map.entry(80210, expected(false, 160010133, 10))
	);

	@Test
	void completedEventCollectionDefinitionsMatchRetailMetadataAndRewards() throws Exception {
		ClassLoader loader = getClass().getClassLoader();
		for (var entry : EXPECTED.entrySet()) {
			int questId = entry.getKey();
			String resource = "aion/data/static_data/quest_definition/quests/" + questId + ".xml";
			try (InputStream input = loader.getResourceAsStream(resource)) {
				assertNotNull(input, resource);
				QuestDefinition definition = QuestDefinitionXmlCompiler.compile(input).definition();
				QuestMetadata metadata = definition.metadata();
				assertEquals(entry.getValue().cannotShare(), metadata.cannotShare(), "quest " + questId);
				assertEquals(RepeatPolicy.once(), metadata.repeatPolicy(), "quest " + questId);
				assertEquals(Set.of(), metadata.repeatCycles(), "quest " + questId);

				Map<Integer, Long> declaredItems = metadata.rewards().stream()
					.filter(reward -> "ITEM".equals(reward.kind()))
					.collect(Collectors.toMap(QuestReward::id, QuestReward::amount));
				assertEquals(entry.getValue().itemRewards(), declaredItems, "quest " + questId);

				Map<Integer, Long> grantedItems = definition.transitions().stream()
					.flatMap(transition -> transition.actions().stream())
					.filter(QuestAction.GrantReward.class::isInstance)
					.map(QuestAction.GrantReward.class::cast)
					.filter(reward -> reward.rewardKind() == QuestRewardKind.ITEM)
					.distinct()
					.collect(Collectors.toMap(QuestAction.GrantReward::id, QuestAction.GrantReward::amount));
				assertEquals(entry.getValue().itemRewards(), grantedItems, "quest " + questId);

				assertTrue(definition.transitions().stream()
					.flatMap(transition -> transition.actions().stream())
					.filter(QuestAction.GrantReward.class::isInstance)
					.map(QuestAction.GrantReward.class::cast)
					.filter(reward -> reward.rewardKind() == QuestRewardKind.GOLD
						|| reward.rewardKind() == QuestRewardKind.EXP)
					.allMatch(reward -> reward.amountMode() == QuestRewardAmountMode.QUEST_BASE),
					"quest " + questId);
			}
		}
	}

	private static Expected expected(boolean cannotShare, int itemId, long amount) {
		return new Expected(cannotShare, Map.of(itemId, amount));
	}

	private static Expected expected(boolean cannotShare, int firstItemId, long firstAmount,
			int secondItemId, long secondAmount) {
		return new Expected(cannotShare, Map.of(firstItemId, firstAmount, secondItemId, secondAmount));
	}

	private record Expected(boolean cannotShare, Map<Integer, Long> itemRewards) {
	}
}
