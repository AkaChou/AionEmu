package com.aionemu.gameserver.questEngine.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RetailQuestStateTest {

	@Test
	void mapsRetailQuestStatesToAionEmuLifecycle() {
		assertTrue(RetailQuestState.QSTATEI_NONE.matches(null));
		assertTrue(RetailQuestState.QSTATEI_NONE.matches(state(QuestStatus.NONE)));
		assertTrue(RetailQuestState.QSTATEI_NONE.matches(state(QuestStatus.LOCKED)));
		assertFalse(RetailQuestState.QSTATEI_NONE.matches(state(QuestStatus.START)));

		assertTrue(RetailQuestState.QSTATEI_ACQUIRED.matches(state(QuestStatus.START)));
		assertTrue(RetailQuestState.QSTATEI_ACQUIRED.matches(state(QuestStatus.REWARD)));
		assertFalse(RetailQuestState.QSTATEI_ACQUIRED.matches(state(QuestStatus.COMPLETE)));

		assertTrue(RetailQuestState.QSTATEI_SUCCEED.matches(state(QuestStatus.COMPLETE)));
		assertFalse(RetailQuestState.QSTATEI_SUCCEED.matches(state(QuestStatus.REWARD)));
	}

	private static QuestState state(QuestStatus status) {
		return new QuestState(1, status, 0, 0, null, null, null);
	}
}
