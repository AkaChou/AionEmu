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
 * 账号累计在线时长 DAO 的 MySQL 8 实现。
 * MySQL 8 AccountPlayTimeDAO implementation using JdbcClient.
 * @author Updated for MySQL 8
 */
@Slf4j
@Repository
public class AccountPlayTimeDAO extends com.aionemu.loginserver.dao.AccountPlayTimeDAO {

    private static final String UPDATE_QUERY = "INSERT INTO account_playtime (`account_id`, `accumulated_online`) VALUES (?, ?) "
        + "ON DUPLICATE KEY UPDATE `accumulated_online` = `accumulated_online` + ?";

    private final JdbcClient jdbcClient;

    public AccountPlayTimeDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public AccountPlayTimeDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    @Override
    public boolean update(final Integer accountId, final AccountTime accountTime) {
        try {
            long onlineTime = accountTime.getAccumulatedOnlineTime();
            int affected = jdbcClient.sql(UPDATE_QUERY)
                .param(accountId)
                .param(onlineTime)
                .param(onlineTime)
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.f2c20e9730e3", accountId), e);
            return false;
        }
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
