package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BroadcastZoneMissionEndDefinitionTest {
	@Test
	void dslBroadcastZoneMissionEndCarriesQuestIds() {
		var dsl = quest(990051)
			.metadata(QuestMetadata.minimal("broadcast-demo", 1, "QUEST"))
			.progress(bitField("var0", 0, 3, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.node("done", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(talkToNpc(203057, com.aionemu.gameserver.questEngine.model.QuestDialog.START_DIALOG)).from("start").goTo("done")
			.afterCommit(broadcastZoneMissionEnd(10521, 10522, 10523)).compile();

		AfterCommitAction action = dsl.definition().transitions().getFirst().afterCommit().stream()
			.filter(AfterCommitAction.BroadcastZoneMissionEnd.class::isInstance).findFirst().orElseThrow();
		assertArrayEquals(new int[]{10521, 10522, 10523},
			((AfterCommitAction.BroadcastZoneMissionEnd) action).questIds());
	}

	@Test
	void xmlBroadcastZoneMissionEndParsesQuestIds() {
		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(new java.io.ByteArrayInputStream(
("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
+ "<quest-definition id=\"990052\" version=\"1\">\n"
+ "  <metadata name=\"xml-broadcast\" display-name-id=\"990052\" min-level=\"1\" max-level=\"99\" category=\"QUEST\"/>\n"
+ "  <progress><bit-field name=\"var0\" offset=\"0\" width=\"3\" min=\"0\" max=\"3\" persistence=\"PERSISTENT\" scope=\"LOCAL\"/></progress>\n"
+ "  <nodes>\n"
+ "    <node label=\"unaccepted\" status=\"NONE\"><var name=\"var0\" value=\"0\"/></node>\n"
+ "    <node label=\"started\" status=\"START\"><var name=\"var0\" value=\"0\"/></node>\n"
+ "    <node label=\"done\" status=\"REWARD\"><var name=\"var0\" value=\"1\"/></node>\n"
+ "  </nodes>\n"
+ "  <transitions>\n"
+ "    <transition source=\"unaccepted\" target=\"started\"><event><enter-world/></event></transition>\n"
+ "    <transition source=\"started\" target=\"done\"><event><kill-npc npc-id=\"210133\"/></event>\n"
+ "      <after-commit><broadcast-zone-mission-end quest-ids=\"10521 10522 10523\"/></after-commit></transition>\n"
+ "  </transitions>\n"
+ "</quest-definition>\n"
+ "").getBytes(java.nio.charset.StandardCharsets.UTF_8)));

		AfterCommitAction action = compiled.definition().transitions().get(1).afterCommit().stream()
			.filter(AfterCommitAction.BroadcastZoneMissionEnd.class::isInstance).findFirst().orElseThrow();
		assertArrayEquals(new int[]{10521, 10522, 10523},
			((AfterCommitAction.BroadcastZoneMissionEnd) action).questIds());
	}

	@Test
	void plannerRetainsBroadcastActionInPlan() {
		var dsl = quest(990053)
			.metadata(QuestMetadata.minimal("broadcast-plan", 1, "QUEST"))
			.progress(bitField("var0", 0, 3, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.node("done", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(killNpc(210133)).from("start").goTo("done")
			.afterCommit(broadcastZoneMissionEnd(10521)).compile();

		var plan = QuestMutationPlanner.plan(dsl,
			new QuestSnapshot(7, 990053, QuestStatus.START, 0, Map.of()),
			new QuestEvent.KillNpc(210133),
			dsl.definition().transitions().getFirst());
		assertTrue(plan.isPresent());
		assertTrue(plan.orElseThrow().afterCommit().stream()
			.anyMatch(AfterCommitAction.BroadcastZoneMissionEnd.class::isInstance));
	}

	@Test
	void npcCompleteAppendsCustomActionsBetweenCompletionSyncAndFinishDialog() {
		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(new java.io.ByteArrayInputStream(
("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
+ "<quest-definition id=\"990054\" version=\"1\">\n"
+ "  <metadata name=\"npc-complete-broadcast\" display-name-id=\"990054\" min-level=\"1\" max-level=\"99\" category=\"QUEST\"/>\n"
+ "  <progress><bit-field name=\"var0\" offset=\"0\" width=\"1\" min=\"0\" max=\"1\" persistence=\"PERSISTENT\" scope=\"LOCAL\"/></progress>\n"
+ "  <nodes>\n"
+ "    <node label=\"reward\" status=\"REWARD\"><var name=\"var0\" value=\"1\"/></node>\n"
+ "    <node label=\"complete\" status=\"COMPLETE\"><var name=\"var0\" value=\"0\"/></node>\n"
+ "  </nodes>\n"
+ "  <transitions><npc-complete npc-id=\"203057\" source=\"reward\" target=\"complete\" dialog-ids=\"8\" preview-dialog-ids=\"-1 1009\" complete-reward-index=\"0\" finish=\"SELECTION_DIALOG\">\n"
+ "    <after-commit><broadcast-zone-mission-end quest-ids=\"10521 10522\"/></after-commit>\n"
+ "  </npc-complete></transitions>\n"
+ "</quest-definition>\n"
+ "").getBytes(java.nio.charset.StandardCharsets.UTF_8)));

		QuestTransition completion = compiled.definition().transitions().stream()
			.filter(transition -> "complete".equals(transition.targetNode()))
			.findFirst().orElseThrow();
		assertEquals(4, completion.afterCommit().size());
		assertInstanceOf(AfterCommitAction.RefreshPlayerStats.class, completion.afterCommit().get(0));
		AfterCommitAction.SyncQuestState sync = assertInstanceOf(AfterCommitAction.SyncQuestState.class,
			completion.afterCommit().get(1));
		assertEquals(QuestStateSyncMode.COMPLETION, sync.mode());
		assertInstanceOf(AfterCommitAction.BroadcastZoneMissionEnd.class, completion.afterCommit().get(2));
		assertInstanceOf(AfterCommitAction.ShowQuestSelectionDialog.class, completion.afterCommit().get(3));
	}

	@Test
	void npcCompleteRejectsInvalidCustomAfterCommitAction() {
		QuestCompilationException failure = assertThrows(QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(new java.io.ByteArrayInputStream(
("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
+ "<quest-definition id=\"990055\" version=\"1\">\n"
+ "  <metadata name=\"invalid-npc-complete-action\" display-name-id=\"990055\" min-level=\"1\" max-level=\"99\" category=\"QUEST\"/>\n"
+ "  <progress><bit-field name=\"var0\" offset=\"0\" width=\"1\" min=\"0\" max=\"1\" persistence=\"PERSISTENT\" scope=\"LOCAL\"/></progress>\n"
+ "  <nodes>\n"
+ "    <node label=\"reward\" status=\"REWARD\"><var name=\"var0\" value=\"1\"/></node>\n"
+ "    <node label=\"complete\" status=\"COMPLETE\"><var name=\"var0\" value=\"0\"/></node>\n"
+ "  </nodes>\n"
+ "  <transitions><npc-complete npc-id=\"203057\" source=\"reward\" target=\"complete\" dialog-ids=\"8\" preview-dialog-ids=\"-1 1009\" complete-reward-index=\"0\" finish=\"NONE\">\n"
+ "    <after-commit><broadcast-zone-mission-end quest-ids=\"not-a-quest-id\"/></after-commit>\n"
+ "  </npc-complete></transitions>\n"
+ "</quest-definition>\n"
+ "").getBytes(java.nio.charset.StandardCharsets.UTF_8))));

		assertEquals("NPC_COMPLETE_AFTER_COMMIT_INVALID", failure.code());
	}
}
