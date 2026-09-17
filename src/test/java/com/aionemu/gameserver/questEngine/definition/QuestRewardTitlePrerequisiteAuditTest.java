package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRewardTitlePrerequisiteAuditTest {
	private static final Map<Integer, Integer> REWARD_TITLE_QUESTS = Map.ofEntries(
		Map.entry(19075, 38),
		Map.entry(11033, 107),
		Map.entry(2434, 66),
		Map.entry(2511, 75),
		Map.entry(29074, 88),
		Map.entry(3922, 38),
		Map.entry(3923, 38),
		Map.entry(3924, 38),
		Map.entry(3925, 38),
		Map.entry(3926, 38),
		Map.entry(3927, 38),
		Map.entry(3928, 38),
		Map.entry(3929, 38),
		Map.entry(4923, 88),
		Map.entry(4924, 88),
		Map.entry(4925, 88),
		Map.entry(4928, 88),
		Map.entry(10521, 306),
		Map.entry(20521, 306));

	@Test
	void noQuestRequiresItsOwnRewardTitle() throws Exception {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		for (CompiledQuestDefinition compiled : catalog.all()) {
			QuestDefinition definition = compiled.definition();
			int titlePrereq = definition.metadata().titleId();
			if (titlePrereq > 0) {
				for (QuestReward reward : definition.metadata().rewards()) {
					if ("TITLE".equalsIgnoreCase(reward.kind())) {
						org.junit.jupiter.api.Assertions.assertNotEquals(titlePrereq, reward.id(),
							() -> "quest " + definition.id() + " requires its own reward title " + titlePrereq);
					}
				}
			}
		}
	}

	@Test
	void rewardTitlesDoNotBecomeStartPrerequisites() throws Exception {
		for (Map.Entry<Integer, Integer> entry : REWARD_TITLE_QUESTS.entrySet()) {
			int questId = entry.getKey();
			int titleId = entry.getValue();
			QuestMetadata metadata = load(questId).definition().metadata();

			assertEquals(0, metadata.titleId(), "quest " + questId + " has an unexpected title prerequisite");
			assertTrue(metadata.rewards().contains(new QuestReward("TITLE", titleId, 1)),
				"quest " + questId + " must keep title " + titleId + " as a reward");
		}
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = QuestRewardTitlePrerequisiteAuditTest.class.getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
