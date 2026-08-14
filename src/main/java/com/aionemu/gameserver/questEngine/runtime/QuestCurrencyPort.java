package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestAction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/** 基纳/AP/GP 及其他耐久货币奖励的类型化事务边界。 / Typed transactional boundary for kinah/AP/GP and other durable currency rewards. */
public interface QuestCurrencyPort {
	void preflight(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.GrantReward> rewards) throws SQLException;

	default void preflightDebits(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.DecreaseCurrency> debits) throws SQLException {
		if (!debits.isEmpty()) {
			throw new SQLException("currency debit support is not composed");
		}
	}

	default void preflightSets(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.SetCurrency> sets) throws SQLException {
		if (!sets.isEmpty()) {
			throw new SQLException("currency set support is not composed");
		}
	}

	QuestTransactionParticipant apply(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.GrantReward> rewards) throws SQLException;

	default QuestTransactionParticipant applyDebits(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.DecreaseCurrency> debits) throws SQLException {
		if (!debits.isEmpty()) {
			throw new SQLException("currency debit support is not composed");
		}
		return QuestTransactionParticipant.none();
	}

	default QuestTransactionParticipant applySets(Connection connection, QuestSnapshot snapshot,
			List<QuestAction.SetCurrency> sets) throws SQLException {
		if (!sets.isEmpty()) {
			throw new SQLException("currency set support is not composed");
		}
		return QuestTransactionParticipant.none();
	}
}
