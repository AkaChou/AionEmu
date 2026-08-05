package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest30313RetailAlignmentTest {
	@Test
	void workAndCollectedItemsAreTransactionalAcrossEveryNpcPath() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/30313.xml");
		try (InputStream input = Files.newInputStream(path)) {
			QuestDefinition definition = QuestDefinitionXmlCompiler.compile(input).definition();
			assertEquals(List.of(new QuestItemRequirement(182209716, 1)),
				definition.metadata().questWorkItems());

			List<QuestTransition> starts = transitions(definition, "unaccepted", "started");
			assertEquals(6, starts.size());
			assertTrue(starts.stream().allMatch(transition ->
				transition.actions().contains(new QuestAction.GiveItem(182209716, 1))));

			List<QuestTransition> turnIns = transitions(definition, "started", "reward");
			assertEquals(3, turnIns.size());
			assertTrue(turnIns.stream().allMatch(transition ->
				transition.conditions().contains(new QuestCondition.HasItem(182209717, 1))
					&& transition.actions().contains(new QuestAction.RemoveItem(182209717, 1))));

			List<QuestTransition> completions = transitions(definition, "reward", "complete");
			assertEquals(6, completions.size());
			assertTrue(completions.stream().allMatch(transition -> transition.actions()
				.contains(new QuestAction.RemoveItem(182209716, QuestAction.RemoveItem.ALL))));
		}
	}

	private static List<QuestTransition> transitions(QuestDefinition definition, String source, String target) {
		return definition.transitions().stream()
			.filter(transition -> source.equals(transition.sourceNode()))
			.filter(transition -> target.equals(transition.targetNode()))
			.toList();
	}
}
