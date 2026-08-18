package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 魔族转职任务 2008 的零售协议结构回归覆盖。
 * Retail-anchored structural coverage for the Asmodian ascension owner.
 */
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
	void entersAscensionInstanceFromFutureExperienceAction() throws Exception {
		CompiledQuestDefinition definition = load();
		assertNode(definition.definition(), "s4", QuestStatus.START, 4);
		assertNode(definition.definition(), "s99", QuestStatus.START, 99);
		QuestTransition enterInstance = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("s4")
				&& transition.targetNode().equals("s99")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203550
				&& Integer.valueOf(QuestDialogAction.SETPRO5.id()).equals(talk.dialogId()))
			.findFirst().orElseThrow();

		assertEquals(List.of(), enterInstance.conditions());
		assertEquals(List.of(), enterInstance.actions());
		assertNull(enterInstance.priority());
		assertEquals(List.of(
			new AfterCommitAction.CloseDialog(),
			new AfterCommitAction.TeleportPlayer(QuestInstanceTarget.nextAvailable(320020000),
				320020000, 457.65f, 426.8f, 230.4f, (byte) 0),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY)), enterInstance.afterCommit());
	}

	@Test
	void followsClientBeliefAndAdvancedClassActions() throws Exception {
		QuestDefinition definition = load().definition();
		assertNode(definition, "s6", QuestStatus.START, 6);
		assertNode(definition, "reward", QuestStatus.REWARD, 7);
		var transitions = definition.transitions();
		QuestTransition beliefPage = route(transitions, "s6", "s6", QuestDialogAction.SELECT6_1, null);
		assertEquals(List.of(), beliefPage.conditions());
		assertEquals(List.of(), beliefPage.actions());
		assertNull(beliefPage.priority());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT6_1.id())),
			beliefPage.afterCommit());

		Map<PlayerClass, QuestDialogPage> classPages = Map.of(
			PlayerClass.WARRIOR, QuestDialogPage.SELECT7,
			PlayerClass.SCOUT, QuestDialogPage.SELECT8,
			PlayerClass.MAGE, QuestDialogPage.SELECT9,
			PlayerClass.PRIEST, QuestDialogPage.SELECT10,
			PlayerClass.TECHNIST, QuestDialogPage.SELECT8_3_3,
			PlayerClass.MUSE, QuestDialogPage.SELECT9_3);
		for (var entry : classPages.entrySet()) {
			QuestCondition.PlayerClassIs condition = new QuestCondition.PlayerClassIs(entry.getKey());
			QuestTransition classPage = route(transitions, "s6", "s6", QuestDialogAction.SETPRO6, condition);
			assertEquals(List.of(condition), classPage.conditions());
			assertEquals(List.of(), classPage.actions());
			assertNull(classPage.priority());
			assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(entry.getValue().id())), classPage.afterCommit());
		}

		Map<PlayerClass, QuestDialogAction> classActions = Map.ofEntries(
			Map.entry(PlayerClass.GLADIATOR, QuestDialogAction.SETPRO7),
			Map.entry(PlayerClass.TEMPLAR, QuestDialogAction.SETPRO8),
			Map.entry(PlayerClass.ASSASSIN, QuestDialogAction.SETPRO9),
			Map.entry(PlayerClass.RANGER, QuestDialogAction.SETPRO10),
			Map.entry(PlayerClass.SORCERER, QuestDialogAction.SETPRO11),
			Map.entry(PlayerClass.SPIRIT_MASTER, QuestDialogAction.SETPRO12),
			Map.entry(PlayerClass.CHANTER, QuestDialogAction.SETPRO13),
			Map.entry(PlayerClass.CLERIC, QuestDialogAction.SETPRO14),
			Map.entry(PlayerClass.GUNSLINGER, QuestDialogAction.SETPRO15),
			Map.entry(PlayerClass.SONGWEAVER, QuestDialogAction.SETPRO16),
			Map.entry(PlayerClass.AETHERTECH, QuestDialogAction.SETPRO17));
		for (var entry : classActions.entrySet()) {
			QuestTransition classChange = transitions.stream()
				.filter(transition -> transition.sourceNode().equals("s6")
					&& transition.targetNode().equals("reward")
					&& transition.afterCommit().contains(new AfterCommitAction.SetPlayerClass(entry.getKey())))
				.findFirst().orElseThrow();
			assertEquals(new QuestEvent.TalkToNpc(203550, entry.getValue().id()), classChange.event());
			assertEquals(List.of(new QuestCondition.PlayerClassIs(startingClass(entry.getKey()))),
				classChange.conditions());
			assertEquals(List.of(), classChange.actions());
			assertNull(classChange.priority());
			assertEquals(List.of(
				new AfterCommitAction.SetPlayerClass(entry.getKey()),
				new AfterCommitAction.TeleportPlayer(220010000, 386.03476f, 1893.9309f, 327.62283f, (byte) 59),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH)),
				classChange.afterCommit());
		}
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

	private static QuestTransition route(List<QuestTransition> transitions, String source, String target,
			QuestDialogAction action, QuestCondition condition) {
		return transitions.stream()
			.filter(transition -> transition.sourceNode().equals(source)
				&& transition.targetNode().equals(target)
				&& transition.event().equals(new QuestEvent.TalkToNpc(203550, action.id()))
				&& (condition == null || transition.conditions().contains(condition)))
			.findFirst().orElseThrow();
	}

	private static void assertNode(QuestDefinition definition, String label, QuestStatus status, int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst().orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static PlayerClass startingClass(PlayerClass advancedClass) {
		return switch (advancedClass) {
			case GLADIATOR, TEMPLAR -> PlayerClass.WARRIOR;
			case ASSASSIN, RANGER -> PlayerClass.SCOUT;
			case SORCERER, SPIRIT_MASTER -> PlayerClass.MAGE;
			case CLERIC, CHANTER -> PlayerClass.PRIEST;
			case GUNSLINGER, AETHERTECH -> PlayerClass.TECHNIST;
			case SONGWEAVER -> PlayerClass.MUSE;
			default -> throw new IllegalArgumentException("not an ascension class: " + advancedClass);
		};
	}
}
