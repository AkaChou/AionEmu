package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Retail-anchored structural coverage for the Asmodian ascension owner. */
class Quest2008RetailAlignmentTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/2008.xml");

	@Test
	void preservesAsmodianMetadataAndThreeTokenGuards() throws Exception {
		CompiledQuestDefinition definition = load();
		QuestMetadata metadata = definition.definition().metadata();
		assertEquals("Ascension", metadata.name());
		assertEquals(1103108, metadata.displayNameId());
		assertEquals(9, metadata.minLevel());
		assertEquals(Set.of("ASMODIANS"), metadata.permittedRaces());
		assertEquals(73200, metadata.rewards().get(0).amount());

		for (int itemId : new int[] {182203009, 182203010, 182203011}) {
			assertTrue(definition.definition().transitions().stream().anyMatch(transition ->
				transition.actions().contains(new QuestAction.GiveItem(itemId, 1))
					&& transition.conditions().contains(new QuestCondition.HasItem(itemId, 1, false))));
		}
		assertTrue(definition.definition().transitions().stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203550 && Integer.valueOf(2376).equals(talk.dialogId())
				&& transition.actions().contains(new QuestAction.RemoveItem(182203011, 1))
				&& transition.afterCommit().contains(new AfterCommitAction.PlayMovie(57))));
	}

	@Test
	void retainsBossHpGateMovieTimerAndElevenClassChanges() throws Exception {
		CompiledQuestDefinition definition = load();
		var transitions = definition.definition().transitions();
		QuestTransition attack = transitions.stream()
			.filter(transition -> transition.event() instanceof QuestEvent.AttackNpc attackEvent
				&& attackEvent.npcId() == 205041)
			.findFirst().orElseThrow();
		assertTrue(attack.conditions().contains(new QuestCondition.NpcHpBelowPercent(205041, 50)));
		assertTrue(attack.afterCommit().contains(new AfterCommitAction.PlayMovie(152)));
		assertTrue(attack.afterCommit().contains(new AfterCommitAction.DespawnNpc("boss")));

		assertTrue(transitions.stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.InvisibleTimerEnd
				&& transition.afterCommit().stream().anyMatch(action -> action instanceof AfterCommitAction.SpawnNpc spawn
					&& spawn.templateId() == 205040)));
		assertEquals(4, transitions.stream().filter(transition -> transition.event() instanceof QuestEvent.KillNpc kill
			&& kill.npcId() == 205040).count());
		assertTrue(transitions.stream().anyMatch(transition ->
			transition.event() instanceof QuestEvent.MovieEnd movie && movie.movieId() == 152
				&& transition.targetNode().equals("s6")
				&& transition.afterCommit().stream().anyMatch(action -> action instanceof AfterCommitAction.SpawnNpc spawn
					&& spawn.templateId() == 203550)));

		long classChoices = transitions.stream()
			.filter(transition -> transition.sourceNode().equals("s6")
				&& transition.targetNode().equals("reward")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203550)
			.filter(transition -> transition.afterCommit().stream()
				.anyMatch(AfterCommitAction.SetPlayerClass.class::isInstance))
			.count();
		assertEquals(11, classChoices);
		assertTrue(transitions.stream().anyMatch(transition -> transition.afterCommit()
			.contains(new AfterCommitAction.SetPlayerClass(PlayerClass.AETHERTECH))));
	}

	@Test
	void keepsDeathExitRecoveryAndRewardCompletion() throws Exception {
		CompiledQuestDefinition definition = load();
		var transitions = definition.definition().transitions();
		assertEquals(4, transitions.stream().filter(transition -> transition.event() instanceof QuestEvent.Die
			&& transition.targetNode().equals("s4")
			&& transition.afterCommit().stream().anyMatch(action -> action instanceof AfterCommitAction.SendSystemMessage))
			.count());
		assertEquals(7, transitions.stream().filter(transition -> transition.event() instanceof QuestEvent.EnterWorld
			&& transition.targetNode().equals("s4")
			&& transition.afterCommit().stream().anyMatch(action -> action instanceof AfterCommitAction.SendSystemMessage))
			.count());
		assertEquals(7, transitions.stream().filter(transition -> transition.event() instanceof QuestEvent.EnterWorld
			&& transition.afterCommit().stream().anyMatch(action -> action instanceof AfterCommitAction.Morph morph
				&& morph.ascensionId() == 1)).count());

		QuestTransition completion = transitions.stream()
			.filter(transition -> transition.sourceNode().equals("reward")
				&& transition.targetNode().equals("complete"))
			.findFirst().orElseThrow();
		assertTrue(completion.event() instanceof QuestEvent.TalkToNpc talk
			&& talk.npcId() == 203550 && talk.dialogId() >= 8 && talk.dialogId() <= 23);
		assertEquals(16, transitions.stream().filter(transition -> transition.sourceNode().equals("reward")
			&& transition.targetNode().equals("complete")).count());
		assertTrue(completion.actions().contains(new QuestAction.GrantReward(
			"EXP", 0, 73200, QuestRewardAmountMode.QUEST_BASE)));
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(XML)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
