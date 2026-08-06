package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

class QuestCurrencyPortFailClosedTest {
	@Test
	void anUncomposedCurrencyPortCannotSilentlySkipDebits() {
		QuestCurrencyPort port = new QuestCurrencyPort() {
			@Override
			public void preflight(java.sql.Connection connection, QuestSnapshot snapshot,
					List<QuestAction.GrantReward> rewards) {
			}

			@Override
			public QuestTransactionParticipant apply(java.sql.Connection connection, QuestSnapshot snapshot,
					List<QuestAction.GrantReward> rewards) {
				return QuestTransactionParticipant.none();
			}
		};
		List<QuestAction.DecreaseCurrency> debits = List.of(
			new QuestAction.DecreaseCurrency(QuestRewardKind.GOLD, 1));
		QuestSnapshot snapshot = new QuestSnapshot(7, 1001, QuestStatus.START, 0, Map.of(), Map.of());

		assertThrows(SQLException.class, () -> port.preflightDebits(null, snapshot, debits));
		assertThrows(SQLException.class, () -> port.applyDebits(null, snapshot, debits));
	}
}
