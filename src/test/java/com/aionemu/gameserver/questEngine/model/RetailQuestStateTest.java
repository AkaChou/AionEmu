package com.aionemu.gameserver.questEngine.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.aionemu.gameserver.questEngine.definition.QuestMetadata;
import com.aionemu.gameserver.questEngine.definition.RepeatPolicy;

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

	@Test
	void repeatEligibilityUsesCanonicalRepeatPolicyAndCooldown() {
		QuestState once = new QuestState(1, QuestStatus.COMPLETE, 0, 1, null, null, null);
		QuestState dailyReady = new QuestState(2, QuestStatus.COMPLETE, 0, 1,
			new Timestamp(System.currentTimeMillis() - 1000), null, null);
		QuestState dailyWaiting = new QuestState(2, QuestStatus.COMPLETE, 0, 1,
			new Timestamp(System.currentTimeMillis() + 60_000), null, null);

		assertFalse(once.canRepeat(metadata("once", RepeatPolicy.once())));
		assertTrue(dailyReady.canRepeat(metadata("daily", new RepeatPolicy(255, 0, true, false))));
		assertFalse(dailyWaiting.canRepeat(metadata("daily", new RepeatPolicy(255, 0, true, false))));
		assertFalse(dailyReady.canRepeat(null));
	}

	private static QuestState state(QuestStatus status) {
		return new QuestState(1, status, 0, 0, null, null, null);
	}

	private static QuestMetadata metadata(String name, RepeatPolicy repeat) {
		return new QuestMetadata(name, 0, 1, 80, Set.of(), "QUEST", repeat,
			Set.of(), List.of(), List.of(), List.of());
	}
}
