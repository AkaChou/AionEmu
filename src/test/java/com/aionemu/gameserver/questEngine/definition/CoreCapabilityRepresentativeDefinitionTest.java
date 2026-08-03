package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.bonusApply;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.canAct;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.enterWorld;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.enterZone;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.leaveZone;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.levelUp;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.project;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.quest;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.setVariable;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.statusIs;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.vars;
import static com.aionemu.gameserver.questEngine.definition.QuestDsl.zoneMissionEnd;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real handler-backed representatives for the shared core event families. */
class CoreCapabilityRepresentativeDefinitionTest {
	@Test
	void levelInteractionRewardAndWorldEventsCompileAndPlanThroughDslAndXml() {
		assertEquivalent(1001, levelUp(), "<level-up/>");
		assertEquivalent(1002, canAct(730010, "ACTION_ITEM_USE"),
			"<can-act template-id=\"730010\" action-type=\"ACTION_ITEM_USE\"/>");
		assertEquivalent(80016, bonusApply("MOVIE"), "<bonus-apply bonus-type=\"MOVIE\"/>");
		assertEquivalent(1001, zoneMissionEnd(), "<zone-mission-end/>");
		assertEquivalent(1002, enterWorld(), "<enter-world/>");
		assertEquivalent(1000, enterZone("AKARIOS_PLAINS_210010000"),
			"<enter-zone zone=\"AKARIOS_PLAINS_210010000\"/>");
		assertEquivalent(24046, leaveZone("BALTASAR_HILL_VILLAGE_220050000"),
			"<leave-zone zone=\"BALTASAR_HILL_VILLAGE_220050000\"/>");
	}

	private static void assertEquivalent(int id, QuestEvent event, String xmlEvent) {
		CompiledQuestDefinition dsl = quest(id)
			.metadata(QuestMetadata.minimal("representative", 1, "QUEST"))
			.progress(QuestDsl.bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.node("done", project(QuestStatus.REWARD, vars("var0", 1)))
			.on(event).from("start").when(statusIs(QuestStatus.START))
			.then(setVariable("var0", 1)).goTo("done").compile();
		String xml = """
			<quest-definition id="%d" version="1">
			  <metadata name="representative" display-name-id="1" min-level="0" max-level="2147483647" category="QUEST"/>
			  <progress><bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
			  <nodes><node label="start"><project status="START"><vars><var name="var0" value="0"/></vars></project></node>
			    <node label="done"><project status="REWARD"><vars><var name="var0" value="1"/></vars></project></node></nodes>
			  <transitions><transition source="start" target="done"><event>%s</event><conditions><status-is status="START"/></conditions><actions><set-variable field="var0" value="1"/></actions></transition></transitions>
			</quest-definition>
			""".formatted(id, xmlEvent);
		CompiledQuestDefinition fromXml = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		assertEquals(dsl.definition(), fromXml.definition());
		var plan = QuestMutationPlanner.plan(dsl,
			new QuestSnapshot(7, id, QuestStatus.START, 0, Map.of()), event,
			dsl.definition().transitions().getFirst());
		assertTrue(plan.isPresent());
		assertEquals(QuestStatus.REWARD, plan.orElseThrow().nextStatus());
		assertEquals(1, plan.orElseThrow().nextPackedVariables());
	}

}
