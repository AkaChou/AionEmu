package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.ProgressLayout;
import com.aionemu.gameserver.questEngine.definition.QuestCondition;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestSnapshotCurrencyCaptureTest {
	@Test
	void partialCurrencyProjectionFailsClosedEvenWhenOneSourceWasCaptured() {
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0,
			Map.of(), Map.of(QuestRewardKind.GOLD, 100L), true, false, 0);

		assertThrows(IllegalStateException.class, () -> snapshot.balance(QuestRewardKind.GOLD));
	}

	@Test
	void unsupportedWireCurrencyDoesNotBecomeKnownZero() {
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0,
			Map.of(), Map.of(QuestRewardKind.GOLD, 100L));

		assertThrows(IllegalStateException.class, () -> snapshot.balance(QuestRewardKind.CP));
		assertThrows(IllegalStateException.class, () -> snapshot.balance(QuestRewardKind.ABYSS_OP));
		assertFalse(QuestConditionEvaluator.matches(ProgressLayout.empty(), snapshot,
			List.of(new QuestCondition.CurrencyBelow(QuestRewardKind.ABYSS_OP, 1))));
	}

	@Test
	void explicitlyCapturedZeroSupportedBalanceRemainsZero() {
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0,
			Map.of(), Map.of(QuestRewardKind.DP, 0L));

		assertEquals(0L, snapshot.balance(QuestRewardKind.DP));
	}
}
