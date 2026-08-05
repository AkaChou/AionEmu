package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A kill of any listed npc satisfies the KillNpcSet event. */
class KillNpcSetDefinitionTest {
	@Test
	void dslKillNpcIdsMatchesAnyListedNpc() {
		var dsl = quest(990041)
			.metadata(QuestMetadata.minimal("killset-demo", 1, "QUEST"))
			.progress(bitField("var0", 0, 3, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.node("done", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(killNpcIds(1001, 1002, 1003)).from("start").goTo("done").compile();

		var plan = QuestMutationPlanner.plan(dsl,
			new QuestSnapshot(7, 990041, QuestStatus.START, 0, Map.of()),
			new QuestEvent.KillNpc(1002),
			dsl.definition().transitions().getFirst());
		assertTrue(plan.isPresent());
		assertEquals(QuestStatus.REWARD, plan.orElseThrow().nextStatus());
	}

	@Test
	void killNpcIdsDoesNotMatchUnlistedNpc() {
		var dsl = quest(990042)
			.metadata(QuestMetadata.minimal("killset-negative", 1, "QUEST"))
			.progress(bitField("var0", 0, 3, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.node("done", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(killNpcIds(1001, 1002)).from("start").goTo("done").compile();

		var plan = QuestMutationPlanner.plan(dsl,
			new QuestSnapshot(7, 990042, QuestStatus.START, 0, Map.of()),
			new QuestEvent.KillNpc(9999),
			dsl.definition().transitions().getFirst());
		assertTrue(plan.isEmpty());
	}

	@Test
	void xmlNpcIdsParsesIntoKillNpcSet() {
		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(new java.io.ByteArrayInputStream(
			("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<quest-definition id=\"990043\" version=\"1\">\n"
				+ "  <metadata name=\"xml-killset\" display-name-id=\"990043\" min-level=\"1\" max-level=\"99\" category=\"QUEST\"/>\n"
				+ "  <progress><bit-field name=\"var0\" offset=\"0\" width=\"3\" min=\"0\" max=\"3\" persistence=\"PERSISTENT\" scope=\"LOCAL\"/></progress>\n"
				+ "  <nodes>\n"
				+ "    <node label=\"unaccepted\"><project status=\"NONE\"><vars><var name=\"var0\" value=\"0\"/></vars></project></node>\n"
				+ "    <node label=\"started\"><project status=\"START\"><vars><var name=\"var0\" value=\"0\"/></vars></project></node>\n"
				+ "    <node label=\"done\"><project status=\"REWARD\"><vars><var name=\"var0\" value=\"1\"/></vars></project></node>\n"
				+ "  </nodes>\n"
				+ "  <transitions>\n"
				+ "    <transition source=\"unaccepted\" target=\"started\"><event><enter-world/></event></transition>\n"
				+ "    <transition source=\"started\" target=\"done\"><event><kill-npc npc-ids=\"2001 2002 2003\"/></event></transition>\n"
				+ "  </transitions>\n"
				+ "</quest-definition>\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));

		QuestTransition killTransition = compiled.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.KillNpcSet).findFirst().orElseThrow();
		QuestEvent.KillNpcSet set = assertInstanceOf(QuestEvent.KillNpcSet.class, killTransition.event());
		assertEquals(Set.of(2001, 2002, 2003), set.npcIds());

		var plan = QuestMutationPlanner.plan(compiled,
			new QuestSnapshot(7, 990043, QuestStatus.START, 0, Map.of()),
			new QuestEvent.KillNpc(2002),
			killTransition);
		assertTrue(plan.isPresent());
	}

	@Test
	void singleNpcIdStillParsesToKillNpc() {
		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(new java.io.ByteArrayInputStream(
			("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<quest-definition id=\"990044\" version=\"1\">\n"
				+ "  <metadata name=\"xml-single\" display-name-id=\"990044\" min-level=\"1\" max-level=\"99\" category=\"QUEST\"/>\n"
				+ "  <progress><bit-field name=\"var0\" offset=\"0\" width=\"3\" min=\"0\" max=\"3\" persistence=\"PERSISTENT\" scope=\"LOCAL\"/></progress>\n"
				+ "  <nodes>\n"
				+ "    <node label=\"unaccepted\"><project status=\"NONE\"><vars><var name=\"var0\" value=\"0\"/></vars></project></node>\n"
				+ "    <node label=\"started\"><project status=\"START\"><vars><var name=\"var0\" value=\"0\"/></vars></project></node>\n"
				+ "    <node label=\"done\"><project status=\"REWARD\"><vars><var name=\"var0\" value=\"1\"/></vars></project></node>\n"
				+ "  </nodes>\n"
				+ "  <transitions>\n"
				+ "    <transition source=\"unaccepted\" target=\"started\"><event><enter-world/></event></transition>\n"
				+ "    <transition source=\"started\" target=\"done\"><event><kill-npc npc-id=\"210133\"/></event></transition>\n"
				+ "  </transitions>\n"
				+ "</quest-definition>\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)));

		QuestTransition killTransition = compiled.definition().transitions().stream()
			.filter(t -> t.event() instanceof QuestEvent.KillNpc).findFirst().orElseThrow();
		assertInstanceOf(QuestEvent.KillNpc.class, killTransition.event());
	}

	@Test
	void xmlRejectsBothSingleAndSetNpcIds() {
		var failure = assertThrows(QuestCompilationException.class, () -> QuestDefinitionXmlCompiler.compile(
			new java.io.ByteArrayInputStream(
				("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
					+ "<quest-definition id=\"990045\" version=\"1\">\n"
					+ "  <metadata name=\"xml-ambiguous-kill\" display-name-id=\"990045\" min-level=\"1\" max-level=\"99\" category=\"QUEST\"/>\n"
					+ "  <progress><bit-field name=\"var0\" offset=\"0\" width=\"3\" min=\"0\" max=\"3\" persistence=\"PERSISTENT\" scope=\"LOCAL\"/></progress>\n"
					+ "  <nodes>\n"
					+ "    <node label=\"start\"><project status=\"START\"><vars><var name=\"var0\" value=\"0\"/></vars></project></node>\n"
					+ "    <node label=\"done\"><project status=\"REWARD\"><vars><var name=\"var0\" value=\"1\"/></vars></project></node>\n"
					+ "  </nodes>\n"
					+ "  <transitions><transition source=\"start\" target=\"done\"><event><kill-npc npc-id=\"210133\" npc-ids=\"210133 210134\"/></event></transition></transitions>\n"
					+ "</quest-definition>\n").getBytes(java.nio.charset.StandardCharsets.UTF_8))));

		assertEquals("AMBIGUOUS_KILL_NPC_EVENT", failure.code());
	}
}
