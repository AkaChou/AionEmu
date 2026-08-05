package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.aionemu.gameserver.questEngine.definition.QuestDsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Increment-variable action advances the packed variable by delta. */
class IncrementVariableDefinitionTest {
	@Test
	void incrementVariableAdvancesFieldByDelta() {
		var dsl = quest(990031)
			.metadata(QuestMetadata.minimal("increment-demo", 1, "QUEST"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 2)))
			.node("done", project(QuestStatus.REWARD, vars("var0", 4)))
			.on(new QuestEvent.TalkToNpc(203057, 31)).from("start")
			.then(incrementVariable("var0", 2)).goTo("done").compile();

		var plan = QuestMutationPlanner.plan(dsl,
			new QuestSnapshot(7, 990031, QuestStatus.START, 2, Map.of()),
			new QuestEvent.TalkToNpc(203057, 31, 0),
			dsl.definition().transitions().getFirst());

		assertTrue(plan.isPresent());
		assertEquals(4, plan.orElseThrow().nextPackedVariables());
	}

	/** variable-at-least condition gates completion until the counter reaches threshold. */
	@Test
	void variableAtLeastGatesUntilThresholdReached() {
		// var0=2 (below 3): transition does not fire.
		var below = quest(990032)
			.metadata(QuestMetadata.minimal("at-least-demo", 1, "QUEST"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 2)))
			.node("done", project(QuestStatus.REWARD, vars("var0", 3)))
			.on(new QuestEvent.TalkToNpc(203057, 31)).from("start")
			.when(variableAtLeast("var0", 3)).goTo("done").compile();
		var belowPlan = QuestMutationPlanner.plan(below,
			new QuestSnapshot(7, 990032, QuestStatus.START, 2, Map.of()),
			new QuestEvent.TalkToNpc(203057, 31, 0),
			below.definition().transitions().getFirst());
		assertTrue(belowPlan.isEmpty());

		// var0=3 (at threshold): transition fires.
		var at = quest(990033)
			.metadata(QuestMetadata.minimal("at-least-demo", 1, "QUEST"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 3)))
			.node("done", project(QuestStatus.REWARD, vars("var0", 3)))
			.on(new QuestEvent.TalkToNpc(203057, 31)).from("start")
			.when(variableAtLeast("var0", 3)).goTo("done").compile();
		var atPlan = QuestMutationPlanner.plan(at,
			new QuestSnapshot(7, 990033, QuestStatus.START, 3, Map.of()),
			new QuestEvent.TalkToNpc(203057, 31, 0),
			at.definition().transitions().getFirst());
		assertTrue(atPlan.isPresent());
	}

	@Test
	void variableBelowGatesUntilTheCounterReachesTheCap() {
		var below = quest(990034)
			.metadata(QuestMetadata.minimal("below-demo", 1, "QUEST"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 2)))
			.node("done", project(QuestStatus.REWARD, vars("var0", 3)))
			.on(new QuestEvent.TalkToNpc(203057, 31)).from("start")
			.when(variableBelow("var0", 3)).goTo("done").compile();
		assertTrue(QuestMutationPlanner.plan(below,
			new QuestSnapshot(7, 990034, QuestStatus.START, 2, Map.of()),
			new QuestEvent.TalkToNpc(203057, 31, 0),
			below.definition().transitions().getFirst()).isPresent());

		var atCap = new QuestSnapshot(7, 990034, QuestStatus.START, 3, Map.of());
		assertTrue(QuestMutationPlanner.plan(below, atCap,
			new QuestEvent.TalkToNpc(203057, 31, 0),
			below.definition().transitions().getFirst()).isEmpty());
	}

	@Test
	void variableSumConditionsEvaluateAcrossMultipleProgressFields() {
		var definition = quest(990037)
			.metadata(QuestMetadata.minimal("sum-demo", 1, "QUEST"))
			.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
			.progress(bitField("var2", 6, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, Map.of()))
			.node("done", project(QuestStatus.REWARD, vars("var1", 3, "var2", 2)))
			.on(new QuestEvent.TalkToNpc(203057, 31)).from("start")
			.when(variableSumIs(List.of("var1", "var2"), 5)).goTo("done")
			.compile();
		var event = new QuestEvent.TalkToNpc(203057, 31, 0);
		int exactPacked = definition.definition().progressLayout().pack(Map.of("var1", 3, "var2", 2));
		int belowPacked = definition.definition().progressLayout().pack(Map.of("var1", 2, "var2", 2));
		var transition = definition.definition().transitions().getFirst();

		assertTrue(QuestMutationPlanner.plan(definition,
			new com.aionemu.gameserver.questEngine.runtime.QuestSnapshot(7, 990037, QuestStatus.START,
				exactPacked, Map.of()), event, transition).isPresent());
		assertTrue(QuestMutationPlanner.plan(definition,
			new com.aionemu.gameserver.questEngine.runtime.QuestSnapshot(7, 990037, QuestStatus.START,
				belowPacked, Map.of()), event, transition).isEmpty());

		var belowDefinition = quest(990038)
			.metadata(QuestMetadata.minimal("sum-below-demo", 1, "QUEST"))
			.progress(bitField("var1", 0, 6, PersistenceMode.PERSISTENT))
			.progress(bitField("var2", 6, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, Map.of()))
			.node("done", project(QuestStatus.REWARD, vars("var1", 1, "var2", 1)))
			.on(new QuestEvent.TalkToNpc(203057, 31)).from("start")
			.when(variableSumBelow(List.of("var1", "var2"), 5)).goTo("done")
			.compile();
		assertTrue(QuestMutationPlanner.plan(belowDefinition,
			new com.aionemu.gameserver.questEngine.runtime.QuestSnapshot(7, 990038, QuestStatus.START,
				belowDefinition.definition().progressLayout().pack(Map.of("var1", 2, "var2", 2)), Map.of()),
			event, belowDefinition.definition().transitions().getFirst()).isPresent());
	}

	@Test
	void variableBelowCompilesFromXmlAndUnknownFieldsFailClosed() {
		String xml = """
				<quest-definition id="990035" version="1">
				  <metadata name="below-xml" display-name-id="1" min-level="0" max-level="2147483647" category="QUEST"/>
				  <progress><bit-field name="var0" offset="0" width="6" min="0" max="63" persistence="PERSISTENT" scope="LOCAL"/></progress>
				  <nodes><node label="start"><project status="START"><vars><var name="var0" value="0"/></vars></project></node><node label="done"><project status="REWARD"><vars><var name="var0" value="1"/></vars></project></node></nodes>
				  <transitions><transition source="start" target="done"><event><talk-to-npc npc-id="203057" dialog-id="31"/></event><conditions><variable-below field="var0" value="1"/></conditions></transition></transitions>
				</quest-definition>
				""";
		CompiledQuestDefinition compiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
		assertTrue(compiled.definition().transitions().getFirst().conditions()
			.contains(new QuestCondition.VariableBelow("var0", 1)));

		String invalid = xml.replace("field=\"var0\"", "field=\"missing\"");
		assertEquals("UNKNOWN_PROGRESS_FIELD", org.junit.jupiter.api.Assertions.assertThrows(
			QuestCompilationException.class,
			() -> QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(
					invalid.getBytes(StandardCharsets.UTF_8)))).code());

		String sumXml = xml.replace("variable-below field=\"var0\" value=\"1\"",
			"variable-sum-is fields=\"var0\" value=\"0\"");
		CompiledQuestDefinition sumCompiled = QuestDefinitionXmlCompiler.compile(
			new ByteArrayInputStream(sumXml.getBytes(StandardCharsets.UTF_8)));
		assertTrue(sumCompiled.definition().transitions().getFirst().conditions()
			.contains(new QuestCondition.VariableSumIs(List.of("var0"), 0)));
	}

	@Test
	void complementaryVariableRangesDisambiguateSameEventTransitions() {
		var definition = quest(990036)
			.metadata(QuestMetadata.minimal("range-demo", 1, "QUEST"))
			.progress(bitField("var0", 0, 6, PersistenceMode.PERSISTENT))
			.node("start", project(QuestStatus.START, vars("var0", 0)))
			.node("below", project(QuestStatus.REWARD, vars("var0", 1)))
			.node("atLeast", project(QuestStatus.REWARD, vars("var0", 2)))
			.on(new QuestEvent.TalkToNpc(203057, 31)).from("start")
			.when(variableBelow("var0", 3)).goTo("below")
			.on(new QuestEvent.TalkToNpc(203057, 31)).from("start")
			.when(variableAtLeast("var0", 3)).goTo("atLeast")
			.compile();
		assertEquals(2, definition.definition().transitions().size());
	}
}
