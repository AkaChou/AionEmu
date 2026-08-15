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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestDialogOrderAuditTest {
	private static final Set<Integer> CONFIRMED_ACCEPT_DIALOG_QUESTS = Set.of(
		1354, 1614, 2284, 2333, 2392, 2436, 2493, 2533, 2611, 3001, 3006, 3020, 3023, 3035, 3050,
		3092, 3200, 3208, 3721, 3725, 4051, 4082, 4200, 4718, 4721, 4725, 11006, 11033, 11040, 11147,
		14120, 14123, 14153, 18500, 18511, 19000, 21136, 21467, 28511, 30051, 30151, 30227, 30327,
		80016, 80018);
	private static final Set<String> ACCEPT_DIALOG_PAGES = Set.of("4", "1003", "1004");

	@Test
	void activeClientPagesHaveRoutesInTheCompiledQuestIr() throws Exception {
		Path pages = Path.of("docs/quest/client-dialog-mapping/quest-dialog-pages.csv");
		Path details = Path.of("docs/quest/client-dialog-mapping/quest-dialog-action-details.csv");
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<QuestDialogOrderAudit.AuditRow> rows = QuestDialogOrderAudit.audit(
			catalog, QuestDialogOrderAudit.readClientPages(pages, details));

		assertFalse(rows.isEmpty());
		assertTrue(rows.stream().allMatch(row -> Set.of("PAGE_ACTION_MATCHED", "TERMINAL_PAGE_REACHED",
			"EVIDENCE_REQUIRED", "CLIENT_PAGE_UNREACHED")
			.contains(row.auditStatus())));
		assertTrue(rows.stream()
			.filter(row -> Set.of("EVIDENCE_REQUIRED", "CLIENT_PAGE_UNREACHED").contains(row.auditStatus()))
			.allMatch(row -> row.fixStatus().equals("EVIDENCE_REQUIRED") && !row.unresolvedReason().isBlank()));
		assertTrue(rows.stream().filter(row -> row.auditStatus().equals("PAGE_ACTION_MATCHED"))
			.allMatch(row -> row.candidateCount() > 0 && row.candidate() != null
				&& row.candidate().index() >= 1 && row.candidate().index() <= row.candidateCount()));
		assertEquals(List.of(), rows.stream()
			.filter(row -> CONFIRMED_ACCEPT_DIALOG_QUESTS.contains(row.questId()))
			.filter(row -> Set.of("EVIDENCE_REQUIRED", "CLIENT_PAGE_UNREACHED").contains(row.auditStatus()))
			.filter(row -> (row.serverSourceState().equals("unaccepted")
					&& (row.clientVisibleAction().equals("1007") || ACCEPT_DIALOG_PAGES.contains(row.shownPage())))
				|| (row.auditStatus().equals("CLIENT_PAGE_UNREACHED")
					&& ACCEPT_DIALOG_PAGES.contains(row.shownPage())))
			.map(row -> row.questId() + ": " + row.actualPath() + "; " + row.unresolvedReason())
			.toList());
		assertTrue(rows.stream()
			.filter(row -> row.auditStatus().equals("EVIDENCE_REQUIRED")
				&& row.unresolvedReason().startsWith("visible client action has no route"))
			.allMatch(row -> row.candidateCount() == 0 && row.candidate() == null));
		assertEquals(List.of(), rows.stream()
			.filter(row -> row.questId() == 1149)
			.filter(row -> Set.of("EVIDENCE_REQUIRED", "CLIENT_PAGE_UNREACHED").contains(row.auditStatus()))
			.map(row -> row.actualPath() + "; " + row.unresolvedReason())
			.toList());
		assertEquals(List.of(), rows.stream()
			.filter(row -> row.questId() == 1913)
			.filter(row -> Set.of("EVIDENCE_REQUIRED", "CLIENT_PAGE_UNREACHED").contains(row.auditStatus()))
			.map(row -> row.actualPath() + "; " + row.unresolvedReason())
			.toList());
		assertEquals(List.of(), rows.stream()
			.filter(row -> row.questId() == 25512)
			.filter(row -> Set.of("EVIDENCE_REQUIRED", "CLIENT_PAGE_UNREACHED").contains(row.auditStatus()))
			.map(row -> row.actualPath() + "; " + row.unresolvedReason())
			.toList());
		assertEquals(List.of(), rows.stream()
			.filter(row -> Set.of(1993, 1994, 2993, 2994, 80292, 80293, 80296, 80297)
				.contains(row.questId()))
			.filter(row -> Set.of("EVIDENCE_REQUIRED", "CLIENT_PAGE_UNREACHED").contains(row.auditStatus()))
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
		assertEquals("EVIDENCE_REQUIRED", rows.getFirst().auditStatus());
		assertEquals("9999", rows.getFirst().shownPage());
		assertTrue(rows.getFirst().unresolvedReason().contains("absent from the active client page index"));
	}

	@Test
	void recognizesActiveClientPagesWithoutButtonsAsTerminalResponses() {
		QuestCatalog catalog = catalog(90005, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of()))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(100005, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(500)), null, "unaccepted")));
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = Map.of(90005,
			new QuestDialogOrderAudit.ClientQuest("QUEST_Q90005.html", Map.of(500,
				new QuestDialogOrderAudit.ClientPage(500, "terminal", 2, Map.of(), Map.of(),
					"QUEST_Q90005.html#terminal page-order=2 sha256=abc"))));

		List<QuestDialogOrderAudit.AuditRow> rows = QuestDialogOrderAudit.audit(catalog, clientQuests);

		assertEquals(1, rows.size());
		assertEquals("TERMINAL_PAGE_REACHED", rows.getFirst().auditStatus());
		assertEquals("NOT_NEEDED", rows.getFirst().fixStatus());
		assertEquals("", rows.getFirst().unresolvedReason());
	}

	@Test
	void clientPageReaderAllowsRepeatedTerminalPages() throws Exception {
		Path directory = Files.createTempDirectory("quest-dialog-pages");
		Path pages = directory.resolve("pages.csv");
		Path details = directory.resolve("details.csv");
		Files.writeString(pages, """
			quest_id,source_file,source_variant,source_sha256,page_order,html_page_name,page_id,page_mapping,action_count
			90020,q20.html,active,hash20,1,terminal,500,exact,0
			90020,q20.html,active,hash20,2,terminal,500,exact,0
			""");
		Files.writeString(details, """
			quest_id,source_file,source_variant,source_sha256,html_page_name,page_id,page_mapping,button_text_zh,action_mapping,action_id,action_constant
			""");

		Map<Integer, QuestDialogOrderAudit.ClientQuest> quests = QuestDialogOrderAudit.readClientPages(pages, details);

		assertEquals(Set.of(90020), quests.keySet());
		assertEquals(1, quests.get(90020).pages().size());
	}

	@Test
	void clientPageReaderRejectsMultipleActiveSources() throws Exception {
		assertAmbiguousClientPages("""
			90021,q21-a.html,active,hash21a,1,first,501,exact,0
			90021,q21-b.html,active,hash21b,1,second,502,exact,0
			""", "90021");
	}

	@Test
	void clientPageReaderRejectsRepeatedInteractivePages() throws Exception {
		assertAmbiguousClientPages("""
			90022,q22.html,active,hash22,1,interactive,503,exact,1
			90022,q22.html,active,hash22,2,interactive,503,exact,1
			""", "90022");
	}

	@Test
	void keepsUnmappedClientActionsVisibleAsEvidenceGaps() {
		QuestCatalog catalog = catalog(90006, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of()))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(100006, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(500)), null, "unaccepted")));
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = Map.of(90006,
			new QuestDialogOrderAudit.ClientQuest("QUEST_Q90006.html", Map.of(
				500, new QuestDialogOrderAudit.ClientPage(500, "shown", 1, Map.of(),
					Map.of("HACTION_UNKNOWN", "QUEST_Q90006.html#shown"), "shown page"),
				501, new QuestDialogOrderAudit.ClientPage(501, "unreached", 2, Map.of(),
					Map.of("HACTION_OTHER", "QUEST_Q90006.html#unreached"), "unreached page"))));

		List<QuestDialogOrderAudit.AuditRow> rows = QuestDialogOrderAudit.audit(catalog, clientQuests);

		assertEquals(List.of("CLIENT_PAGE_UNREACHED", "EVIDENCE_REQUIRED"), rows.stream()
			.map(QuestDialogOrderAudit.AuditRow::auditStatus).sorted().toList());
		assertEquals(List.of("HACTION_OTHER", "HACTION_UNKNOWN"), rows.stream()
			.map(QuestDialogOrderAudit.AuditRow::clientVisibleAction).sorted().toList());
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
				new QuestDialogOrderAudit.ClientPage(500, "500", 1, Map.of(42,
					new QuestDialogOrderAudit.ClientAction(42, "QUEST_Q90002.html#500")), Map.of(),
					"QUEST_Q90002.html#500"))));

		List<QuestDialogOrderAudit.AuditRow> rows = QuestDialogOrderAudit.audit(catalog, clientQuests);

		assertEquals(1, rows.size());
		assertEquals("PAGE_ACTION_MATCHED", rows.getFirst().auditStatus());
	}

	@Test
	void emitsTheCompleteSingleCandidateContractInExecutionOrder() {
		QuestCatalog catalog = catalog(90007, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of())),
			new QuestNode("reward", new NodeProjection(QuestStatus.REWARD, Map.of("step", 1)))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(100007, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(500)), null, "unaccepted"),
			new QuestTransition(new QuestEvent.TalkToNpc(100007, 42),
				List.of(new QuestCondition.HasItem(182400001, 2)),
				List.of(new QuestAction.SetVariable("step", 1), new QuestAction.SetStatus(QuestStatus.REWARD),
					new QuestAction.GrantReward("EXP", 0, 120), new QuestAction.CompleteQuest(0)),
				"reward", List.of(new AfterCommitAction.SyncQuestState(QuestStateSyncMode.COMPLETION),
					new AfterCommitAction.ShowQuestDialog(700), new AfterCommitAction.CloseDialog()), 5, "started")));
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = clientQuest(90007, 42,
			Map.of(700, new QuestDialogOrderAudit.ClientPage(700, "terminal", 2, Map.of(), Map.of(),
				"QUEST_Q90007.html#terminal")));

		QuestDialogOrderAudit.AuditRow row = QuestDialogOrderAudit.audit(catalog, clientQuests).stream()
			.filter(candidate -> candidate.clientVisibleAction().equals("42"))
			.findFirst().orElseThrow();

		assertEquals(1, row.candidateCount());
		QuestDialogOrderAudit.CandidateContract contract = row.candidate();
		assertEquals(1, contract.index());
		assertEquals("started", contract.sourceNode());
		assertEquals("reward", contract.targetNode());
		assertEquals("REWARD", contract.targetStatus());
		assertEquals("{\"step\"=1}", contract.targetVariables());
		assertEquals("1:HasItem(itemId=182400001, count=2, expected=true)", contract.conditions());
		assertEquals("5", contract.priority());
		assertEquals("1:SetVariable(field=\"step\", value=1)"
			+ " -> 2:SetStatus(status=REWARD)"
			+ " -> 3:GrantReward(kind=\"EXP\", id=0, amount=120, amountMode=EXACT)"
			+ " -> 4:CompleteQuest(rewardIndex=0)", contract.transactionActions());
		assertEquals("2:SHOW_QUEST_PAGE(page=700) -> 3:CLOSE_DIALOG", contract.response());
		assertEquals("1:SyncQuestState(mode=COMPLETION)"
			+ " -> 2:ShowQuestDialog(dialogId=700)"
			+ " -> 3:CloseDialog()", contract.afterCommitSequence());
	}

	@Test
	void emitsAllConditionalCandidatesInDeterministicOrder() {
		QuestTransition success = new QuestTransition(new QuestEvent.TalkToNpc(100008, 42),
			List.of(new QuestCondition.HasItem(182400001, 1)),
			List.of(new QuestAction.SetVariable("result", 1)), "success",
			List.of(new AfterCommitAction.CloseDialog()), 20, "started");
		QuestTransition failure = new QuestTransition(new QuestEvent.TalkToNpc(100008, 42),
			List.of(new QuestCondition.HasItem(182400001, 1, false)), List.of(), "failure",
			List.of(new AfterCommitAction.ShowQuestDialog(701)), 10, "started");
		List<QuestNode> nodes = List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of())),
			new QuestNode("success", new NodeProjection(QuestStatus.START, Map.of("result", 1))),
			new QuestNode("failure", new NodeProjection(QuestStatus.START, Map.of())));
		QuestTransition entry = new QuestTransition(new QuestEvent.TalkToNpc(100008, 31), List.of(),
			List.of(), "started", List.of(new AfterCommitAction.ShowQuestDialog(500)), null, "unaccepted");
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = clientQuest(90008, 42, Map.of());

		List<QuestDialogOrderAudit.AuditRow> forward = QuestDialogOrderAudit.audit(
			catalog(90008, nodes, List.of(entry, success, failure)), clientQuests).stream()
			.filter(row -> row.clientVisibleAction().equals("42")).toList();
		List<QuestDialogOrderAudit.AuditRow> reversed = QuestDialogOrderAudit.audit(
			catalog(90008, nodes, List.of(entry, failure, success)), clientQuests).stream()
			.filter(row -> row.clientVisibleAction().equals("42")).toList();

		assertEquals(forward, reversed);
		assertEquals(List.of(2, 2), forward.stream().map(QuestDialogOrderAudit.AuditRow::candidateCount).toList());
		assertEquals(List.of(1, 2), forward.stream().map(row -> row.candidate().index()).toList());
		assertEquals(List.of("failure", "success"), forward.stream()
			.map(row -> row.candidate().targetNode()).toList());
		assertEquals(List.of(
			"1:HasItem(itemId=182400001, count=1, expected=false)",
			"1:HasItem(itemId=182400001, count=1, expected=true)"), forward.stream()
			.map(row -> row.candidate().conditions()).toList());
		assertEquals(List.of("1:SHOW_QUEST_PAGE(page=701)", "1:CLOSE_DIALOG"), forward.stream()
			.map(row -> row.candidate().response()).toList());
	}

	@Test
	void followsEachConditionalResponsePageWithoutJoiningSiblingBranches() {
		QuestTransition firstBranch = new QuestTransition(new QuestEvent.TalkToNpc(100011, 42),
			List.of(new QuestCondition.HasItem(182400001, 1)), List.of(), "first",
			List.of(new AfterCommitAction.ShowQuestDialog(501)), 20, "started");
		QuestTransition secondBranch = new QuestTransition(new QuestEvent.TalkToNpc(100011, 42),
			List.of(new QuestCondition.HasItem(182400001, 1, false)), List.of(), "second",
			List.of(new AfterCommitAction.ShowQuestDialog(502)), 10, "started");
		QuestCatalog catalog = catalog(90011, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of())),
			new QuestNode("first", new NodeProjection(QuestStatus.START, Map.of("branch", 1))),
			new QuestNode("second", new NodeProjection(QuestStatus.START, Map.of("branch", 2))),
			new QuestNode("firstDone", new NodeProjection(QuestStatus.START, Map.of("branch", 3))),
			new QuestNode("secondDone", new NodeProjection(QuestStatus.START, Map.of("branch", 4)))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(100011, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(500)), null, "unaccepted"),
			firstBranch,
			secondBranch,
			new QuestTransition(new QuestEvent.TalkToNpc(100011, 43), List.of(), List.of(), "firstDone",
				List.of(new AfterCommitAction.CloseDialog()), null, "first"),
			new QuestTransition(new QuestEvent.TalkToNpc(100011, 44), List.of(), List.of(), "secondDone",
				List.of(new AfterCommitAction.CloseDialog()), null, "second")));
		Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuests = Map.of(90011,
			new QuestDialogOrderAudit.ClientQuest("QUEST_Q90011.html", Map.of(
				500, new QuestDialogOrderAudit.ClientPage(500, "root", 3, Map.of(42,
					new QuestDialogOrderAudit.ClientAction(42, "QUEST_Q90011.html#root")), Map.of(), "root"),
				501, new QuestDialogOrderAudit.ClientPage(501, "first", 1, Map.of(43,
					new QuestDialogOrderAudit.ClientAction(43, "QUEST_Q90011.html#first")), Map.of(), "first"),
				502, new QuestDialogOrderAudit.ClientPage(502, "second", 2, Map.of(44,
					new QuestDialogOrderAudit.ClientAction(44, "QUEST_Q90011.html#second")), Map.of(), "second"))));

		List<QuestDialogOrderAudit.AuditRow> rows = QuestDialogOrderAudit.audit(catalog, clientQuests);

		assertEquals(List.of("43", "44"), rows.stream()
			.filter(row -> row.shownPage().equals("501") || row.shownPage().equals("502"))
			.map(QuestDialogOrderAudit.AuditRow::clientVisibleAction).sorted().toList());
		assertEquals(List.of("firstDone", "secondDone"), rows.stream()
			.filter(row -> row.shownPage().equals("501") || row.shownPage().equals("502"))
			.map(row -> row.candidate().targetNode()).sorted().toList());
	}

	@Test
	void distinguishesCloseAndSelectionWindowResponses() {
		QuestCatalog catalog = catalog(90009, List.of(
			new QuestNode("unaccepted", new NodeProjection(QuestStatus.NONE, Map.of())),
			new QuestNode("started", new NodeProjection(QuestStatus.START, Map.of())),
			new QuestNode("next", new NodeProjection(QuestStatus.START, Map.of()))), List.of(
			new QuestTransition(new QuestEvent.TalkToNpc(100009, 31), List.of(), List.of(), "started",
				List.of(new AfterCommitAction.ShowQuestDialog(500)), null, "unaccepted"),
			new QuestTransition(new QuestEvent.TalkToNpc(100009, 42), List.of(), List.of(), "next",
				List.of(new AfterCommitAction.CloseDialog(),
					new AfterCommitAction.SyncQuestState(QuestStateSyncMode.PACKET_ONLY),
					new AfterCommitAction.ShowQuestSelectionDialog(10)), null, "started")));

		QuestDialogOrderAudit.CandidateContract contract = QuestDialogOrderAudit.audit(catalog,
			clientQuest(90009, 42, Map.of())).stream()
			.filter(row -> row.clientVisibleAction().equals("42"))
			.findFirst().orElseThrow().candidate();

		assertEquals("1:CLOSE_DIALOG -> 3:SHOW_SELECTION_PAGE(page=10)", contract.response());
		assertEquals("1:CloseDialog() -> 2:SyncQuestState(mode=PACKET_ONLY)"
			+ " -> 3:ShowQuestSelectionDialog(dialogId=10)", contract.afterCommitSequence());
	}

	@Test
	void writesUtf8BomForSpreadsheetCompatibility() throws Exception {
		Path output = Files.createTempFile("quest-order-audit", ".csv");
		try {
			QuestDialogOrderAudit.CandidateContract contract = new QuestDialogOrderAudit.CandidateContract(
				1, "started", "reward", "REWARD", "{}", "", "", "", "1:CLOSE_DIALOG",
				"1:CloseDialog()");
			QuestDialogOrderAudit.write(output, List.of(new QuestDialogOrderAudit.AuditRow(90010,
				"QUEST_Q90010.html", "started", "100010", "42", "path", "500", "42", "route",
				1, contract, "evidence", "PAGE_ACTION_MATCHED", "NOT_NEEDED", "")));
			String csv = Files.readString(output);
			assertEquals("\ufeffquest_id", Files.readString(output).substring(0, 9));
			assertTrue(csv.contains("candidate_count,candidate_index,candidate_source_node"));
			assertTrue(csv.contains(",1,1,started,reward,REWARD,{},,,,1:CLOSE_DIALOG,1:CloseDialog(),"));
		} finally {
			Files.deleteIfExists(output);
		}
	}

	private static Map<Integer, QuestDialogOrderAudit.ClientQuest> clientQuest(int questId, int actionId,
			Map<Integer, QuestDialogOrderAudit.ClientPage> additionalPages) {
		Map<Integer, QuestDialogOrderAudit.ClientPage> pages = new java.util.LinkedHashMap<>();
		pages.put(500, new QuestDialogOrderAudit.ClientPage(500, "start", 1, Map.of(actionId,
			new QuestDialogOrderAudit.ClientAction(actionId, "QUEST_Q" + questId + ".html#start")),
			Map.of(), "QUEST_Q" + questId + ".html#start"));
		pages.putAll(additionalPages);
		return Map.of(questId, new QuestDialogOrderAudit.ClientQuest("QUEST_Q" + questId + ".html",
			Map.copyOf(pages)));
	}

	private static void assertAmbiguousClientPages(String pageRows, String questId) throws Exception {
		Path directory = Files.createTempDirectory("quest-dialog-pages");
		Path pages = directory.resolve("pages.csv");
		Path details = directory.resolve("details.csv");
		Files.writeString(pages, """
			quest_id,source_file,source_variant,source_sha256,page_order,html_page_name,page_id,page_mapping,action_count
			""" + pageRows);
		Files.writeString(details, """
			quest_id,source_file,source_variant,source_sha256,html_page_name,page_id,page_mapping,button_text_zh,action_mapping,action_id,action_constant
			""");

		IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
			() -> QuestDialogOrderAudit.readClientPages(pages, details));
		assertTrue(failure.getMessage().contains(questId));
	}

	private static QuestCatalog catalog(int id, List<QuestNode> nodes, List<QuestTransition> transitions) {
		QuestDefinition definition = new QuestDefinition(id, 1, QuestMetadata.minimal("audit", id, "QUEST"),
			ProgressLayout.empty(), nodes, transitions);
		return new ImmutableQuestCatalog(List.of(new CompiledQuestDefinition(definition)));
	}
}
