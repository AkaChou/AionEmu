package com.aionemu.gameserver.questEngine.definition;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestRandomRewardDefinitionTest {
	private static final Map<Integer, ExpectedQuest> EXPECTED = Map.of(
		13712, new ExpectedQuest(1111612, "ELYOS", 802565),
		13714, new ExpectedQuest(1111614, "ELYOS", 802610),
		23712, new ExpectedQuest(1111638, "ASMODIANS", 802579),
		23714, new ExpectedQuest(1111640, "ASMODIANS", 802596));

	@Test
	void migratedOwnersPreserveRetailItemsAndRandomRewardPool() {
		for (Map.Entry<Integer, ExpectedQuest> entry : EXPECTED.entrySet()) {
			CompiledQuestDefinition compiled = load(entry.getKey());
			QuestDefinition definition = compiled.definition();
			ExpectedQuest expected = entry.getValue();

			assertEquals(expected.displayNameId(), definition.metadata().displayNameId());
			assertEquals(Set.of(expected.race()), definition.metadata().permittedRaces());
			assertEquals(999, definition.metadata().minLevel());
			assertEquals(999, definition.metadata().maxLevel());
			assertEquals(255, definition.metadata().repeatPolicy().maxRepeatCount());
			assertEquals(Set.of(new QuestItemRequirement(186000469, 63),
				new QuestItemRequirement(186000244, 1)), Set.copyOf(definition.metadata().itemRequirements()));
			assertEquals(List.of(new QuestReward("RANDOM", 18505, 1)), definition.metadata().rewards());
			assertTrue(definition.transitions().stream()
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == expected.npcId())
				.anyMatch(transition -> transition.actions().contains(new QuestAction.GrantReward("RANDOM", 18505, 1))
					&& transition.actions().contains(new QuestAction.CompleteQuest(0))));
			assertTrue(definition.transitions().stream()
				.filter(transition -> transition.sourceNode().equals("started") && transition.targetNode().equals("reward"))
				.allMatch(transition -> transition.actions().isEmpty()
					|| transition.actions().containsAll(List.of(new QuestAction.RemoveItem(186000469, 63),
						new QuestAction.RemoveItem(186000244, 1)))));
		}
	}

	@Test
	void catalogOwnsEachQuestOnceAndLegacyTemplateOwnsNone() throws Exception {
		String catalog = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/quest_definition/quest_definition_catalog.xml"));
		String legacy = Files.readString(Path.of(
			"src/main/resources/aion/data/static_data/quest_script_data/levinshor.xml"));

		for (int questId : EXPECTED.keySet()) {
			assertEquals(1, occurrences(catalog, "id=\"" + questId + "\""));
			assertFalse(legacy.contains("id=\"" + questId + "\""));
		}
	}

	private static CompiledQuestDefinition load(int questId) {
		String resource = "/aion/data/static_data/quest_definition/quests/" + questId + ".xml";
		try (InputStream input = QuestRandomRewardDefinitionTest.class.getResourceAsStream(resource)) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input, resource));
		} catch (Exception e) {
			throw new AssertionError("failed to compile quest " + questId, e);
		}
	}

	private static int occurrences(String text, String token) {
		return text.split(java.util.regex.Pattern.quote(token), -1).length - 1;
	}

	private record ExpectedQuest(int displayNameId, String race, int npcId) {
	}
}
