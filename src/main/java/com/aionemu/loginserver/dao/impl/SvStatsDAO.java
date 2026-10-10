package com.aionemu.loginserver.dao.impl;

import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * 服务器在线统计 DAO 的 MySQL 8 实现。
 * MySQL 8 SvStatsDAO implementation (server online statistics) using JdbcClient.
 * @author Updated for MySQL 8
 */
@Slf4j
public class SvStatsDAO extends com.aionemu.loginserver.dao.SvStatsDAO {

    /** 更新在线服务器统计 SQL / Update online server stats SQL */
    private static final String UPDATE_ONLINE = "UPDATE `svstats` SET status = ?, current = ?, max = ?, last_update = NOW() WHERE server = ?";
    /** 更新离线服务器统计 SQL / Update offline server stats SQL */
    private static final String UPDATE_OFFLINE = "UPDATE `svstats` SET status = ?, current = ?, last_update = NOW() WHERE server = ?";
    /** Mark all servers offline SQL / Mark all servers offline SQL */
    private static final String UPDATE_ALL_OFFLINE = "UPDATE `svstats` SET status = ?, current = ?, last_update = NOW()";
    /** 插入服务器统计 SQL / Insert server stats SQL */
    private static final String INSERT_STATS = "INSERT INTO svstats (server, status, current, max, last_update) VALUES (?, ?, ?, ?, NOW())";

    private final JdbcClient jdbcClient;

    public SvStatsDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    public SvStatsDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    @Override
    public void update_SvStats_Online(int server, int status, int current, int max) {
        try {
            int updated = jdbcClient.sql(UPDATE_ONLINE)
                .param(status)
                .param(current)
                .param(max)
                .param(server)
                .update();
            if (updated == 0) {
                insertServerStats(server, status, current, max);
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.e8405213aab7", server), e);
        }
    }

    /**
     * 插入新的服务器统计行（更新未命中时调用）。
     * Inserts a new server stats row when update hits zero rows.
     */
    private void insertServerStats(int server, int status, int current, int max) {
        try {
            jdbcClient.sql(INSERT_STATS)
                .param(server)
                .param(status)
                .param(current)
                .param(max)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.ccd3f03c62c4", server), e);
        }
    }

    @Override
    public void update_SvStats_Offline(int server, int status, int current) {
        try {
            int updated = jdbcClient.sql(UPDATE_OFFLINE)
                .param(status)
                .param(current)
                .param(server)
                .update();
            if (updated == 0) {
                insertServerStats(server, status, current, 0);
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.edce6e5f9a35", server), e);
        }
    }

    @Override
    public void update_SvStats_All_Offline(int status, int current) {
        try {
            jdbcClient.sql(UPDATE_ALL_OFFLINE)
                .param(status)
                .param(current)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.4b7fedeec762"), e);
        }
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
