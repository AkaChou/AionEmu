package com.aionemu.loginserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.model.Vip;

import lombok.extern.slf4j.Slf4j;

/**
 * 账号 VIP 状态 DAO 的 MySQL 8 实现。
 * MySQL 8 account VIP persistence using JdbcClient.
 */
@Slf4j
@Repository
public class VipDAO extends com.aionemu.loginserver.dao.VipDAO {

    static final String FIND_QUERY = "SELECT account_id, vip_level, vip_exp, expire_time "
        + "FROM account_vip WHERE account_id = ?";
    static final String SYNC_QUERY = "INSERT IGNORE INTO account_vip (account_id, vip_level, expire_time) "
        + "SELECT id, ?, UNIX_TIMESTAMP() + 30 * 24 * 3600 FROM account_data";
    static final String INSERT_QUERY = "INSERT IGNORE INTO account_vip (account_id, vip_level, expire_time) "
        + "VALUES (?, ?, UNIX_TIMESTAMP() + 30 * 24 * 3600)";

    private final JdbcClient jdbcClient;

    public VipDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public VipDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    @Override
    public Vip findByAccountId(int accountId) {
        try {
            return jdbcClient.sql(FIND_QUERY)
                .param(accountId)
                .query((rs, rowNum) -> new Vip(
                    rs.getInt("account_id"),
                    rs.getInt("vip_level"),
                    rs.getLong("vip_exp"),
                    rs.getLong("expire_time")
                ))
                .optional()
                .orElse(null);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.2a8dc6bf0f91", accountId), e);
            return null;
        }
    }

    @Override
    public int syncMissingAccounts(int level) {
        try {
            return jdbcClient.sql(SYNC_QUERY)
                .param(level)
                .update();
        } catch (DataAccessException e) {
            throw new IllegalStateException("Could not synchronize missing account VIP data", e);
        }
    }

    @Override
    public boolean insertIfAbsent(int accountId, int level) {
        try {
            return jdbcClient.sql(INSERT_QUERY)
                .param(accountId)
                .param(level)
                .update() > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.7bc951a4d602", accountId), e);
            return false;
        }
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
