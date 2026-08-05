package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Quest30311RetailAlignmentTest {
	@Test
	void everyTurnInRequiresAndConsumesTheCollectedItem() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/30311.xml");
		try (InputStream input = Files.newInputStream(path)) {
			QuestDefinition definition = QuestDefinitionXmlCompiler.compile(input).definition();
			List<QuestTransition> turnIns = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()))
				.filter(transition -> "reward".equals(transition.targetNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& Integer.valueOf(1009).equals(talk.dialogId()))
				.toList();

			assertEquals(Set.of(799322, 730275), turnIns.stream()
				.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).npcId())
				.collect(Collectors.toSet()));
			for (QuestTransition turnIn : turnIns) {
				assertEquals(List.of(new QuestCondition.HasItem(182209714, 1)), turnIn.conditions());
				assertEquals(List.of(new QuestAction.RemoveItem(182209714, 1)), turnIn.actions());
			}
		}
	}
}
