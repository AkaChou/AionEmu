package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.runtime.QuestMutationPlanner;
import com.aionemu.gameserver.questEngine.runtime.QuestSnapshot;
import com.aionemu.gameserver.questEngine.runtime.QuestStartEligibility;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Quest10520StartEligibilityTest {
	@Test
	void covertCommuniquesStartsInSanctumOnlyWhenLevelRequirementsAreMet() throws Exception {
		CompiledQuestDefinition definition = load();
		QuestEvent.EnterWorld enterWorld = new QuestEvent.EnterWorld();
		QuestTransition start = definition.definition().transitions().stream()
			.filter(transition -> transition.sourceNode().equals("unaccepted")
				&& transition.event().equals(enterWorld))
			.findFirst().orElseThrow();

		assertEquals(65, definition.definition().metadata().minLevel());
		assertTrue(start.conditions().contains(new QuestCondition.WorldIs(110010000, true)));
		assertTrue(start.conditions().contains(new QuestCondition.StartEligible()));

		QuestSnapshot snapshot = new QuestSnapshot(7, 10520, QuestStatus.NONE, 0,
			Map.of(), Map.of(), true, true, 0, 0, 110010000, 0, 0, 0, 0, (byte) 0);
		assertFalse(QuestMutationPlanner.plan(definition,
			snapshot.withStartEligibility(QuestStartEligibility.rejected("MIN_LEVEL_NOT_MET")),
			enterWorld, start).isPresent());
		assertTrue(QuestMutationPlanner.plan(definition,
			snapshot.withStartEligibility(QuestStartEligibility.allowed()), enterWorld, start).isPresent());
	}

	private static CompiledQuestDefinition load() throws Exception {
		try (InputStream input = Quest10520StartEligibilityTest.class.getResourceAsStream(
			"/aion/data/static_data/quest_definition/quests/10520.xml")) {
			return QuestDefinitionXmlCompiler.compile(Objects.requireNonNull(input));
		}
	}
}
