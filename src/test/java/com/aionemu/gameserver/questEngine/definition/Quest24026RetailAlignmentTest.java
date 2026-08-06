package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Retail-anchored structural coverage for the Asmodian Morheim campaign defense. */
class Quest24026RetailAlignmentTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/24026.xml");

	@Test
	void preservesMetadataPrerequisitesAndRewards() throws Exception {
		QuestMetadata metadata = load().definition().metadata();
		assertEquals("A Hand from Each Side", metadata.name());
		assertEquals(1129899, metadata.displayNameId());
		assertEquals(35, metadata.minLevel());
		assertEquals(Integer.MAX_VALUE, metadata.maxLevel());
		assertEquals(Set.of("ASMODIANS"), metadata.permittedRaces());
		assertTrue(metadata.cannotShare());
		assertTrue(metadata.cannotGiveup());
		assertEquals(Set.of(24020, 24021, 24022, 24023, 24024, 24025), metadata.prerequisites());
		// 客户端证据:24026 保留 start-conditions unfinished+noacquired Q2041。
		assertEquals(List.of(new QuestStartCondition("unfinished", 2041, 0),
			new QuestStartCondition("noacquired", 2041, 0)), metadata.startConditions());
		assertEquals(10, metadata.rewards().size());
		assertEquals(new QuestReward("EXP", 0, 3504765), metadata.rewards().get(0));
		assertEquals(new QuestReward("TITLE", 60, 1), metadata.rewards().get(1));
		assertEquals(new QuestReward("ITEM", 186000008, 20), metadata.rewards().get(8));
		assertEquals(new QuestReward("ITEM", 190200000, 10), metadata.rewards().get(9));
		assertTrue(metadata.rewards().stream().noneMatch(reward -> reward.id() == 186000003));
	}

	@Test
	void retainsConversationItemsTeleportAndDefenseLifecycle() throws Exception {
		var transitions = load().definition().transitions();

		QuestTransition aegirStep = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 204301 && Integer.valueOf(10000).equals(talk.dialogId()))
			.findFirst().orElseThrow();
		assertEquals("s1", aegirStep.targetNode());
		assertTrue(aegirStep.actions().contains(new QuestAction.GiveItem(182215371, 1)));
		assertTrue(aegirStep.afterCommit().stream().anyMatch(action -> action instanceof AfterCommitAction.TeleportPlayer teleport
			&& teleport.worldId() == 220020000 && teleport.x() == 2795.9f
				&& teleport.y() == 478.37f && teleport.z() == 265.86f && teleport.heading() == 51));

		QuestTransition taisanStep = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 204403 && Integer.valueOf(10001).equals(talk.dialogId()))
			.findFirst().orElseThrow();
		assertEquals("s2", taisanStep.targetNode());
		assertTrue(taisanStep.actions().contains(new QuestAction.GiveItem(182215372, 1)));

		QuestTransition defenseStart = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 204432 && Integer.valueOf(10002).equals(talk.dialogId()))
			.findFirst().orElseThrow();
		assertEquals("defense", defenseStart.targetNode());
		AfterCommitAction.StartQuestTimer timer = defenseStart.afterCommit().stream()
			.filter(AfterCommitAction.StartQuestTimer.class::isInstance)
			.map(AfterCommitAction.StartQuestTimer.class::cast)
			.findFirst().orElseThrow();
		assertEquals(120, timer.seconds());
		assertEquals(new QuestTimerPolicy.Identity("24026-defense", QuestTimerPolicy.Scope.PLAYER_QUEST),
			timer.policy().identity());
		AfterCommitAction.SpawnNpcRandom spawn = defenseStart.afterCommit().stream()
			.filter(AfterCommitAction.SpawnNpcRandom.class::isInstance)
			.map(AfterCommitAction.SpawnNpcRandom.class::cast)
			.findFirst().orElseThrow();
		assertEquals("defense-mob", spawn.slot());
		assertTrue(spawn.replaceExisting());
		assertEquals(Set.of(213576, 213577, 213578), spawn.variants().stream()
			.map(QuestSpawnVariant::templateId).collect(java.util.stream.Collectors.toSet()));
		assertTrue(defenseStart.afterCommit().contains(new AfterCommitAction.AttackNpcTemplate("defense-mob", 204432)));

		QuestTransition defenseKill = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.KillNpcSet kill
				&& kill.npcIds().equals(Set.of(213576, 213577, 213578, 213579)))
			.findFirst().orElseThrow();
		assertEquals("defense", defenseKill.targetNode());
		assertTrue(defenseKill.afterCommit().stream().anyMatch(AfterCommitAction.SpawnNpcRandom.class::isInstance));
		assertTrue(defenseKill.afterCommit().contains(new AfterCommitAction.AttackNpcTemplate("defense-mob", 204432)));

		assertTrue(transitions.stream().anyMatch(t -> t.event() instanceof QuestEvent.QuestTimerEnd
			&& t.sourceNode().equals("defense") && t.targetNode().equals("defense-done")));
		for (QuestEvent event : new QuestEvent[] {new QuestEvent.Die(), new QuestEvent.LogOut(null)}) {
			assertTrue(transitions.stream().anyMatch(t -> t.event().equals(event)
				&& t.sourceNode().equals("defense") && t.targetNode().equals("s2")
				&& t.afterCommit().contains(new AfterCommitAction.CancelQuestTimer(
					new QuestTimerPolicy.Identity("24026-defense", QuestTimerPolicy.Scope.PLAYER_QUEST)))));
		}
	}

	@Test
	void keepsSixSelectableCompletionBranchesAndRequiredItemRemoval() throws Exception {
		var transitions = load().definition().transitions();
		QuestTransition returnToReward = transitions.stream()
			.filter(t -> t.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 204432 && Integer.valueOf(10003).equals(talk.dialogId()))
			.findFirst().orElseThrow();
		assertEquals("reward", returnToReward.targetNode());
		assertTrue(returnToReward.actions().contains(new QuestAction.RemoveItem(182215371, 1)));

		var completions = transitions.stream()
			.filter(t -> t.sourceNode().equals("reward") && t.targetNode().equals("complete")
				&& t.event() instanceof QuestEvent.TalkToNpc talk && talk.npcId() == 204301
				&& talk.dialogId() != null && talk.dialogId() >= 8 && talk.dialogId() <= 13)
			.toList();
		assertEquals(6, completions.size());
		assertEquals(Set.of(114101704, 114301833, 114301835, 114501742, 114501744, 114601583),
			completions.stream().flatMap(t -> t.actions().stream())
				.filter(QuestAction.GrantReward.class::isInstance)
				.map(QuestAction.GrantReward.class::cast)
				.map(QuestAction.GrantReward::id)
				.filter(Set.of(114101704, 114301833, 114301835, 114501742, 114501744, 114601583)::contains)
				.collect(java.util.stream.Collectors.toSet()));
		assertTrue(completions.stream().allMatch(t -> t.actions().contains(new QuestAction.CompleteQuest(
			((QuestEvent.TalkToNpc) t.event()).dialogId() - 6))));
		assertTrue(completions.stream().allMatch(t -> t.afterCommit().contains(new AfterCommitAction.ShowQuestSelectionDialog(10))));
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(XML)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
