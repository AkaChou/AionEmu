package com.aionemu.gameserver.instance.handlers.scripts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

class BeshmundirTempleInstanceTest {

	@Test
	void summonsRespondentOnlyForActiveQuestWithOil() {
		QuestState active = new QuestState(30208, QuestStatus.START, 0, 0, null, null, null);
		QuestState completed = new QuestState(30208, QuestStatus.COMPLETE, 0, 1, null, null, null);

		assertTrue(BeshmundirTempleInstance.canSummonRespondent(active, 1));
		assertFalse(BeshmundirTempleInstance.canSummonRespondent(active, 0));
		assertFalse(BeshmundirTempleInstance.canSummonRespondent(completed, 1));
		assertFalse(BeshmundirTempleInstance.canSummonRespondent(null, 1));
	}
}
