package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest30318To30321RetailAlignmentTest {
	@Test
	void preservesTheRetailPrerequisiteNpcRouteAndRewardChain() throws Exception {
		assertQuest(30318, 30316, 799504,
			Set.of(216276, 216277, 216278, 216279), 25,
			Set.of(new QuestReward("GOLD", 0, 28900), new QuestReward("EXP", 0, 6517414)));
		assertQuest(30319, 30318, 799504, Set.of(216241), 1,
			Set.of(new QuestReward("GOLD", 0, 197550), new QuestReward("EXP", 0, 6517414),
				new QuestReward("ITEM", 182209718, 1)));
		assertQuest(30320, 30316, 799505,
			Set.of(216280, 216281, 216282, 216283, 216284), 25,
			Set.of(new QuestReward("GOLD", 0, 28900), new QuestReward("EXP", 0, 6517414)));
		assertQuest(30321, 30320, 799505,
			Set.of(216161, 216162, 216242, 216243), 1,
			Set.of(new QuestReward("GOLD", 0, 197550), new QuestReward("EXP", 0, 6517414),
				new QuestReward("ITEM", 182209719, 1)));
	}

	private static void assertQuest(int questId, int prerequisite, int npcId,
		Set<Integer> expectedKillNpcIds, int expectedKillCount, Set<QuestReward> expectedRewards) throws Exception {
		QuestDefinition definition = load(questId);
		assertTrue(definition.metadata().startConditions()
			.contains(new QuestStartCondition("finished", prerequisite, 0)));
		assertEquals(expectedRewards, Set.copyOf(definition.metadata().rewards()));
		assertTrue(definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc)
			.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
			.allMatch(id -> id == npcId));

		Set<Integer> declaredKillNpcIds = new HashSet<>();
		for (int id : expectedKillNpcIds) {
			assertEquals(expectedKillCount, killRouteCount(definition, id),
				"unexpected kill count for quest " + questId + " NPC " + id);
			declaredKillNpcIds.add(id);
		}
		assertTrue(killNpcIds(definition).containsAll(declaredKillNpcIds));

		assertTrue(definition.transitions().stream().anyMatch(transition ->
			"reward".equals(transition.sourceNode())
				&& "complete".equals(transition.targetNode())
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == npcId
				&& Integer.valueOf(8).equals(talk.dialogId())));
	}

	private static QuestDefinition load(int questId) throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/" + questId + ".xml");
		try (InputStream input = Files.newInputStream(path)) {
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}

	private static long killRouteCount(QuestDefinition definition, int npcId) {
		return definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc single
				? single.npcId() == npcId
				: transition.event() instanceof QuestEvent.KillNpcSet set
					&& set.npcIds().contains(npcId))
			.count();
	}

	private static Set<Integer> killNpcIds(QuestDefinition definition) {
		Set<Integer> ids = new HashSet<>();
		definition.transitions().forEach(transition -> {
			if (transition.event() instanceof QuestEvent.KillNpc single) {
				ids.add(single.npcId());
			} else if (transition.event() instanceof QuestEvent.KillNpcSet set) {
				ids.addAll(set.npcIds());
			}
		});
		return ids;
	}
}
