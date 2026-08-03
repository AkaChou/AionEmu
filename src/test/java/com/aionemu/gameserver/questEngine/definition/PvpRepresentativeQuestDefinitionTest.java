package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.killInWorld;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.killRanked;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.pvpRecipientInZone;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.pvpVictimLevelDelta;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real PvP handler facts projected through both canonical definition front ends. */
class PvpRepresentativeQuestDefinitionTest {
	@Test
	void quest3741UsesMinimumRankAndRejectsLowerRank() {
		CompiledQuestDefinition definition = quest(3741)
			.node("start", project(QuestStatus.START, Map.of()))
			.node("reward", project(QuestStatus.REWARD, Map.of()))
			.on(killRanked(3)).from("start").when(statusIs(QuestStatus.START)).goTo("reward")
			.compile();
		QuestPvpKillFacts facts = facts(8, 9, 20, 4, 3, 4, 210010000, Set.of());
		QuestSnapshot snapshot = snapshot(3741, QuestStatus.START, facts);

		assertTrue(QuestMutationPlanner.plan(definition, snapshot,
			new QuestEvent.KillRanked(4, facts), definition.definition().transitions().get(0)).isPresent());
		QuestPvpKillFacts lowerFacts = facts(8, 9, 20, 4, 2, 2, 210010000, Set.of());
		assertFalse(QuestMutationPlanner.plan(definition, snapshot.withPvpFacts(lowerFacts),
			new QuestEvent.KillRanked(2, lowerFacts), definition.definition().transitions().get(0)).isPresent());
	}

	@Test
	void quest15204WorldCountCompilesIdenticallyThroughDslAndXml() {
		CompiledQuestDefinition dsl = quest(15204)
			.metadata(QuestMetadata.minimal("pvp-15204", 1, "QUEST"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 4)))
			.node("reward", project(QuestStatus.REWARD, vars("var0", 5)))
			.on(killInWorld(210070000)).from("start").when(statusIs(QuestStatus.START))
			.when(com.aionemu.gameserver.questEngine.definition.QuestDsl.variableIs("var0", 4)).goTo("reward")
			.compile();
		CompiledQuestDefinition xml = QuestDefinitionXmlCompiler.compile(xml(15204,
			"<progress><bit-field name=\"var0\" offset=\"0\" width=\"6\" min=\"0\" max=\"63\" persistence=\"PERSISTENT\" scope=\"LOCAL\"/></progress>",
			"<node label=\"start\"><project status=\"START\"><vars><var name=\"var0\" value=\"4\"/></vars></project></node>"
				+ "<node label=\"reward\"><project status=\"REWARD\"><vars><var name=\"var0\" value=\"5\"/></vars></project></node>",
			"<event><kill-in-world world-id=\"210070000\"/></event><conditions><status-is status=\"START\"/><variable-is field=\"var0\" value=\"4\"/></conditions>"));
		assertEquals(dsl.definition(), xml.definition());
	}

	@Test
	void quest11362LevelDeltaIsInclusiveAndFailClosedWithoutFacts() {
		CompiledQuestDefinition definition = quest(11362)
			.node("start", project(QuestStatus.START, Map.of()))
			.node("reward", project(QuestStatus.REWARD, Map.of()))
			.on(killInWorld(210130000)).from("start")
			.when(pvpVictimLevelDelta(-5, 9)).goTo("reward").compile();
		QuestPvpKillFacts facts = facts(8, 9, 20, 50, 55, 3, 210130000, Set.of());
		assertTrue(QuestMutationPlanner.plan(definition, snapshot(11362, QuestStatus.START, facts),
			new QuestEvent.KillInWorld(210130000, facts), definition.definition().transitions().get(0)).isPresent());
		assertFalse(QuestMutationPlanner.plan(definition, new QuestSnapshot(8, 11362, QuestStatus.START, 0, Map.of()),
			new QuestEvent.KillInWorld(210130000), definition.definition().transitions().get(0)).isPresent());
	}

	@Test
	void quest23851RequiresZoneAndLevelDelta() {
		CompiledQuestDefinition definition = quest(23851)
			.node("start", project(QuestStatus.START, Map.of()))
			.node("reward", project(QuestStatus.REWARD, Map.of()))
			.on(killInWorld(400010000)).from("start")
			.when(pvpVictimLevelDelta(-5, 9)).when(pvpRecipientInZone("sulfur_fortress_400010000"))
			.goTo("reward").compile();
		QuestPvpKillFacts eligible = facts(8, 9, 20, 50, 55, 3, 400010000,
			Set.of("SULFUR_FORTRESS_400010000"));
		assertTrue(QuestMutationPlanner.plan(definition, snapshot(23851, QuestStatus.START, eligible),
			new QuestEvent.KillInWorld(400010000, eligible), definition.definition().transitions().get(0)).isPresent());
		QuestPvpKillFacts wrongZone = facts(8, 9, 20, 50, 55, 3, 400010000, Set.of("OTHER_ZONE"));
		assertFalse(QuestMutationPlanner.plan(definition, snapshot(23851, QuestStatus.START, wrongZone),
			new QuestEvent.KillInWorld(400010000, wrongZone), definition.definition().transitions().get(0)).isPresent());
	}

	@Test
	void xmlCompilerReadsPvpFactsConditionsAsTypedIr() {
		CompiledQuestDefinition definition = QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(("""
			<quest-definition id="23851" version="1">
			  <metadata name="pvp-23851" display-name-id="1" min-level="0" max-level="2147483647" category="QUEST"/>
			  <nodes><node label="start"><project status="START"/></node><node label="reward"><project status="REWARD"/></node></nodes>
			  <transitions><transition source="start" target="reward">
			    <event><kill-in-world world-id="400010000"/></event>
			    <conditions><pvp-victim-level-delta minimum="-5" maximum="9"/><pvp-recipient-in-zone zone="SULFUR_FORTRESS_400010000"/></conditions>
			  </transition></transitions>
			</quest-definition>
			""").getBytes(StandardCharsets.UTF_8)));
		assertEquals(List.of(new QuestCondition.PvpVictimLevelDelta(-5, 9),
			new QuestCondition.PvpRecipientInZone("SULFUR_FORTRESS_400010000")),
			definition.definition().transitions().get(0).conditions());
	}

	@Test
	void compilerRejectsPvpFactsOnNonPvpEvents() {
		var builder = quest(23851)
			.node("start", project(QuestStatus.START, Map.of()));
		builder.on(com.aionemu.gameserver.questEngine.definition.QuestDsl.talkToNpc(1))
			.from("start").when(pvpVictimLevelDelta(-5, 9)).goTo("start");
		assertEquals("PVP_CONDITION_EVENT_MISMATCH",
			org.junit.jupiter.api.Assertions.assertThrows(QuestCompilationException.class, builder::compile).code());
	}

	private static QuestSnapshot snapshot(int questId, QuestStatus status, QuestPvpKillFacts facts) {
		return new QuestSnapshot(facts.recipientId(), questId, status, 0, Map.of()).withPvpFacts(facts);
	}

	private static QuestPvpKillFacts facts(int killerId, int recipientId, int victimId,
		int recipientLevel, int victimLevel, int victimRankId, int worldId, Set<String> zones) {
		return new QuestPvpKillFacts(killerId, recipientId, victimId, recipientLevel, victimLevel,
			victimRankId, worldId, QuestPvpCreditSource.SOLO, zones);
	}

	private static ByteArrayInputStream xml(int id, String progress, String startNode, String eventAndConditions) {
		String document = "<quest-definition id=\"" + id + "\" version=\"1\">"
			+ "<metadata name=\"pvp-" + id + "\" display-name-id=\"1\" min-level=\"0\" max-level=\"2147483647\" category=\"QUEST\"/>"
			+ progress + "<nodes>" + startNode + "</nodes>"
			+ "<transitions><transition source=\"start\" target=\"reward\">" + eventAndConditions
			+ "</transition></transitions></quest-definition>";
		return new ByteArrayInputStream(document.getBytes(StandardCharsets.UTF_8));
	}
}
