package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** broadcast-zone-mission-end after-commit action dispatches to the listed quests. */
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
				+ "    <node label=\"unaccepted\"><project status=\"NONE\"><vars><var name=\"var0\" value=\"0\"/></vars></project></node>\n"
				+ "    <node label=\"started\"><project status=\"START\"><vars><var name=\"var0\" value=\"0\"/></vars></project></node>\n"
				+ "    <node label=\"done\"><project status=\"REWARD\"><vars><var name=\"var0\" value=\"1\"/></vars></project></node>\n"
				+ "  </nodes>\n"
				+ "  <transitions>\n"
				+ "    <transition source=\"unaccepted\" target=\"started\"><event><enter-world/></event></transition>\n"
				+ "    <transition source=\"started\" target=\"done\"><event><kill-npc npc-id=\"210133\"/></event>\n"
				+ "      <after-commit><broadcast-zone-mission-end quest-ids=\"10521 10522 10523\"/></after-commit></transition>\n"
				+ "  </transitions>\n"
				+ "</quest-definition>\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));

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
}