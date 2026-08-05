package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestGenderConditionTest {
	@Test
	void matchesOnlyTheCapturedPlayerGenderAndFailsClosedWhenUnknown() {
		QuestCondition condition = new QuestCondition.GenderIs(Gender.MALE);
		QuestSnapshot male = new QuestSnapshot(7, 10521, QuestStatus.START, 0, Map.of())
			.withGender(Gender.MALE);
		QuestSnapshot female = new QuestSnapshot(7, 10521, QuestStatus.START, 0, Map.of())
			.withGender(Gender.FEMALE);
		QuestSnapshot unknown = new QuestSnapshot(7, 10521, QuestStatus.START, 0, Map.of());

		assertTrue(QuestConditionEvaluator.matches(ProgressLayout.empty(), male, List.of(condition)));
		assertFalse(QuestConditionEvaluator.matches(ProgressLayout.empty(), female, List.of(condition)));
		assertFalse(QuestConditionEvaluator.matches(ProgressLayout.empty(), unknown, List.of(condition)));
	}
}
