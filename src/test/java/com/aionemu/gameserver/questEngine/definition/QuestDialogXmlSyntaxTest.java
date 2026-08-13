package com.aionemu.gameserver.questEngine.definition;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestDialogXmlSyntaxTest {
	@Test
	void compilesAllSixDialogContextsToCanonicalIntegerIr() {
		QuestDefinition definition = parse("""
			<transition source="unaccepted" target="unaccepted"><event><dialog type="TALK_TO_NPC" npc-id="203758" action="QUEST_REFUSE_1"/></event></transition>
			<transition source="unaccepted" target="unaccepted"><event><dialog type="QUEST_ACTION" action="FINISH_DIALOG"/></event></transition>
			<transition source="unaccepted" target="unaccepted"><event><dialog type="TALK_TO_NPC" npc-id="203758" action="QUEST_SELECT"/></event><after-commit><dialog type="SHOW_QUEST_PAGE" page="QUEST_ACCEPT_1"/></after-commit></transition>
			<transition source="unaccepted" target="unaccepted"><event><dialog type="TALK_TO_NPC" npc-id="203758" action="FINISH_DIALOG"/></event><after-commit><dialog type="SHOW_SELECTION_PAGE" page="SELECT_QUEST"/></after-commit></transition>
			<dialog type="NPC_START" npc-id="203758" source="unaccepted" target="started" start-page="SELECT1"/>
			<dialog type="NPC_REPORT" npc-id="203097" source="started" target="reward" page="SELECT5"/>
		""");

		assertEquals(new QuestEvent.TalkToNpc(203758, 1003), definition.transitions().get(0).event());
		assertEquals(new QuestEvent.QuestDialog(1008), definition.transitions().get(1).event());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1003)),
			definition.transitions().get(2).afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestSelectionDialog(10)),
			definition.transitions().get(3).afterCommit());
		QuestTransition startAccept = definition.transitions().stream()
			.filter(transition -> transition.sourceNode().equals("unaccepted"))
			.filter(transition -> transition.targetNode().equals("started"))
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(203758, 1002)))
			.findFirst().orElseThrow();
		assertEquals(List.of(
			new AfterCommitAction.SyncQuestState(QuestStateSyncMode.VISIBILITY_REFRESH),
			new AfterCommitAction.ShowQuestDialog(1003)), startAccept.afterCommit());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(2375)), definition.transitions().stream()
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(203097, 31)))
			.findFirst().orElseThrow().afterCommit());
	}

	@Test
	void npcStartExpandsTheFirstClientStepForSelectNone() {
		QuestDefinition definition = parse("""
			<dialog type="NPC_START" npc-id="203758" source="unaccepted" target="started" start-page="SELECT_NONE"/>
		""");

		QuestTransition next = definition.transitions().stream()
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(203758,
				QuestDialogAction.SELECT_NONE_1.id())))
			.findFirst().orElseThrow();
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT_NONE_1.id())),
			next.afterCommit());
	}

	@Test
	void actionAndPageEnumsKeepOverlappingIdsInSeparateSpaces() {
		assertEquals(1003, QuestDialogAction.QUEST_REFUSE_1.id());
		assertEquals(1003, QuestDialogPage.QUEST_ACCEPT_1.id());
		assertEquals(QuestDialogAction.QUEST_REFUSE_1, QuestDialogAction.fromId(1003));
		assertEquals(QuestDialogPage.QUEST_ACCEPT_1, QuestDialogPage.fromId(1003));
	}

	@Test
	void actionListsAndSymbolRangesExpandInClientIdOrder() {
		QuestDefinition definition = parse("""
			<transition source="unaccepted" target="unaccepted"><event><dialog type="TALK_TO_NPC" npc-id="203758" actions="USE_OBJECT SELECTED_QUEST_REWARD1..SELECTED_QUEST_REWARD3 FINISH_DIALOG"/></event></transition>
		""");

		assertEquals(List.of(-1, 8, 9, 10, 1008), definition.transitions().stream()
			.map(transition -> assertInstanceOf(QuestEvent.TalkToNpc.class, transition.event()).dialogId()).toList());
	}

	@Test
	void rejectsDialogTypesOutsideTheirContextWithStableCodes() {
		assertCode("DIALOG_TYPE_NOT_ALLOWED_IN_EVENT",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"SHOW_QUEST_PAGE\" page=\"SELECT1\"/></event></transition>");
		assertCode("DIALOG_TYPE_NOT_ALLOWED_IN_AFTER_COMMIT",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"QUEST_ACTION\" action=\"FINISH_DIALOG\"/></event><after-commit><dialog type=\"QUEST_ACTION\" action=\"FINISH_DIALOG\"/></after-commit></transition>");
		assertCode("DIALOG_TYPE_NOT_ALLOWED_IN_TRANSITIONS",
			"<dialog type=\"SHOW_QUEST_PAGE\" page=\"SELECT1\"/>");
	}

	@Test
	void rejectsUnknownMissingAndConflictingDialogAttributesWithStableCodes() {
		assertCode("UNKNOWN_DIALOG_TYPE",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"UNKNOWN\" action=\"FINISH_DIALOG\"/></event></transition>");
		assertCode("DIALOG_ACTION_REQUIRED",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"QUEST_ACTION\"/></event></transition>");
		assertCode("DIALOG_ACTION_ATTRIBUTE_CONFLICT",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"QUEST_ACTION\" action=\"FINISH_DIALOG\" actions=\"FINISH_DIALOG\"/></event></transition>");
		assertCode("UNKNOWN_DIALOG_ACTION",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"QUEST_ACTION\" action=\"START_DIALOG\"/></event></transition>");
		assertCode("DIALOG_PAGE_REQUIRED",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"QUEST_ACTION\" action=\"FINISH_DIALOG\"/></event><after-commit><dialog type=\"SHOW_QUEST_PAGE\"/></after-commit></transition>");
		assertCode("UNKNOWN_DIALOG_PAGE",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"QUEST_ACTION\" action=\"FINISH_DIALOG\"/></event><after-commit><dialog type=\"SHOW_QUEST_PAGE\" page=\"ACCEPT_QUEST\"/></after-commit></transition>");
	}

	@Test
	void rejectsDialogAttributesAndChildrenOutsideTheirContext() {
		assertCode("DIALOG_ATTRIBUTE_NOT_ALLOWED",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"QUEST_ACTION\" action=\"FINISH_DIALOG\" page=\"SELECT1\"/></event></transition>");
		assertCode("DIALOG_CHILD_NOT_ALLOWED",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"QUEST_ACTION\" action=\"FINISH_DIALOG\"><accept-actions/></dialog></event></transition>");
		assertCode("DIALOG_ACTION_REQUIRED",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"QUEST_ACTION\" action=\"\"/></event></transition>");
		assertCode("DIALOG_ACTION_ATTRIBUTE_CONFLICT",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"QUEST_ACTION\" action=\"\" actions=\"\"/></event></transition>");
	}

	@Test
	void requiresPagesForTransitionDialogShortcuts() {
		assertCode("DIALOG_START_PAGE_REQUIRED",
			"<dialog type=\"NPC_START\" npc-id=\"203758\" source=\"unaccepted\" target=\"started\"/>");
		assertCode("DIALOG_PAGE_REQUIRED",
			"<dialog type=\"NPC_REPORT\" npc-id=\"203758\" source=\"started\" target=\"reward\"/>");
	}

	@Test
	void explicitDialogRouteOverridesOnlyItsMatchingNpcStartRoute() {
		QuestDefinition definition = parse("""
			<dialog type="NPC_START" npc-id="203758" source="unaccepted" target="started" start-page="SELECT1"/>
			<transition source="unaccepted" target="unaccepted"><event><dialog type="TALK_TO_NPC" npc-id="203758" action="QUEST_SELECT"/></event><after-commit><dialog type="SHOW_QUEST_PAGE" page="SELECT2"/></after-commit></transition>
			""");

		List<QuestTransition> questSelect = definition.transitions().stream()
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(203758,
				QuestDialogAction.QUEST_SELECT.id()))).toList();
		assertEquals(1, questSelect.size());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(QuestDialogPage.SELECT2.id())),
			questSelect.getFirst().afterCommit());
		assertEquals(1, definition.transitions().stream()
			.filter(transition -> transition.event().equals(new QuestEvent.TalkToNpc(203758,
				QuestDialogAction.QUEST_ACCEPT_1.id()))).count());
	}

	@Test
	void xsdValidationFailuresUseTheStableInvalidXmlCode() {
		assertCode("INVALID_XML",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"QUEST_ACTION\" action=\"FINISH_DIALOG\" dialog-id=\"1008\"/></event></transition>");
		assertCode("INVALID_XML",
			"<transition source=\"unaccepted\" target=\"unaccepted\"><event><dialog type=\"QUEST_ACTION\" action=\"FINISH_DIALOG\"><unexpected/></dialog></event></transition>");
	}

	@Test
	void legacyDialogTagsRemainReadable() {
		QuestDefinition definition = parse("""
			<transition source="unaccepted" target="unaccepted"><event><talk-to-npc npc-id="203758" dialog-id="1003"/></event><after-commit><show-quest-dialog dialog-id="1003"/></after-commit></transition>
		""");
		assertEquals(new QuestEvent.TalkToNpc(203758, 1003), definition.transitions().getFirst().event());
		assertEquals(List.of(new AfterCommitAction.ShowQuestDialog(1003)),
			definition.transitions().getFirst().afterCommit());
	}

	private static void assertCode(String expected, String transitions) {
		assertEquals(expected, assertThrows(QuestCompilationException.class,
			() -> parse(transitions)).code());
	}

	private static QuestDefinition parse(String transitions) {
		String xml = """
			<quest-definition id="1913" version="1">
			  <metadata name="dialog test" display-name-id="1102913" min-level="10" max-level="99" category="QUEST"/>
			  <nodes>
			    <node label="unaccepted" status="NONE"/>
			    <node label="started" status="START"/>
			    <node label="reward" status="REWARD"/>
			  </nodes>
			  <transitions>%s</transitions>
			</quest-definition>
			""".formatted(transitions);
		return QuestDefinitionXmlCompiler.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}
}
