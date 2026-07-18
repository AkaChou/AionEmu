package com.aionemu.gameserver.dao.impl;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** MySQL 8 全服共享限量任务名额持久化。 */
@Slf4j
public class LimitedQuestDAO extends com.aionemu.gameserver.dao.LimitedQuestDAO {

	static final String INITIALIZE_QUERY = "INSERT IGNORE INTO `limited_quest_counters` (`quest_id`, `remaining`) VALUES (?, ?)";
	static final String ACQUIRE_QUERY = "UPDATE `limited_quest_counters` SET `remaining` = `remaining` - 1 WHERE `quest_id` = ? AND `remaining` > 0";
	static final String RECOVER_QUERY = "UPDATE `limited_quest_counters` SET `remaining` = LEAST(`remaining` + ?, ?) WHERE `quest_id` = ?";

	@Override
	public boolean tryAcquire(int questId, int maxCount) {
		if (questId <= 0 || maxCount <= 0) {
			return false;
		}
		try (Connection connection = DatabaseFactory.getConnection()) {
			connection.setAutoCommit(false);
			try {
				initialize(connection, questId, maxCount);
				int updated;
				try (PreparedStatement statement = connection.prepareStatement(ACQUIRE_QUERY)) {
					statement.setInt(1, questId);
					updated = statement.executeUpdate();
				}
				connection.commit();
				return updated == 1;
			} catch (SQLException e) {
				rollback(connection, e);
				return false;
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.limited_quest.acquire_failed", questId), e);
			return false;
		}
	}

	@Override
	public boolean recover(int questId, int amount, int maxCount) {
		if (questId <= 0 || amount <= 0 || maxCount <= 0) {
			return false;
		}
		try (Connection connection = DatabaseFactory.getConnection()) {
			connection.setAutoCommit(false);
			try {
				initialize(connection, questId, maxCount);
				try (PreparedStatement statement = connection.prepareStatement(RECOVER_QUERY)) {
					statement.setInt(1, amount);
					statement.setInt(2, maxCount);
					statement.setInt(3, questId);
					statement.executeUpdate();
				}
				connection.commit();
				return true;
			} catch (SQLException e) {
				rollback(connection, e);
				return false;
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.limited_quest.recover_failed", questId), e);
			return false;
		}
	}

	private static void initialize(Connection connection, int questId, int maxCount) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(INITIALIZE_QUERY)) {
			statement.setInt(1, questId);
			statement.setInt(2, maxCount);
			statement.executeUpdate();
		}
	}

	private static void rollback(Connection connection, SQLException cause) {
		try {
			connection.rollback();
		} catch (SQLException rollbackError) {
			cause.addSuppressed(rollbackError);
		}
		log.error(I18n.get("log.limited_quest.transaction_failed"), cause);
	}

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
