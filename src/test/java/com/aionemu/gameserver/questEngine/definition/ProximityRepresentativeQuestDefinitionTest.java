package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.atDistance;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bitField;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setStatus;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real proximity handler facts projected through both canonical definition front ends. */
class ProximityRepresentativeQuestDefinitionTest {
	private static final int NPC = 806700;

	@Test
	void quest29600AtDistanceStartsQuestAndAdvancesStep() {
		CompiledQuestDefinition definition = dslDefinition();
		QuestEvent.AtDistance event = new QuestEvent.AtDistance(NPC,
			new QuestProximityFacts(7, 20, NPC, 110010000, 110010000, 0, 0, 4d, 20d));
		var plan = QuestMutationPlanner.plan(definition,
			new QuestSnapshot(7, 29600, QuestStatus.NONE, 0, Map.of())
				.withStartEligibility(QuestStartEligibility.allowed()), event,
			definition.definition().transitions().get(0));

		assertTrue(plan.isPresent());
		assertEquals(QuestStatus.START, plan.orElseThrow().nextStatus());
		assertEquals(1, definition.definition().progressLayout().unpack(plan.orElseThrow().nextPackedVariables()).get("var0"));
	}

	@Test
	void xmlAndDslCompileToTheSameImmutableDefinition() {
		assertEquals(dslDefinition().definition(), QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(xml()
			.getBytes(StandardCharsets.UTF_8))).definition());
	}

	private static CompiledQuestDefinition dslDefinition() {
		return quest(29600)
			.metadata(QuestMetadata.minimal("proximity-29600", 1, "QUEST"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("unaccepted", project(QuestStatus.NONE, vars("var0", 0)))
			.node("started", project(QuestStatus.START, vars("var0", 1)))
			.on(atDistance(NPC)).from("unaccepted").when(statusIs(QuestStatus.NONE))
			.then(setVariable("var0", 1)).then(setStatus(QuestStatus.START)).goTo("started")
			.compile();
	}

	private static String xml() {
		return """
			<quest-definition id="29600" version="1">
			  <metadata name="proximity-29600" display-name-id="1" min-level="0" max-level="2147483647" category="QUEST"/>
			  <progress><bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
			  <nodes><node label="unaccepted"><project status="NONE"><vars><var name="var0" value="0"/></vars></project></node><node label="started"><project status="START"><vars><var name="var0" value="1"/></vars></project></node></nodes>
			  <transitions><transition source="unaccepted" target="started"><event><at-distance npc-id="806700"/></event><conditions><status-is status="NONE"/></conditions><actions><set-variable field="var0" value="1"/><set-status status="START"/></actions></transition></transitions>
			</quest-definition>
			""";
	}
}
