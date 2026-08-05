package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Quest30315RetailAlignmentTest {
	@Test
	void everyCertificationTurnInRequiresAndConsumesTheCertificationItems() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/30315.xml");
		try (InputStream input = Files.newInputStream(path)) {
			QuestDefinition definition = QuestDefinitionXmlCompiler.compile(input).definition();
			List<QuestTransition> turnIns = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()))
				.filter(transition -> "reward".equals(transition.targetNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == 799226)
				.toList();

			assertEquals(3, turnIns.size());
			assertEquals(Set.of(39, 20002, 10255), turnIns.stream()
				.map(transition -> ((QuestEvent.TalkToNpc) transition.event()).dialogId())
				.collect(Collectors.toSet()));
			for (QuestTransition turnIn : turnIns) {
				assertEquals(List.of(new QuestCondition.HasItem(186000098, 200)), turnIn.conditions());
				assertEquals(List.of(new QuestAction.RemoveItem(186000098, QuestAction.RemoveItem.ALL)),
					turnIn.actions());
			}
		}
	}
}
