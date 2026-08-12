package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestStartEligibilityContractTest {
	@Test
	void compilerRestoresMissingEligibilityWithoutDuplicatingExplicitCondition() {
		String missing = """

					<quest-definition id="900001" version="1">
					  <metadata name="test" display-name-id="0" min-level="65" max-level="2147483647" category="MISSION"/>
					  <nodes>
					    <node label="unaccepted" status="NONE"/>
					    <node label="started" status="START"/>
					  </nodes>
					  <transitions>
					    <transition source="unaccepted" target="started"><event><enter-world/></event><conditions><world-is world-id="110010000"/></conditions></transition>
					  </transitions>
					</quest-definition>

			""";
		String explicit = missing.replace("<world-is world-id=\"110010000\"/>",
			"<world-is world-id=\"110010000\"/><start-eligible/>");
		String statusBound = missing.replace(" source=\"unaccepted\"", "")
			.replace("<world-is world-id=\"110010000\"/>",
				"<status-is status=\"NONE\"/><world-is world-id=\"110010000\"/>");

		for (String xml : List.of(missing, explicit)) {
			QuestTransition start = compile(xml).definition().transitions().get(0);
			assertEquals(List.of(new QuestCondition.WorldIs(110010000, true),
				new QuestCondition.StartEligible()), start.conditions());
		}
		QuestTransition wildcardStart = compile(statusBound).definition().transitions().get(0);
		assertEquals(List.of(new QuestCondition.StatusIs(QuestStatus.NONE),
			new QuestCondition.WorldIs(110010000, true), new QuestCondition.StartEligible()),
			wildcardStart.conditions());
	}

	@Test
	void archdaevaCapitalStartsHonorTheirLevelSixtyFiveGate() throws Exception {
		assertCapitalStart(10520, 110010000);
		assertCapitalStart(20520, 120010000);
	}

	@Test
	void productionCatalogHasEligibilityOnEveryAcquisitionRoute() {
		QuestCatalog catalog = QuestDefinitionDirectoryLoader.compile(getClass().getClassLoader());
		List<String> violations = new ArrayList<>();
		for (CompiledQuestDefinition definition : catalog.executables()) {
			Map<String, QuestStatus> statuses = new HashMap<>();
			for (QuestNode node : definition.definition().nodes()) {
				statuses.put(node.label(), node.projection().status());
			}
			for (QuestTransition transition : definition.definition().transitions()) {
				boolean startsFromNone = transition.sourceNode() == null
					? transition.conditions().stream().anyMatch(condition ->
						condition instanceof QuestCondition.StatusIs status && status.status() == QuestStatus.NONE)
					: statuses.get(transition.sourceNode()) == QuestStatus.NONE;
				QuestStatus targetStatus = statuses.get(transition.targetNode());
				if (startsFromNone && targetStatus != null && targetStatus != QuestStatus.NONE
						&& transition.conditions().stream()
							.noneMatch(QuestCondition.StartEligible.class::isInstance)) {
					violations.add(definition.id() + ":" + transition.event().type());
				}
			}
		}
		assertTrue(violations.isEmpty(), () -> "acquisition routes without eligibility: " + violations);
	}

	private static void assertCapitalStart(int questId, int worldId) throws Exception {
		CompiledQuestDefinition definition = load(questId);
		QuestEvent.EnterWorld enterWorld = new QuestEvent.EnterWorld();
		QuestTransition start = definition.definition().transitions().stream()
			.filter(transition -> statuses(definition).get(transition.sourceNode()) == QuestStatus.NONE
				&& transition.event().equals(enterWorld))
			.findFirst().orElseThrow();

		assertEquals(65, definition.definition().metadata().minLevel());
		assertTrue(start.conditions().contains(new QuestCondition.WorldIs(worldId, true)));
		assertTrue(start.conditions().contains(new QuestCondition.StartEligible()));

		QuestSnapshot snapshot = new QuestSnapshot(7, questId, QuestStatus.NONE, 0,
			Map.of(), Map.of(), true, true, 0, 0, worldId, 0, 0, 0, 0, (byte) 0);
		assertFalse(QuestMutationPlanner.plan(definition,
			snapshot.withStartEligibility(QuestStartEligibility.rejected("MIN_LEVEL_NOT_MET")),
			enterWorld, start).isPresent());
		assertTrue(QuestMutationPlanner.plan(definition,
			snapshot.withStartEligibility(QuestStartEligibility.allowed()), enterWorld, start).isPresent());
	}

	private static Map<String, QuestStatus> statuses(CompiledQuestDefinition definition) {
		Map<String, QuestStatus> statuses = new HashMap<>();
		for (QuestNode node : definition.definition().nodes()) {
			statuses.put(node.label(), node.projection().status());
		}
		return statuses;
	}

	private static CompiledQuestDefinition compile(String xml) {
		return QuestDefinitionXmlCompiler.compile(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
	}

	private static CompiledQuestDefinition load(int questId) throws Exception {
		try (InputStream input = QuestStartEligibilityContractTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/" + questId + ".xml")) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input));
		}
	}
}
