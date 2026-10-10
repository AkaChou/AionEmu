package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家变量 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerVarsDAO using JdbcClient.
 * @author KID
 */
@Slf4j
@Repository
public class PlayerVarsDAO extends com.aionemu.gameserver.dao.PlayerVarsDAO {

    /** 查询变量 SQL / Select variables SQL*/
    private static final String SELECT_QUERY = "SELECT param,value FROM player_vars WHERE player_id=?";
    /** 插入变量 SQL / Insert variable SQL*/
    private static final String INSERT_QUERY = "INSERT INTO player_vars (`player_id`, `param`, `value`, `time`) VALUES (?,?,?,NOW()) ON DUPLICATE KEY UPDATE `value`=VALUES(`value`), `time`=NOW()";
    /** 删除变量 SQL / Delete variable SQL*/
    private static final String DELETE_QUERY = "DELETE FROM player_vars WHERE player_id=? AND param=?";

    private final JdbcClient jdbcClient;

    public PlayerVarsDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public PlayerVarsDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 加载玩家全部自定义变量。
     * Loads all custom variables for a player.
     * @param playerId 玩家 ID / player id
     * @return 变量映射 / variable map
     */
    @Override
    public Map<String, Object> load(final int playerId) {
        final Map<String, Object> map = new LinkedHashMap<>();

        try {
            jdbcClient.sql(SELECT_QUERY)
                .param(playerId)
                .query((rset, rowNum) -> {
                    String key = rset.getString("param");
                    String value = rset.getString("value");
                    map.put(key, value);
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.eef4c3c016c8", playerId), e);
        }
        return map;
    }

    /**
     * 设置玩家变量。
     * Sets a player variable.
     * @param playerId 玩家 ID / player id
     * @param key 变量键 / variable key
     * @param value 变量值 / variable value
     * @return 是否设置成功 / whether the set succeeded
     */
    @Override
    public boolean set(final int playerId, final String key, final Object value) {
        try {
            jdbcClient.sql(INSERT_QUERY)
                .param(playerId)
                .param(key)
                .param(value.toString())
                .update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.093f2900065e", playerId, key), e);
            return false;
        }
    }

    /**
     * 删除玩家变量。
     * Removes a player variable.
     * @param playerId 玩家 ID / player id
     * @param key 变量键 / variable key
     * @return 是否删除成功 / whether the remove succeeded
     */
    @Override
    public boolean remove(final int playerId, final String key) {
        try {
            jdbcClient.sql(DELETE_QUERY)
                .param(playerId)
                .param(key)
                .update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.c3f19e683101", playerId, key), e);
            return false;
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     * @param database 数据库名 / database name
     * @param majorVersion 主版本 / major version
     * @param minorVersion 次版本 / minor version
     * @return 是否支持 / whether supported
     */
    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return DAOUtils.supports(database, majorVersion, minorVersion);
    }
}
