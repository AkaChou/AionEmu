package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Quest30312RetailAlignmentTest {
	@Test
	void dropsAndEveryTurnInMatchRetailItemRequirements() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/30312.xml");
		try (InputStream input = Files.newInputStream(path)) {
			QuestDefinition definition = QuestDefinitionXmlCompiler.compile(input).definition();
			assertEquals(Set.of(
				new QuestDrop(216007, 182209715, 50, false, 0),
				new QuestDrop(216008, 182209715, 50, false, 0)
			), Set.copyOf(definition.metadata().drops()));

			List<QuestTransition> turnIns = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()))
				.filter(transition -> "reward".equals(transition.targetNode()))
				.toList();
			assertEquals(Set.of(39, 20002, 10255), turnIns.stream()
				.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).dialogId())
				.collect(java.util.stream.Collectors.toSet()));
			for (QuestTransition turnIn : turnIns) {
				assertEquals(List.of(new QuestCondition.HasItem(182209715, 20)), turnIn.conditions());
				assertEquals(List.of(new QuestAction.RemoveItem(182209715, 20)),
					turnIn.actions());
			}
		}
	}
}
