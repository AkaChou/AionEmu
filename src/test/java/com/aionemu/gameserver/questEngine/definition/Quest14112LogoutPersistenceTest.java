package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 14112 的任务刷怪边：交付 NPC {@code Soul_Kato}(203195) 在本服 {@code spawns/**} 里没有任何静态 spot，
 * 只由本任务在接取/登入时刷到玩家身边；真端家族表没有刷怪列，因此该行按
 * {@code SEMANTIC_GAP:QUEST_SPAWN_UNEXPRESSED} 保留 XML（P0c-6 裁定），本文件即该裁定的证据锁。
 * Quest 14112 keeps its XML (P0c-6, SEMANTIC_GAP:QUEST_SPAWN_UNEXPRESSED): the hand-in NPC exists in
 * the world only through this quest's own spawn edges, which no retail table column can express.
 */
class Quest14112LogoutPersistenceTest {
	private static final Path XML = Path.of(
		"src/main/resources/aion/data/static_data/quest_definition/quests/14112.xml");

	@Test
	void keepsPoisonousBubblegutKillProgressAcrossLogout() throws Exception {
		var definition = load().definition();

		QuestTransition kill = definition.transitions().stream()
			.filter(transition -> transition.event() instanceof QuestEvent.KillNpc(int npcId)
				&& npcId == 210318)
			.findFirst().orElseThrow();
		assertEquals("armed", kill.sourceNode());
		assertEquals("k1", kill.targetNode());
		assertTrue(kill.afterCommit().stream().anyMatch(action ->
			action instanceof AfterCommitAction.SpawnNpc(String slot, int templateId, QuestSpawnLocation location)
				&& slot.equals("kato")
				&& templateId == 203195
				&& location instanceof QuestSpawnLocation.PlayerPosition));

		assertFalse(definition.transitions().stream().anyMatch(transition ->
			transition.sourceNode().equals("k1")
				&& transition.event() instanceof QuestEvent.LogOut));
		assertTrue(definition.transitions().stream().anyMatch(transition ->
			transition.sourceNode().equals("k1")
				&& transition.targetNode().equals("reward")
				&& transition.event() instanceof QuestEvent.TalkToNpc talk
				&& talk.npcId() == 203195));
	}

	@Test
	void restoresKatoAfterReloggingInPostKillStates() throws Exception {
		var definition = load().definition();

		for (String source : List.of("k1", "reward")) {
			QuestTransition enterWorld = definition.transitions().stream()
				.filter(transition -> transition.sourceNode().equals(source)
					&& transition.targetNode().equals(source)
					&& transition.event() instanceof QuestEvent.EnterWorld)
				.findFirst().orElseThrow();
			assertTrue(enterWorld.afterCommit().stream().anyMatch(action ->
				action instanceof AfterCommitAction.SpawnNpc(String slot, int templateId, QuestSpawnLocation location)
					&& slot.equals("kato")
					&& templateId == 203195
					&& location instanceof QuestSpawnLocation.PlayerPosition));
		}
	}

	@Test
	void opensRewardWindowOnTheFirstKatoAnswer() throws Exception {
		var definition = load().definition();

		for (String source : List.of("started", "k1")) {
			QuestTransition answer = definition.transitions().stream()
				.filter(transition -> transition.sourceNode().equals(source)
					&& transition.targetNode().equals("reward")
					&& transition.event() instanceof QuestEvent.TalkToNpc talk
					&& talk.npcId() == 203195
					&& talk.dialogId() == QuestDialogAction.SELECT_QUEST_REWARD.id())
				.findFirst().orElseThrow();
			assertEquals(List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), answer.afterCommit());
		}
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Files.newInputStream(XML)) {
			return QuestDefinitionXmlCompiler.compile(input);
		}
	}
}
