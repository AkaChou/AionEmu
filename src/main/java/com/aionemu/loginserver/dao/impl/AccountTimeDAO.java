package com.aionemu.loginserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.model.AccountTime;

import lombok.extern.slf4j.Slf4j;

/**
 * 账号会话/惩罚时间 DAO 的 MySQL 8 实现。
 * MySQL 8 AccountTimeDAO implementation using JdbcClient.
 * @author Updated for MySQL 8
 */
@Slf4j
@Repository
public class AccountTimeDAO extends com.aionemu.loginserver.dao.AccountTimeDAO {

    private static final String REPLACE_QUERY = "REPLACE INTO account_time (account_id, last_active, expiration_time, "
        + "session_duration, accumulated_online, accumulated_rest, penalty_end) "
        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_QUERY = "SELECT * FROM account_time WHERE account_id = ?";

    private final JdbcClient jdbcClient;

    public AccountTimeDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public AccountTimeDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    @Override
    public boolean updateAccountTime(final int accountId, final AccountTime accountTime) {
        try {
            int affected = jdbcClient.sql(REPLACE_QUERY)
                .param((long) accountId)
                .param(accountTime.getLastLoginTime())
                .param(accountTime.getExpirationTime())
                .param(accountTime.getSessionDuration())
                .param(accountTime.getAccumulatedOnlineTime())
                .param(accountTime.getAccumulatedRestTime())
                .param(accountTime.getPenaltyEnd())
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.ff2d8c2ec1de", accountId), e);
            return false;
        }
    }

    @Override
    public AccountTime getAccountTime(int accountId) {
        try {
            return jdbcClient.sql(SELECT_QUERY)
                .param((long) accountId)
                .query((rs, _) -> {
                    AccountTime accountTime = new AccountTime();
                    accountTime.setLastLoginTime(rs.getTimestamp("last_active"));
                    accountTime.setSessionDuration(rs.getLong("session_duration"));
                    accountTime.setAccumulatedOnlineTime(rs.getLong("accumulated_online"));
                    accountTime.setAccumulatedRestTime(rs.getLong("accumulated_rest"));
                    accountTime.setPenaltyEnd(rs.getTimestamp("penalty_end"));
                    accountTime.setExpirationTime(rs.getTimestamp("expiration_time"));
                    return accountTime;
                })
                .optional()
                .orElse(null);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.acdac4f0d5c7", accountId), e);
            return null;
        }
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
