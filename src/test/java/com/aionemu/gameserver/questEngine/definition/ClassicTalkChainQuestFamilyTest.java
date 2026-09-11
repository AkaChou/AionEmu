package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the classic multi-step talk chains for quests 2422 and 25051.
 */
class ClassicTalkChainQuestFamilyTest {

	@Test
	void quest2422UsesTheLegacyTwoStageDialogAndTeleport() throws Exception {
		QuestDefinition definition = definition(2422);
		assertNode(definition, "step0", QuestStatus.START, 0);
		assertNode(definition, "step1", QuestStatus.START, 1);
		assertNode(definition, "reward", QuestStatus.REWARD, 2);

		assertPage(definition, "unaccepted", 204326,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT_NONE);
		assertTalk(definition, "unaccepted", "step0", 204326,
			QuestDialogAction.QUEST_ACCEPT_1, List.of(new QuestCondition.StartEligible()),
			List.of(), List.of(
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_ACCEPT_1.id())));

		assertPage(definition, "step0", 204327,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		assertPage(definition, "step0", 204327,
			QuestDialogAction.SELECT1_1, QuestDialogPage.SELECT1_1);
		assertTalk(definition, "step0", "step1", 204327,
			QuestDialogAction.SETPRO1, List.of(), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));

		assertPage(definition, "step1", 204375,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "step1", 204375,
			QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertPage(definition, "step1", 204375,
			QuestDialogAction.SELECT2_2, QuestDialogPage.SELECT2_2);
		assertTalk(definition, "step1", "step1", 204375,
			QuestDialogAction.FINISH_DIALOG, List.of(), List.of(),
			List.of(new AfterCommitAction.CloseDialog()));
		QuestTransition drink = talk(definition, "step1", 204375,
			QuestDialogAction.SETPRO2, List.of());
		assertEquals("reward", drink.targetNode());
		assertTrue(drink.afterCommit().stream().anyMatch(action ->
			action instanceof AfterCommitAction.TeleportPlayer teleport
				&& teleport.worldId() == 210020000
				&& teleport.x() == 535.46f
				&& teleport.y() == 2555.62f));
		assertEquals(List.of(
			drink.afterCommit().get(0),
			new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), drink.afterCommit());

		assertReportAndCompletion(definition, 204326);
	}

	@Test
	void quest25051UsesTheLegacySpawnAndTimerDialogChain() throws Exception {
		QuestDefinition definition = definition(25051);
		assertEquals(List.of(new QuestItemRequirement(182215720, 1)),
			definition.metadata().questWorkItems());
		for (int step = 0; step <= 3; step++) {
			assertNode(definition, "step" + step, QuestStatus.START, step);
		}
		assertNode(definition, "reward", QuestStatus.REWARD, 4);

		assertPage(definition, "unaccepted", 804915,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT_NONE);
		assertTalk(definition, "unaccepted", "step0", 804915,
			QuestDialogAction.QUEST_ACCEPT_SIMPLE,
			List.of(new QuestCondition.StartEligible()), List.of(),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
				new AfterCommitAction.CloseDialog()));

		assertPage(definition, "step0", 731553,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT1);
		assertTalk(definition, "step0", "step0", 731553,
			QuestDialogAction.FINISH_DIALOG, List.of(), List.of(),
			List.of(new AfterCommitAction.CloseDialog()));
		QuestTransition spawnGuard = talk(definition, "step0", 731553,
			QuestDialogAction.SETPRO1, List.of());
		assertEquals("step1", spawnGuard.targetNode());
		assertSpawn(spawnGuard, "ancient-king-guard", 805160);
		assertTimer(spawnGuard, 300);
		assertEquals(List.of(
			spawnGuard.afterCommit().get(0),
			spawnGuard.afterCommit().get(1),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
			new AfterCommitAction.CloseDialog()), spawnGuard.afterCommit());

		assertPage(definition, "step1", 805160,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT2);
		assertPage(definition, "step1", 805160,
			QuestDialogAction.SELECT2_1, QuestDialogPage.SELECT2_1);
		assertTalk(definition, "step1", "step2", 805160,
			QuestDialogAction.SETPRO2, List.of(), List.of(), List.of(
				new AfterCommitAction.DespawnNpc("ancient-king-guard"),
				new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));
		assertTimerExpiryDespawns(definition, "step1", "ancient-king-guard");

		assertPage(definition, "step2", 731554,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT3);
		assertTalk(definition, "step2", "step2", 731554,
			QuestDialogAction.FINISH_DIALOG, List.of(), List.of(),
			List.of(new AfterCommitAction.CloseDialog()));
		assertTalk(definition, "step2", "step3", 731554,
			QuestDialogAction.SETPRO3, List.of(),
			List.of(new QuestAction.GiveItem(182215720, 1)),
			List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
				new AfterCommitAction.CloseDialog()));

		assertPage(definition, "step3", 731555,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.SELECT4);
		assertTalk(definition, "step3", "step3", 731555,
			QuestDialogAction.FINISH_DIALOG, List.of(), List.of(),
			List.of(new AfterCommitAction.CloseDialog()));
		QuestTransition openTreasure = talk(definition, "step3", 731555,
			QuestDialogAction.SET_SUCCEED, List.of());
		assertEquals("reward", openTreasure.targetNode());
		assertEquals(List.of(new QuestAction.RemoveItem(182215720, 1)),
			openTreasure.actions());
		assertSpawn(openTreasure, "ancient-king-reward", 220031);
		assertTimer(openTreasure, 300);
		assertEquals(List.of(
			openTreasure.afterCommit().get(0),
			openTreasure.afterCommit().get(1),
			new AfterCommitAction.SyncQuestState(
				QuestStateSyncMode.LEVEL_AND_VISIBILITY_REFRESH),
			new AfterCommitAction.CloseDialog()), openTreasure.afterCommit());

		assertReportAndCompletion(definition, 804916);
		assertTimerExpiryDespawns(definition, "reward", "ancient-king-reward");
	}

	private static void assertReportAndCompletion(QuestDefinition definition, int npcId) {
		assertPage(definition, "reward", npcId,
			QuestDialogAction.QUEST_SELECT, QuestDialogPage.DEFAULT_SUCCESS);
		assertPage(definition, "reward", npcId,
			QuestDialogAction.SELECT_QUEST_REWARD,
			QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1);
		QuestTransition completion = talk(definition, "reward", npcId,
			QuestDialogAction.SELECTED_QUEST_REWARD1, List.of());
		assertEquals("complete", completion.targetNode());
		assertTrue(completion.actions().stream()
			.anyMatch(QuestAction.GrantReward.class::isInstance));
		assertTrue(completion.actions().contains(new QuestAction.CompleteQuest(0)));
		assertEquals(List.of(
			new AfterCommitAction.RefreshPlayerStats(),
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
			new AfterCommitAction.ShowQuestSelectionDialog(QuestDialogPage.SELECT_QUEST.id())),
			completion.afterCommit());
	}

	private static void assertSpawn(QuestTransition transition, String slot, int templateId) {
		assertTrue(transition.afterCommit().stream().anyMatch(action ->
			action instanceof AfterCommitAction.SpawnNpc spawn
				&& spawn.slot().equals(slot)
				&& spawn.templateId() == templateId));
	}

	private static void assertTimer(QuestTransition transition, int seconds) {
		assertTrue(transition.afterCommit().stream().anyMatch(action ->
			action instanceof AfterCommitAction.StartInvisibleTimer timer
				&& timer.seconds() == seconds));
	}

	private static void assertTimerExpiryDespawns(QuestDefinition definition, String source,
			String slot) {
		QuestTransition expiry = definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.event() instanceof QuestEvent.InvisibleTimerEnd)
			.findFirst()
			.orElseThrow();
		assertEquals(List.of(new AfterCommitAction.DespawnNpc(slot)), expiry.afterCommit());
	}

	private static void assertPage(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, QuestDialogPage page) {
		assertTalk(definition, source, source, npcId, action, List.of(), List.of(),
			List.of(new AfterCommitAction.ShowQuestDialog(page.id())));
	}

	private static void assertTalk(QuestDefinition definition, String source, String target,
			int npcId, QuestDialogAction action, List<QuestCondition> conditions,
			List<QuestAction> actions, List<AfterCommitAction> afterCommit) {
		QuestTransition transition = talk(definition, source, npcId, action, conditions);
		assertEquals(target, transition.targetNode());
		assertEquals(conditions, transition.conditions());
		assertEquals(actions, transition.actions());
		assertEquals(afterCommit, transition.afterCommit());
	}

	private static QuestTransition talk(QuestDefinition definition, String source, int npcId,
			QuestDialogAction action, List<QuestCondition> conditions) {
		QuestEvent.TalkToNpc event = new QuestEvent.TalkToNpc(npcId, action.id());
		return definition.transitions().stream()
			.filter(candidate -> candidate.sourceNode().equals(source))
			.filter(candidate -> candidate.event().equals(event))
			.filter(candidate -> candidate.conditions().equals(conditions))
			.findFirst()
			.orElseThrow(() -> new AssertionError(
				"missing route " + source + " + NPC " + npcId + " + action " + action.id()));
	}

	private static void assertNode(QuestDefinition definition, String label,
			QuestStatus status, int var0) {
		QuestNode node = definition.nodes().stream()
			.filter(candidate -> candidate.label().equals(label))
			.findFirst()
			.orElseThrow();
		assertEquals(status, node.projection().status());
		assertEquals(Map.of("var0", var0), node.projection().variables());
	}

	private static QuestDefinition definition(int questId) throws Exception {
		try (InputStream input = ClassicTalkChainQuestFamilyTest.class.getResourceAsStream(
				"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			if (input == null) {
				throw new IllegalStateException("missing quest definition " + questId + ".xml");
			}
			return QuestDefinitionXmlCompiler.compile(input).definition();
		}
	}
}
