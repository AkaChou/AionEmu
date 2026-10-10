package com.aionemu.loginserver.dao.impl;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.model.base.BannedMacEntry;

import lombok.extern.slf4j.Slf4j;

/**
 * MAC 封禁 DAO 的 MySQL 8 实现。
 * MySQL 8 BannedMacDAO implementation using JdbcClient.
 */
@Slf4j
public class BannedMacDAO extends com.aionemu.loginserver.dao.BannedMacDAO {

    private static final String SELECT_ALL_QUERY = "SELECT `address`, `time`, `details` FROM `banned_mac`";
    private static final String REPLACE_QUERY = "REPLACE INTO `banned_mac` (`address`, `time`, `details`) VALUES (?, ?, ?)";
    private static final String DELETE_QUERY = "DELETE FROM `banned_mac` WHERE address = ?";
    private static final String CLEAN_EXPIRED_QUERY = "DELETE FROM `banned_mac` WHERE time < CURDATE()";

    private final JdbcClient jdbcClient;

    public BannedMacDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    public BannedMacDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    @Override
    public Map<String, BannedMacEntry> load() {
        Map<String, BannedMacEntry> map = new ConcurrentHashMap<>();
        try {
            jdbcClient.sql(SELECT_ALL_QUERY)
                .query((rs, rowNum) -> {
                    String address = rs.getString("address");
                    return new BannedMacEntry(address, rs.getTimestamp("time"), rs.getString("details"));
                })
                .list()
                .forEach(entry -> map.put(entry.getMac(), entry));
        } catch (DataAccessException e) {
            log.error(I18n.get("log.8e046bb74f14"), e);
        }
        return map;
    }

    @Override
    public boolean update(BannedMacEntry entry) {
        try {
            int affected = jdbcClient.sql(REPLACE_QUERY)
                .param(entry.getMac())
                .param(entry.getTime())
                .param(entry.getDetails())
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.78eb3d669103", entry.getMac()), e);
            return false;
        }
    }

    @Override
    public boolean remove(String address) {
        try {
            int affected = jdbcClient.sql(DELETE_QUERY)
                .param(address)
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.1f75cfd0c929", address), e);
            return false;
        }
    }

    @Override
    public void cleanExpiredBans() {
        try {
            int deleted = jdbcClient.sql(CLEAN_EXPIRED_QUERY).update();
            if (deleted > 0) {
                log.info(I18n.get("log.bfee9964538a", deleted));
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.043ac354757c"), e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
