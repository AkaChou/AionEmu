package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestEventIndex;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlan;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestXmlDomainBlocksTest {
	@Test
	void npcStartEqualsTheStandardExpandedProtocol() {
		String block = """
			<npc-start npc-id="203110" source="unaccepted" target="started"
			    selection-sources="unaccepted started">
			  <accept-actions><give-item item-id="182400001" count="1"/></accept-actions>
			</npc-start>
			""";
		String expanded = """
			<transition source="unaccepted" target="unaccepted"><event><talk-to-npc npc-id="203110" dialog-id="31"/></event><after-commit><show-quest-dialog dialog-id="1011"/></after-commit></transition>
			<transition source="unaccepted" target="unaccepted"><event><talk-to-npc npc-id="203110" dialog-id="1007"/></event><after-commit><show-quest-dialog dialog-id="4"/></after-commit></transition>
			<transition source="unaccepted" target="started"><event><talk-to-npc npc-id="203110" dialog-id="1002"/></event><conditions><start-eligible/></conditions><actions><give-item item-id="182400001" count="1"/></actions><after-commit><sync-quest-state mode="VISIBILITY_REFRESH"/><show-quest-dialog dialog-id="1003"/></after-commit></transition>
			<transition source="unaccepted" target="started"><event><talk-to-npc npc-id="203110" dialog-id="20000"/></event><conditions><start-eligible/></conditions><actions><give-item item-id="182400001" count="1"/></actions><after-commit><sync-quest-state mode="VISIBILITY_REFRESH"/><close-dialog/></after-commit></transition>
			<transition source="unaccepted" target="unaccepted"><event><talk-to-npc npc-id="203110" dialog-ids="1003 1004 20001"/></event><after-commit><close-dialog/></after-commit></transition>
			<transition source="unaccepted" target="unaccepted"><event><talk-to-npc npc-id="203110" dialog-id="1008"/></event><after-commit><show-quest-selection-dialog dialog-id="10"/></after-commit></transition>
			<transition source="started" target="started"><event><talk-to-npc npc-id="203110" dialog-id="1008"/></event><after-commit><show-quest-selection-dialog dialog-id="10"/></after-commit></transition>
			""";

		assertEquals(compile(startDefinition(block)).definition(), compile(startDefinition(expanded)).definition());
	}

	@Test
	void counterEqualsItsTwoExpandedTransitions() {
		String block = """
			<counter source="started" target="reward" field="var0" required="3">
			  <event><kill-npc npc-ids="210001 210002"/></event>
			  <conditions><world-is world-id="210010000"/></conditions>
			</counter>
			""";
		String expanded = """
			<transition source="started" target="started" priority="1">
			  <event><kill-npc npc-ids="210001 210002"/></event>
			  <conditions><world-is world-id="210010000"/><variable-below field="var0" value="2"/></conditions>
			  <actions><increment-variable field="var0" delta="1"/></actions>
			  <after-commit><sync-quest-state mode="PACKET_ONLY"/></after-commit>
			</transition>
			<transition source="started" target="reward" priority="0">
			  <event><kill-npc npc-ids="210001 210002"/></event>
			  <conditions><world-is world-id="210010000"/><variable-is field="var0" value="2"/></conditions>
			  <actions><increment-variable field="var0" delta="1"/></actions>
			  <after-commit><sync-quest-state mode="PACKET_ONLY"/></after-commit>
			</transition>
			""";

		assertEquals(compile(counterDefinition(block)).definition(),
			compile(counterDefinition(expanded)).definition());
	}

	@Test
	void counterCompletesOnTheNthEventAndIgnoresLaterEvents() {
		CompiledQuestDefinition definition = compile(counterDefinition("""
			<counter source="started" target="reward" field="var0" required="3">
			  <event><kill-npc npc-id="210001"/></event>
			</counter>
			"""));
		QuestEvent event = new QuestEvent.KillNpc(210001);
		QuestSnapshot initial = snapshot(definition, QuestStatus.START, 0);

		QuestMutationPlan first = route(definition, initial, event).orElseThrow();
		assertEquals(QuestStatus.START, first.nextStatus());
		assertEquals(1, first.nextPackedVariables());

		QuestMutationPlan penultimate = route(definition,
			snapshot(definition, first.nextStatus(), first.nextPackedVariables()), event).orElseThrow();
		assertEquals(QuestStatus.START, penultimate.nextStatus());
		assertEquals(2, penultimate.nextPackedVariables());

		QuestMutationPlan nth = route(definition,
			snapshot(definition, penultimate.nextStatus(), penultimate.nextPackedVariables()), event).orElseThrow();
		assertEquals(QuestStatus.REWARD, nth.nextStatus());
		assertEquals(3, nth.nextPackedVariables());
		assertTrue(route(definition, snapshot(definition, QuestStatus.START, 3), event).isEmpty());
		assertTrue(route(definition, snapshot(definition, QuestStatus.REWARD, 3), event).isEmpty());
	}

	@Test
	void killChainEqualsItsExpandedTransitions() {
		String block = """
			<kill-chain nodes="v1 v2 v3 v4">
			  <event><kill-npc npc-ids="210001 210002"/></event>
			  <conditions><world-is world-id="210010000"/></conditions>
			</kill-chain>
			""";
		String expanded = """
			<transition source="v1" target="v2">
			  <event><kill-npc npc-ids="210001 210002"/></event>
			  <conditions><world-is world-id="210010000"/></conditions>
			  <after-commit><sync-quest-state mode="PACKET_ONLY"/></after-commit>
			</transition>
			<transition source="v2" target="v3">
			  <event><kill-npc npc-ids="210001 210002"/></event>
			  <conditions><world-is world-id="210010000"/></conditions>
			  <after-commit><sync-quest-state mode="PACKET_ONLY"/></after-commit>
			</transition>
			<transition source="v3" target="v4">
			  <event><kill-npc npc-ids="210001 210002"/></event>
			  <conditions><world-is world-id="210010000"/></conditions>
			  <after-commit><sync-quest-state mode="PACKET_ONLY"/></after-commit>
			</transition>
			""";

		assertEquals(compile(killChainDefinition(block)).definition(),
			compile(killChainDefinition(expanded)).definition());
	}

	@Test
	void killChainRejectsInvalidNodesAndEvents() {
		String valid = killChainDefinition("""
			<kill-chain nodes="v1 v2 v3">
			  <event><kill-npc npc-id="210001"/></event>
			</kill-chain>
			""");
		assertCode("KILL_CHAIN_TOO_SHORT", valid.replace("v1 v2 v3", "v1 v2"));
		assertCode("KILL_CHAIN_DUPLICATE_NODE", valid.replace("v1 v2 v3", "v1 v2 v2"));
		assertCode("XML_BLOCK_BAD_NODE_REFERENCE", valid.replace("v1 v2 v3", "v1 missing v3"));
		assertCode("KILL_CHAIN_EVENT_TYPE", valid.replace(
			"<kill-npc npc-id=\"210001\"/>", "<talk-to-npc npc-id=\"210001\" dialog-id=\"31\"/>"));
		assertCode("KILL_CHAIN_EVENT_INVALID", valid.replace(
			"npc-id=\"210001\"", "npc-id=\"210001\" npc-ids=\"210002\""));
	}

	@Test
	void npcCompleteEqualsFixedChoiceAndFallbackTransitions() {
		String block = """
			<npc-complete npc-id="203123" source="reward" target="complete"
			    fixed-reward-indices="0 1" complete-reward-index="0"
			    preview-dialog-ids="-1 1009" finish="SELECTION_DIALOG">
			  <choice dialog-id="8" reward-index="2"/>
			  <choice dialog-id="9" reward-index="3"/>
			  <fallback dialog-ids="23"/>
			</npc-complete>
			""";
		String fixed = """
			<grant-reward kind="EXP" id="0" amount="100" amount-mode="QUEST_BASE"/>
			<grant-reward kind="ITEM" id="188000001" amount="2"/>
			""";
		String after = """
			<after-commit><refresh-player-stats/><sync-quest-state mode="COMPLETION"/><show-quest-selection-dialog dialog-id="10"/></after-commit>
			""";
		String expanded = """
			<transition source="reward" target="reward"><event><talk-to-npc npc-id="203123" dialog-ids="-1 1009"/></event><after-commit><show-quest-dialog dialog-id="5"/></after-commit></transition>
			<transition source="reward" target="complete"><event><talk-to-npc npc-id="203123" dialog-id="8"/></event><actions>
			""" + fixed + """
			  <grant-reward kind="ITEM" id="100000001" amount="1"/><complete-quest reward-index="0"/></actions>
			""" + after + """
			</transition>
			<transition source="reward" target="complete"><event><talk-to-npc npc-id="203123" dialog-id="9"/></event><actions>
			""" + fixed + """
			  <grant-reward kind="ITEM" id="100000002" amount="1"/><complete-quest reward-index="0"/></actions>
			""" + after + """
			</transition>
			<transition source="reward" target="complete"><event><talk-to-npc npc-id="203123" dialog-id="23"/></event><actions>
			""" + fixed + """
			  <complete-quest reward-index="0"/></actions>
			""" + after + """
			</transition>
			""";

		assertEquals(compile(completionDefinition(block)).definition(),
			compile(completionDefinition(expanded)).definition());
	}

	@Test
	void npcCompleteSupportsAllFinishModesInFinalActionOrder() {
		Map<String, Class<? extends AfterCommitAction>> endingTypes = Map.of(
			"SELECTION_DIALOG", AfterCommitAction.ShowQuestSelectionDialog.class,
			"CLOSE_DIALOG", AfterCommitAction.CloseDialog.class);
		for (Map.Entry<String, Class<? extends AfterCommitAction>> entry : endingTypes.entrySet()) {
			QuestTransition completion = completionTransition(entry.getKey());
			assertEquals(List.of(AfterCommitAction.RefreshPlayerStats.class,
				AfterCommitAction.SyncQuestState.class, entry.getValue()),
				completion.afterCommit().stream().map(Object::getClass).toList());
		}
		QuestTransition none = completionTransition("NONE");
		assertEquals(List.of(AfterCommitAction.RefreshPlayerStats.class, AfterCommitAction.SyncQuestState.class),
			none.afterCommit().stream().map(Object::getClass).toList());
		assertEquals(List.of(new QuestAction.GrantReward("EXP", 0, 100, QuestRewardAmountMode.QUEST_BASE),
			new QuestAction.CompleteQuest(0)), none.actions());
	}

	@Test
	void blocksAndOrdinaryTransitionsKeepDocumentOrder() {
		String xml = """
			<quest-definition id="990063" version="1">
			  <metadata name="mixed" display-name-id="1" min-level="0" max-level="99" category="QUEST">
			    <rewards><reward kind="EXP" id="0" amount="100"/></rewards>
			  </metadata>
			  <nodes>
			    <node label="unaccepted"><project status="NONE"/></node>
			    <node label="started"><project status="START"/></node>
			    <node label="reward"><project status="REWARD"/></node>
			    <node label="complete"><project status="COMPLETE"/></node>
			  </nodes>
			  <transitions>
			    <npc-start npc-id="203110" source="unaccepted" target="started" selection-sources="unaccepted started"/>
			    <transition source="started" target="reward"><event><talk-to-npc npc-id="203120" dialog-id="1009"/></event></transition>
			    <npc-complete npc-id="203123" source="reward" target="complete" fixed-reward-indices="0"
			        dialog-ids="8..23" complete-reward-index="0" preview-dialog-ids="-1 1009" finish="NONE"/>
			  </transitions>
			</quest-definition>
			""";
		List<QuestTransition> transitions = compile(xml).definition().transitions();
		assertEquals(9, transitions.stream().takeWhile(t -> talkNpcId(t) == 203110).count());
		assertEquals(203120, talkNpcId(transitions.get(9)));
		assertEquals(203123, talkNpcId(transitions.get(10)));
	}

	@Test
	void npcStartAndCounterRejectInvalidContext() {
		assertCode("NPC_START_SOURCE_STATUS", startDefinition("""
			<npc-start npc-id="203110" source="unaccepted" target="started" selection-sources="unaccepted"/>
			""").replace("label=\"unaccepted\"><project status=\"NONE\"",
			"label=\"unaccepted\"><project status=\"START\""));
		assertCode("NPC_START_ACCEPT_ACTION_INVALID", startDefinition("""
			<npc-start npc-id="203110" source="unaccepted" target="started" selection-sources="unaccepted">
			  <accept-actions><give-item item-id="-1" count="1"/></accept-actions>
			</npc-start>
			"""));

		String valid = counterDefinition("""
			<counter source="started" target="reward" field="var0" required="3">
			  <event><kill-npc npc-id="210001"/></event>
			</counter>
			""");
		assertCode("COUNTER_UNKNOWN_FIELD", valid.replace("field=\"var0\"", "field=\"missing\""));
		assertCode("COUNTER_INVALID_REQUIRED", valid.replace("required=\"3\"", "required=\"0\""));
		assertCode("COUNTER_FIELD_TOO_NARROW", valid.replace("required=\"3\"", "required=\"8\""));
		assertCode("COUNTER_SOURCE_PROJECTION_CONFLICT", valid.replace(
			"label=\"started\"><project status=\"START\"/>",
			"label=\"started\"><project status=\"START\"><vars><var name=\"var0\" value=\"0\"/></vars></project>"));
		assertCode("COUNTER_TARGET_PROJECTION_CONFLICT", valid.replace("value=\"3\"", "value=\"2\""));
	}

	@Test
	void npcCompleteRejectsBadRewardsDialogsAndNodes() {
		String valid = completionDefinition("""
			<npc-complete npc-id="203123" source="reward" target="complete"
			    fixed-reward-indices="0 1" complete-reward-index="0"
			    preview-dialog-ids="-1 1009" finish="NONE">
			  <choice dialog-id="8" reward-index="2"/>
			</npc-complete>
			""");
		assertCode("NPC_COMPLETE_REWARD_INDEX_OUT_OF_RANGE",
			valid.replace("fixed-reward-indices=\"0 1\"", "fixed-reward-indices=\"0 99\""));
		assertCode("NPC_COMPLETE_FIXED_REWARD_TYPE",
			valid.replace("fixed-reward-indices=\"0 1\"", "fixed-reward-indices=\"0 2\""));
		assertCode("NPC_COMPLETE_CHOICE_REWARD_TYPE",
			valid.replace("reward-index=\"2\"", "reward-index=\"1\""));
		assertCode("NPC_COMPLETE_DUPLICATE_DIALOG_ID",
			valid.replace("preview-dialog-ids=\"-1 1009\"", "preview-dialog-ids=\"-1 8\""));
		assertCode("NPC_COMPLETE_INVALID_COMPLETE_REWARD_INDEX",
			valid.replace("complete-reward-index=\"0\"", "complete-reward-index=\"-1\""));
		assertCode("NPC_COMPLETE_SOURCE_STATUS",
			valid.replace("label=\"reward\"><project status=\"REWARD\"",
				"label=\"reward\"><project status=\"START\""));
		assertCode("NPC_COMPLETE_TARGET_STATUS",
			valid.replace("label=\"complete\"><project status=\"COMPLETE\"",
				"label=\"complete\"><project status=\"REWARD\""));

		QuestCompilationException error = assertThrows(QuestCompilationException.class,
			() -> compile(valid.replace("source=\"reward\"", "source=\"missing\"")));
		assertEquals("XML_BLOCK_BAD_NODE_REFERENCE", error.code());
		assertTrue(error.getMessage().contains("quest 990062 npc-complete attribute 'source'"));
	}

	private static QuestTransition completionTransition(String finish) {
		CompiledQuestDefinition definition = compile(completionDefinition("""
			<npc-complete npc-id="203123" source="reward" target="complete" fixed-reward-indices="0"
			    dialog-ids="8" complete-reward-index="0" preview-dialog-ids="-1 1009" finish="%s"/>
			""".formatted(finish)));
		return definition.definition().transitions().stream()
			.filter(t -> t.targetNode().equals("complete")).findFirst().orElseThrow();
	}

	private static Optional<QuestMutationPlan> route(CompiledQuestDefinition definition, QuestSnapshot snapshot,
			QuestEvent event) {
		QuestEventIndex index = new QuestEventIndex(new ImmutableQuestCatalog(List.of(definition)));
		for (QuestEventIndex.Route route : index.routesFor(event, definition.id())) {
			Optional<QuestMutationPlan> plan = QuestMutationPlanner.plan(definition, snapshot, event, route.transition());
			if (plan.isPresent()) {
				return plan;
			}
		}
		return Optional.empty();
	}

	private static QuestSnapshot snapshot(CompiledQuestDefinition definition, QuestStatus status, int var0) {
		int packed = definition.definition().progressLayout().pack(Map.of("var0", var0));
		return new QuestSnapshot(7, definition.id(), status, packed, Map.of());
	}

	private static int talkNpcId(QuestTransition transition) {
		return assertInstanceOf(QuestEvent.TalkToNpc.class, transition.event()).npcId();
	}

	private static void assertCode(String code, String xml) {
		assertEquals(code, assertThrows(QuestCompilationException.class, () -> compile(xml)).code());
	}

	private static CompiledQuestDefinition compile(String xml) {
		return QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	private static String startDefinition(String transitions) {
		return """
			<quest-definition id="990060" version="1">
			  <metadata name="start-block" display-name-id="1" min-level="0" max-level="99" category="QUEST"/>
			  <progress><bit-field name="var0" offset="0" width="3" min="0" max="7" persistence="PERSISTENT" scope="LOCAL"/></progress>
			  <nodes>
			    <node label="unaccepted"><project status="NONE"><vars><var name="var0" value="0"/></vars></project></node>
			    <node label="started"><project status="START"><vars><var name="var0" value="0"/></vars></project></node>
			  </nodes>
			  <transitions>%s</transitions>
			</quest-definition>
			""".formatted(transitions);
	}

	private static String counterDefinition(String transitions) {
		return """
			<quest-definition id="990061" version="1">
			  <metadata name="counter-block" display-name-id="1" min-level="0" max-level="99" category="QUEST"/>
			  <progress><bit-field name="var0" offset="0" width="3" min="0" max="7" persistence="PERSISTENT" scope="LOCAL"/></progress>
			  <nodes>
			    <node label="started"><project status="START"/></node>
			    <node label="reward"><project status="REWARD"><vars><var name="var0" value="3"/></vars></project></node>
			  </nodes>
			  <transitions>%s</transitions>
			</quest-definition>
			""".formatted(transitions);
	}

	private static String killChainDefinition(String transitions) {
		return """
			<quest-definition id="990064" version="1">
			  <metadata name="kill-chain-block" display-name-id="1" min-level="0" max-level="99" category="QUEST"/>
			  <progress><bit-field name="var0" offset="0" width="3" min="0" max="7" persistence="PERSISTENT" scope="LOCAL"/></progress>
			  <nodes>
			    <node label="v1"><project status="START"><vars><var name="var0" value="1"/></vars></project></node>
			    <node label="v2"><project status="START"><vars><var name="var0" value="2"/></vars></project></node>
			    <node label="v3"><project status="START"><vars><var name="var0" value="3"/></vars></project></node>
			    <node label="v4"><project status="START"><vars><var name="var0" value="4"/></vars></project></node>
			  </nodes>
			  <transitions>%s</transitions>
			</quest-definition>
			""".formatted(transitions);
	}

	private static String completionDefinition(String transitions) {
		return """
			<quest-definition id="990062" version="1">
			  <metadata name="complete-block" display-name-id="1" min-level="0" max-level="99" category="QUEST">
			    <rewards>
			      <reward kind="EXP" id="0" amount="100"/>
			      <reward kind="ITEM" id="188000001" amount="2"/>
			      <reward kind="SELECTABLE_ITEM" id="100000001" amount="1"/>
			      <reward kind="SELECTABLE_ITEM" id="100000002" amount="1"/>
			    </rewards>
			  </metadata>
			  <progress><bit-field name="var0" offset="0" width="3" min="0" max="7" persistence="PERSISTENT" scope="LOCAL"/></progress>
			  <nodes>
			    <node label="reward"><project status="REWARD"><vars><var name="var0" value="3"/></vars></project></node>
			    <node label="complete"><project status="COMPLETE"><vars><var name="var0" value="0"/></vars></project></node>
			  </nodes>
			  <transitions>%s</transitions>
			</quest-definition>
			""".formatted(transitions);
	}
}
