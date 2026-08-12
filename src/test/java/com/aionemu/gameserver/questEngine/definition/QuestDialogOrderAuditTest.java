package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDialogOrderAuditTest {
	@Test
	void activeClientPagesHaveRoutesInTheCompiledQuestIr() throws Exception {
		Path details = Path.of("docs/quest/client-dialog-mapping/quest-dialog-action-details.csv");
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<QuestDialogOrderAudit.AuditRow> rows = QuestDialogOrderAudit.audit(
			catalog, QuestDialogOrderAudit.readClientPages(details));

		assertFalse(rows.isEmpty());
		assertTrue(rows.stream().allMatch(row -> Set.of("VERIFIED", "UNRESOLVED", "UNREACHED")
			.contains(row.auditStatus())));
		assertTrue(rows.stream()
			.filter(row -> !row.auditStatus().equals("VERIFIED"))
			.allMatch(row -> row.fixStatus().equals("UNRESOLVED") && !row.unresolvedReason().isBlank()));
		assertEquals(List.of(), rows.stream()
			.filter(row -> row.questId() == 1913)
			.filter(row -> !row.auditStatus().equals("VERIFIED"))
			.map(row -> row.actualPath() + "; " + row.unresolvedReason())
			.toList());
		assertEquals(List.of(), rows.stream()
			.filter(row -> Set.of(1993, 1994, 2993, 2994, 80292, 80293, 80296, 80297)
				.contains(row.questId()))
			.filter(row -> !row.auditStatus().equals("VERIFIED"))
			.map(row -> row.questId() + ": " + row.actualPath() + "; " + row.unresolvedReason())
			.toList());
	}

	@Test
	void reportsCompiledPagesMissingFromTheActiveClientMap() {
		QuestCatalog catalog = catalog(90001, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of()))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(100001, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(9999)), null, "unaccepted")));
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = Map.of(90001,
			new QuestDialogOrderAudit.ClientQuest("QUEST_Q90001.html", Map.of()));

		List<QuestDialogOrderAudit.AuditRow> rows = QuestDialogOrderAudit.audit(catalog, clientQuests);

		assertEquals(1, rows.size());
		assertEquals("UNRESOLVED", rows.getFirst().auditStatus());
		assertEquals("9999", rows.getFirst().shownPage());
		assertTrue(rows.getFirst().unresolvedReason().contains("absent from the active client details"));
	}

	@Test
	void ignoresKnownGenericPagesOutsideTheTaskHtmlSpace() {
		QuestCatalog catalog = catalog(90003, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of()))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(100003, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SHOW_SELECT_QUEST_REWARD_WINDOW1.id())),
				null, "unaccepted")));
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = Map.of(90003,
			new QuestDialogOrderAudit.ClientQuest("QUEST_Q90003.html", Map.of()));

		assertEquals(List.of(), QuestDialogOrderAudit.audit(catalog, clientQuests));
	}

	@Test
	void ignoresTheCommonFailedQuestFeedbackPage() {
		QuestCatalog catalog = catalog(90004, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of()))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(100004, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.QUEST_FAILED_1.id())),
				null, "unaccepted")));
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = Map.of(90004,
			new QuestDialogOrderAudit.ClientQuest("QUEST_Q90004.html", Map.of()));

		assertEquals(List.of(), QuestDialogOrderAudit.audit(catalog, clientQuests));
	}

	@Test
	void genericDialogRoutesProjectAllQuestStateConditions() {
		QuestCatalog catalog = catalog(90002, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of("a", 2, "b", 1))),
			new QuestNode("next", new NodeProjection(QuestStatus.START, Map.of("a", 3, "b", 1)))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(100002, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(500)), null, "unaccepted"),
			new QuestTransition(new QuestEvent.TalkToNpc(100002, 42), List.of(
				new QuestCondition.StatusIs(QuestStatus.START),
				new QuestCondition.QuestVariableIs("a", 2),
				new QuestCondition.VariableAtLeast("a", 2),
				new QuestCondition.VariableBelow("a", 3),
				new QuestCondition.VariableSumIs(List.of("a", "b"), 3),
				new QuestCondition.VariableSumBelow(List.of("a", "b"), 4),
				new QuestCondition.HasItem(100, 1)), List.of(), "next", List.of(), null, null)));
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = Map.of(90002,
			new QuestDialogOrderAudit.ClientQuest("QUEST_Q90002.html", Map.of(500,
				new QuestDialogOrderAudit.ClientPage(500, "500", Map.of(42,
					new QuestDialogOrderAudit.ClientAction(42, "QUEST_Q90002.html#500")), "QUEST_Q90002.html#500"))));

		List<QuestDialogOrderAudit.AuditRow> rows = QuestDialogOrderAudit.audit(catalog, clientQuests);

		assertEquals(1, rows.size());
		assertEquals("VERIFIED", rows.getFirst().auditStatus());
	}

	@Test
	void writesUtf8BomForSpreadsheetCompatibility() throws Exception {
		Path output = Files.createTempFile("quest-order-audit", ".csv");
		try {
			QuestDialogOrderAudit.write(output, List.of());
			assertEquals("\ufeffquest_id", Files.readString(output).substring(0, 9));
		} finally {
			Files.deleteIfExists(output);
		}
	}

	private static QuestCatalog catalog(int id, List<QuestNode> nodes, List<QuestTransition> transitions) {
		QuestDefinition definition = new QuestDefinition(id, 1, QuestMetadata.minimal("audit", id, "QUEST"),
			ProgressLayout.empty(), nodes, transitions);
		return new ImmutableQuestCatalog(List.of(new CompiledQuestDefinition(definition)));
	}
}
