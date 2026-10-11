package com.aionemu.gameserver.dao.impl;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * MySQL 8 全服共享限量任务名额持久化。
 * MySQL 8 implementation of LimitedQuestDAO using JdbcClient.
 */
@Slf4j
@Repository
public class LimitedQuestDAO extends com.aionemu.gameserver.dao.LimitedQuestDAO {

	static final String INITIALIZE_QUERY = "INSERT IGNORE INTO `limited_quest_counters` (`quest_id`, `remaining`) VALUES (?, ?)";
	static final String ACQUIRE_QUERY = "UPDATE `limited_quest_counters` SET `remaining` = `remaining` - 1 WHERE `quest_id` = ? AND `remaining` > 0";
	static final String RECOVER_QUERY = "UPDATE `limited_quest_counters` SET `remaining` = LEAST(`remaining` + ?, ?) WHERE `quest_id` = ?";

	private final JdbcClient jdbcClient;
	private final TransactionTemplate transactionTemplate;

	public LimitedQuestDAO() {
		this(DatabaseFactory.getJdbcClient(), DatabaseFactory.getTransactionTemplate());
	}

	@Autowired
	public LimitedQuestDAO(JdbcClient jdbcClient, TransactionTemplate transactionTemplate) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
		this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate must not be null");
	}

	public LimitedQuestDAO(JdbcClient jdbcClient) {
		this(jdbcClient, DatabaseFactory.getTransactionTemplate());
	}

	@Override
	public boolean tryAcquire(int questId, int maxCount) {
		if (questId <= 0 || maxCount <= 0) {
			return false;
		}
		try {
			return Boolean.TRUE.equals(transactionTemplate.execute(_ -> {
				initialize(questId, maxCount);
				int updated = jdbcClient.sql(ACQUIRE_QUERY)
					.param(questId)
					.update();
				return updated == 1;
			}));
		} catch (RuntimeException e) {
			log.error(I18n.get("log.limited_quest.acquire_failed", questId), e);
			return false;
		}
	}

	@Override
	public boolean recover(int questId, int amount, int maxCount) {
		if (questId <= 0 || amount <= 0 || maxCount <= 0) {
			return false;
		}
		try {
			return Boolean.TRUE.equals(transactionTemplate.execute(_ -> {
				initialize(questId, maxCount);
				jdbcClient.sql(RECOVER_QUERY)
					.param(amount)
					.param(maxCount)
					.param(questId)
					.update();
				return true;
			}));
		} catch (RuntimeException e) {
			log.error(I18n.get("log.limited_quest.recover_failed", questId), e);
			return false;
		}
	}

	private void initialize(int questId, int maxCount) {
		jdbcClient.sql(INITIALIZE_QUERY)
			.param(questId)
			.param(maxCount)
			.update();
	}

	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
