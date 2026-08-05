package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestPlayerFactsConditionTest {
	@Test
	void playerInGroupUsesCapturedFactsAndFailsClosedWhenUnknown() {
		QuestCondition inGroup = new QuestCondition.PlayerInGroup();
		QuestCondition solo = new QuestCondition.PlayerInGroup(false);
		QuestSnapshot grouped = new QuestSnapshot(7, 10032, QuestStatus.START, 0, Map.of())
			.withTeamFacts(new QuestTeamFacts(true, false));
		QuestSnapshot alone = new QuestSnapshot(7, 10032, QuestStatus.START, 0, Map.of())
			.withTeamFacts(new QuestTeamFacts(false, false));
		QuestSnapshot unknown = new QuestSnapshot(7, 10032, QuestStatus.START, 0, Map.of());

		assertTrue(QuestConditionEvaluator.matches(ProgressLayout.empty(), grouped, List.of(inGroup)));
		assertFalse(QuestConditionEvaluator.matches(ProgressLayout.empty(), alone, List.of(inGroup)));
		assertTrue(QuestConditionEvaluator.matches(ProgressLayout.empty(), alone, List.of(solo)));
		assertFalse(QuestConditionEvaluator.matches(ProgressLayout.empty(), unknown, List.of(inGroup)));
	}

	@Test
	void advancedClassMatchesConcreteClassAndNotItsStartingClass() {
		QuestCondition gladiator = new QuestCondition.AdvancedClassIs(PlayerClass.GLADIATOR);
		QuestSnapshot actual = new QuestSnapshot(7, 11102, QuestStatus.REWARD, 0, Map.of())
			.withStartingClass(PlayerClass.WARRIOR)
			.withPlayerClass(PlayerClass.GLADIATOR);
		QuestSnapshot normalizedOnly = new QuestSnapshot(7, 11102, QuestStatus.REWARD, 0, Map.of())
			.withStartingClass(PlayerClass.WARRIOR);

		assertTrue(QuestConditionEvaluator.matches(ProgressLayout.empty(), actual, List.of(gladiator)));
		assertFalse(QuestConditionEvaluator.matches(ProgressLayout.empty(), normalizedOnly, List.of(gladiator)));
	}
}
