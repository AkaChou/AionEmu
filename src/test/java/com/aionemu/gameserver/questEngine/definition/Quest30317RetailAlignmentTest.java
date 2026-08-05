package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest30317RetailAlignmentTest {
	@Test
	void preservesTheDynamicNpcFlowAndCertificationTurnIn() throws Exception {
		Path path = Path.of("src/main/resources/aion/data/static_data/quest_definition/quests/30317.xml");
		try (InputStream input = Files.newInputStream(path)) {
			QuestDefinition definition = QuestDefinitionXmlCompiler.compile(input).definition();

			assertEquals(List.of(new QuestItemRequirement(182209718, 1), new QuestItemRequirement(182209719, 1)),
				definition.metadata().itemRequirements());

			QuestTransition spawn = transition(definition, "started", "s1", 799322, 10000);
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 0)), spawn.conditions());
			assertEquals(List.of(new QuestAction.SetVariable("var0", 1)), spawn.actions());
			assertTrue(spawn.afterCommit().contains(new AfterCommitAction.SpawnNpc("spirit-slot", 799506,
				new QuestSpawnLocation.PlayerPosition((byte) 0))));

			QuestTransition removeNpc = transition(definition, "s1", "s2", 799506, 10001);
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 1)), removeNpc.conditions());
			assertEquals(List.of(new QuestAction.SetVariable("var0", 2)), removeNpc.actions());
			assertTrue(removeNpc.afterCommit().contains(new AfterCommitAction.DespawnNpc("spirit-slot")));

			QuestTransition turnIn = transition(definition, "s2", "reward", 799208, 31);
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2),
				new QuestCondition.HasItem(182209718, 1),
				new QuestCondition.HasItem(182209719, 1)), turnIn.conditions());
			assertEquals(List.of(new QuestAction.RemoveItem(182209718, 1),
				new QuestAction.RemoveItem(182209719, 1)), turnIn.actions());

			QuestTransition insufficient = definition.transitions().stream()
				.filter(candidate -> "s2".equals(candidate.sourceNode()))
				.filter(candidate -> candidate.event().equals(new QuestEvent.TalkToNpc(799208, 31)))
				.filter(candidate -> "s2".equals(candidate.targetNode()))
				.findFirst().orElseThrow();
			assertEquals(List.of(new QuestCondition.QuestVariableIs("var0", 2)), insufficient.conditions());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(10001)), insufficient.afterCommit());
		}
	}

	private static QuestTransition transition(QuestDefinition definition, String source, String target,
			int npcId, int dialogId) {
		return definition.transitions().stream()
			.filter(candidate -> source.equals(candidate.sourceNode()))
			.filter(candidate -> target.equals(candidate.targetNode()))
			.filter(candidate -> candidate.event().equals(new QuestEvent.TalkToNpc(npcId, dialogId)))
			.findFirst().orElseThrow();
	}
}
