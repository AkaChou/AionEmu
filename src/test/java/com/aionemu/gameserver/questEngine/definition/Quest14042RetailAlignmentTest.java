package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Retail-anchored coverage for the world-NPC escort in quest 14042. */
class Quest14042RetailAlignmentTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/14042.xml");

	@Test
	void followsTheLiveSearchSquadNpcInsteadOfAStaleCoordinate() throws Exception {
		CompiledQuestDefinition compiled = load();
		QuestTransition follow = compiled.definition().transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 253623 && Integer.valueOf(10003).equals(talk.dialogId()))
			.findFirst().orElseThrow();

		AfterCommitAction.StartFollowCurrentTargetToNpc action = follow.afterCommit().stream()
			.filter(AfterCommitAction.StartFollowCurrentTargetToNpc.class::isInstance)
			.map(AfterCommitAction.StartFollowCurrentTargetToNpc.class::cast)
			.findFirst().orElseThrow();
		assertEquals(253635, action.npcId());
		assertTrue(follow.afterCommit().stream().anyMatch(AfterCommitAction.CloseDialog.class::isInstance));
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(XML)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
