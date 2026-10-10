package com.aionemu.loginserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.GameServerInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * 游戏服务器列表 DAO 的 MySQL 8 实现。
 * MySQL 8 GameServersDAO implementation using JdbcClient.
 * @author Updated for MySQL 8
 */
@Slf4j
@Repository
public class GameServersDAO extends com.aionemu.loginserver.dao.GameServersDAO {

    private static final String SELECT_ALL_QUERY = "SELECT * FROM gameservers ORDER BY id";

    private final JdbcClient jdbcClient;

    public GameServersDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public GameServersDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    @Override
    public Map<Byte, GameServerInfo> getAllGameServers() {
        Map<Byte, GameServerInfo> result = new HashMap<>();
        try {
            jdbcClient.sql(SELECT_ALL_QUERY)
                .query((rs, rowNum) -> {
                    byte id = rs.getByte("id");
                    String ipMask = rs.getString("mask");
                    String password = rs.getString("password");
                    return new GameServerInfo(id, ipMask, password);
                })
                .list()
                .forEach(gsi -> result.put(gsi.getId(), gsi));
        } catch (DataAccessException e) {
            log.error(I18n.get("log.cb987d22f8fa"), e);
        }
        return result;
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
