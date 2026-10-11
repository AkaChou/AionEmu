package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import lombok.extern.slf4j.Slf4j;

/**
 * 房屋物件冷却 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of HouseObjectCooldownsDAO using JdbcClient.
 * Updated for MySQL 8.
 * @author Rolandas
 */
@Slf4j
@Repository
public class HouseObjectCooldownsDAO extends com.aionemu.gameserver.dao.HouseObjectCooldownsDAO {

    /** 插入或更新冷却 SQL / Insert or update cooldown SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `house_object_cooldowns` (`player_id`, `object_id`, `reuse_time`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `reuse_time` = VALUES(`reuse_time`)";
    /** 删除冷却按玩家 SQL / Delete cooldowns by player SQL*/
    private static final String DELETE_QUERY = "DELETE FROM `house_object_cooldowns` WHERE `player_id` = ?";
    /** 查询冷却 SQL / Select cooldowns SQL*/
    private static final String SELECT_QUERY = "SELECT `object_id`, `reuse_time` FROM `house_object_cooldowns` WHERE `player_id` = ?";
    /** 删除过期冷却 SQL / Delete expired cooldowns SQL */
    private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `house_object_cooldowns` WHERE `reuse_time` < ?";

    private final JdbcClient jdbcClient;

    public HouseObjectCooldownsDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public HouseObjectCooldownsDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 加载玩家房屋物件冷却。
     * Loads house object cooldowns for a player.
     * @param player 玩家 / player
     */
    @Override
    public void loadHouseObjectCooldowns(Player player) {
        try {
            Map<Integer, Long> cooldowns = new HashMap<>();
            long now = System.currentTimeMillis();

            jdbcClient.sql(SELECT_QUERY)
                .param(player.getObjectId())
                .query((rs, _) -> {
                    int objectId = rs.getInt("object_id");
                    long reuseTime = rs.getLong("reuse_time");
                    if (reuseTime > now) {
                        cooldowns.put(objectId, reuseTime);
                    }
                    return null;
                })
                .list();

            player.getHouseObjectCooldownList().setHouseObjectCooldowns(cooldowns);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.fa08c06bca16", player.getObjectId()), e);
        }
    }

    /**
     * 保存玩家房屋物件冷却。
     * Stores house object cooldowns for a player.
     * @param player 玩家 / player
     */
    @Override
    public void storeHouseObjectCooldowns(Player player) {
        Map<Integer, Long> cooldowns = player.getHouseObjectCooldownList().getHouseObjectCooldowns();

        if (cooldowns == null || cooldowns.isEmpty()) {
            return;
        }

        try {
            jdbcClient.sql(DELETE_QUERY)
                .param(player.getObjectId())
                .update();

            long currentTime = System.currentTimeMillis();
            for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
                if (entry.getValue() <= currentTime) {
                    continue;
                }

                jdbcClient.sql(INSERT_QUERY)
                    .param(player.getObjectId())
                    .param(entry.getKey())
                    .param(entry.getValue())
                    .update();
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.4f3f74a85b17", player.getObjectId()), e);
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     * @param databaseName 数据库名 / database name
     * @param majorVersion 主版本 / major version
     * @param minorVersion 次版本 / minor version
     * @return 是否支持 / whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }

    /**
     * 删除已过期的房屋物件冷却。
     * Deletes expired house object cooldowns.
     */
    }
