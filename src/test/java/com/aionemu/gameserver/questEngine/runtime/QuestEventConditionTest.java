package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestEventConditionTest {
	@Test
	void completeCountMatchesExactCapturedCount() {
		QuestSnapshot ninth = new QuestSnapshot(7, 80018, QuestStatus.REWARD, 0, Map.of())
			.withCompleteCount(9);
		QuestSnapshot eighth = ninth.withCompleteCount(8);
		QuestCondition condition = new QuestCondition.CompleteCountIs(9);

		assertTrue(QuestConditionEvaluator.matches(ProgressLayout.empty(), ninth, List.of(condition)));
		assertFalse(QuestConditionEvaluator.matches(ProgressLayout.empty(), eighth, List.of(condition)));
	}

	@Test
	void eventActiveUsesCapturedFactAndFailsClosedWhenUnknown() {
		QuestCondition active = new QuestCondition.EventActive();
		QuestSnapshot activeSnapshot = new QuestSnapshot(7, 80008, QuestStatus.START, 0, Map.of())
			.withEventActive(true);
		QuestSnapshot inactiveSnapshot = activeSnapshot.withEventActive(false);
		QuestSnapshot unknownSnapshot = new QuestSnapshot(7, 80008, QuestStatus.START, 0, Map.of());

		assertTrue(QuestConditionEvaluator.matches(ProgressLayout.empty(), activeSnapshot, List.of(active)));
		assertFalse(QuestConditionEvaluator.matches(ProgressLayout.empty(), inactiveSnapshot, List.of(active)));
		assertFalse(QuestConditionEvaluator.matches(ProgressLayout.empty(), unknownSnapshot, List.of(active)));
	}

	@Test
	void externalEventAndRaceConditionsUseExplicitCapturedFacts() {
		QuestSnapshot snapshot = new QuestSnapshot(7, 80030, QuestStatus.NONE, 0, Map.of())
			.withRace(Race.ELYOS)
			.withEventActivities(Map.of(80029, true, 80032, false));

		assertTrue(QuestConditionEvaluator.matches(ProgressLayout.empty(), snapshot,
			List.of(new QuestCondition.PlayerRaceIs(Race.ELYOS), new QuestCondition.EventActive(80029))));
		assertFalse(QuestConditionEvaluator.matches(ProgressLayout.empty(), snapshot,
			List.of(new QuestCondition.PlayerRaceIs(Race.ASMODIANS))));
		assertTrue(QuestConditionEvaluator.matches(ProgressLayout.empty(), snapshot,
			List.of(new QuestCondition.EventActive(80032, false))));
		assertFalse(QuestConditionEvaluator.matches(ProgressLayout.empty(), snapshot,
			List.of(new QuestCondition.EventActive(80031))));
	}
}
