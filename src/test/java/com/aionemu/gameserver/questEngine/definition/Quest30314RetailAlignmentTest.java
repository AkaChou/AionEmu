package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Quest30314RetailAlignmentTest {
	@Test
	void setRewardRequiresAndConsumesTheCertificationItems() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/30314.xml");
		try (InputStream input = Files.newInputStream(path)) {
			QuestDefinition definition = QuestDefinitionXmlCompiler.compile(input).definition();
			QuestTransition setReward = definition.transitions().stream()
				.filter(transition -> "started".equals(transition.sourceNode()))
				.filter(transition -> "reward".equals(transition.targetNode()))
				.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
					&& Integer.valueOf(10255).equals(talk.dialogId()))
				.findFirst().orElseThrow();
			assertEquals(List.of(new QuestCondition.HasItem(186000098, 100)), setReward.conditions());
			assertEquals(List.of(new QuestAction.RemoveItem(186000098, 100)),
				setReward.actions());
		}
	}
}
