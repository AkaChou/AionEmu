package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDialogSequenceAuditTest {
	@Test
	void forwardClientOrderProducesARowWithoutBackwardFlag() {
		QuestCatalog catalog = catalog(91001, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of()))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(910001, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(500)), null, "unaccepted"),
			new QuestTransition(new QuestEvent.TalkToNpc(910001, 42), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(501)), null, "started")));
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = Map.of(91001,
			new QuestDialogOrderAudit.ClientQuest("QUEST_Q91001.html", Map.of(
				500, new QuestDialogOrderAudit.ClientPage(500, "first", 1, Map.of(42,
					new QuestDialogOrderAudit.ClientAction(42, "QUEST_Q91001.html#first")), Map.of(),
					"QUEST_Q91001.html#first"),
				501, new QuestDialogOrderAudit.ClientPage(501, "second", 2, Map.of(), Map.of(),
					"QUEST_Q91001.html#second"))));

		List<QuestDialogSequenceAudit.Row> rows = QuestDialogSequenceAudit.audit(catalog, clientQuests);

		assertEquals(1, rows.size());
		assertEquals(1, rows.getFirst().prevPageOrder());
		assertEquals(2, rows.getFirst().nextPageOrder());
		assertEquals("FORWARD", rows.getFirst().pattern());
	}

	@Test
	void backwardClientOrderProducesARow() {
		QuestCatalog catalog = catalog(91002, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of()))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(910002, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(500)), null, "unaccepted"),
			new QuestTransition(new QuestEvent.TalkToNpc(910002, 42), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(501)), null, "started")));
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = Map.of(91002,
			new QuestDialogOrderAudit.ClientQuest("QUEST_Q91002.html", Map.of(
				500, new QuestDialogOrderAudit.ClientPage(500, "later", 2, Map.of(42,
					new QuestDialogOrderAudit.ClientAction(42, "QUEST_Q91002.html#later")), Map.of(),
					"QUEST_Q91002.html#later"),
				501, new QuestDialogOrderAudit.ClientPage(501, "earlier", 1, Map.of(), Map.of(),
					"QUEST_Q91002.html#earlier"))));

		List<QuestDialogSequenceAudit.Row> rows = QuestDialogSequenceAudit.audit(catalog, clientQuests);

		assertEquals(1, rows.size());
		QuestDialogSequenceAudit.Row row = rows.getFirst();
		assertEquals(91002, row.questId());
		assertEquals("910002", row.npcId());
		assertEquals("later", row.prevClientPage());
		assertEquals(2, row.prevPageOrder());
		assertEquals("earlier", row.nextClientPage());
		assertEquals(1, row.nextPageOrder());
		assertEquals("ORDER_VIOLATION", row.pattern());
		assertTrue(row.path().contains("page 501"));
	}

	@Test
	void sameOrderTerminalSuccessionStillProducesARow() {
		QuestCatalog catalog = catalog(91003, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of()))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(910003, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(500)), null, "unaccepted"),
			new QuestTransition(new QuestEvent.TalkToNpc(910003, 42), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(501)), null, "started")));
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = Map.of(91003,
			new QuestDialogOrderAudit.ClientQuest("QUEST_Q91003.html", Map.of(
				500, new QuestDialogOrderAudit.ClientPage(500, "start", 1, Map.of(42,
					new QuestDialogOrderAudit.ClientAction(42, "QUEST_Q91003.html#start")), Map.of(),
					"QUEST_Q91003.html#start"),
				501, new QuestDialogOrderAudit.ClientPage(501, "success", 2, Map.of(), Map.of(),
					"QUEST_Q91003.html#success"))));

		List<QuestDialogSequenceAudit.Row> rows = QuestDialogSequenceAudit.audit(catalog, clientQuests);

		assertEquals(1, rows.size());
		assertEquals(1, rows.getFirst().prevPageOrder());
		assertEquals(2, rows.getFirst().nextPageOrder());
		assertEquals("FORWARD", rows.getFirst().pattern());
	}

	@Test
	void genericPagesWithoutClientEntryAreIgnored() {
		QuestCatalog catalog = catalog(91004, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of()))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(910004, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), null, "unaccepted")));
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = Map.of(91004,
			new QuestDialogOrderAudit.ClientQuest("QUEST_Q91004.html", Map.of()));

		assertEquals(List.of(), QuestDialogSequenceAudit.audit(catalog, clientQuests));
	}

	@Test
	void backwardJumpBetweenCyclePagesIsClassifiedCycleNormal() {
		QuestCatalog catalog = catalog(91005, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of()))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(910005, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())), null, "unaccepted"),
			new QuestTransition(new QuestEvent.TalkToNpc(910005, 42), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(
					QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id())), null, "started")));
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = Map.of(91005,
			new QuestDialogOrderAudit.ClientQuest("QUEST_Q91005.html", Map.of(
				QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id(), new QuestDialogOrderAudit.ClientPage(
					QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id(), "select_quest_reward1", 2, Map.of(42,
						new QuestDialogOrderAudit.ClientAction(42, "QUEST_Q91005.html#select_quest_reward1")), Map.of(),
					"QUEST_Q91005.html#select_quest_reward1"),
				QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id(), new QuestDialogOrderAudit.ClientPage(
					QuestDialogPage.SHOW_ASK_QUEST_ACCEPT_WINDOW.id(), "ask_quest_accept", 1, Map.of(), Map.of(),
					"QUEST_Q91005.html#ask_quest_accept"))));

		List<QuestDialogSequenceAudit.Row> rows = QuestDialogSequenceAudit.audit(catalog, clientQuests);

		assertEquals(1, rows.size());
		assertEquals("CYCLE_NORMAL", rows.getFirst().pattern());
	}

	private static QuestCatalog catalog(int id, List<QuestNode> nodes, List<QuestTransition> transitions) {
		QuestDefinition definition = new QuestDefinition(id, 1, QuestMetadata.minimal("audit", id, "QUEST"),
			ProgressLayout.empty(), nodes, transitions);
		return new ImmutableQuestCatalog(List.of(new CompiledQuestDefinition(definition)));
	}
}
