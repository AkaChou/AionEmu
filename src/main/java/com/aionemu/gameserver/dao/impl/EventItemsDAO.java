package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.event.MaxCountOfDay;

import lombok.extern.slf4j.Slf4j;

/**
 * 活动物品计数 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of EventItemsDAO using JdbcClient.
 * Created by wanke on 03/03/2017.
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
@Repository
public class EventItemsDAO extends com.aionemu.gameserver.dao.EventItemsDAO {

    /** 插入或更新活动物品数量 SQL / Insert or update event item counts SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `event_items` (`player_id`, `item_id`, `counts`) VALUES (?,?,?) " + "ON DUPLICATE KEY UPDATE `counts` = VALUES(`counts`)";
    /** 删除活动物品按玩家 SQL / Delete event items by player SQL*/
    private static final String DELETE_QUERY = "DELETE FROM `event_items` WHERE `player_id`=?";
    /** 删除按物品 ID 的活动物品 SQL / Delete event items by item id SQL*/
    private static final String DELETE_ITEM_QUERY = "DELETE FROM `event_items` WHERE `item_id`=?";
    /** 查询活动物品数量 SQL / Select event item counts SQL*/
    private static final String SELECT_QUERY = "SELECT `item_id`, `counts` FROM `event_items` WHERE `player_id`=?";

    private final JdbcClient jdbcClient;

    public EventItemsDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public EventItemsDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 加载玩家活动物品当日上限计数。
     * Loads daily event item max-count data for a player.
     * @param player 玩家 / player
     */
    @Override
    public void loadItems(final Player player) {
        try {
            jdbcClient.sql(SELECT_QUERY)
                .param(player.getObjectId())
                .query((rset, rowNum) -> {
                    int itemId = rset.getInt("item_id");
                    int counts = rset.getInt("counts");
                    player.addItemMaxCountOfDay(itemId, counts);
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.5aff73432829", player.getObjectId()), e);
        }
    }

    /**
     * 保存玩家活动物品当日上限计数。
     * Stores daily event item max-count data for a player.
     * @param player 玩家 / player
     */
    @Override
    public void storeItems(Player player) {
        deleteItems(player);

        Map<Integer, MaxCountOfDay> itemsm = player.getItemMaxThisCounts();
        if (itemsm == null || itemsm.isEmpty()) {
            return;
        }

        try {
            for (Map.Entry<Integer, MaxCountOfDay> entry : itemsm.entrySet()) {
                jdbcClient.sql(INSERT_QUERY)
                    .param(player.getObjectId())
                    .param(entry.getKey())
                    .param(entry.getValue().getThisCount())
                    .update();
            }
            player.clearItemMaxThisCount();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.cdff3db2bd19", player.getObjectId()), e);
        }
    }

    /**
     * 按物品 ID 删除全部玩家的活动物品记录。
     * Deletes event item records for all players by item id.
     * @param itemId 物品 ID / item id
     */
    @Override
    public void deleteItems(final int itemId) {
        try {
            jdbcClient.sql(DELETE_ITEM_QUERY)
                .param(itemId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.903226c3be37", itemId), e);
        }
    }

    /**
     * 删除指定玩家的全部活动物品记录。
     * Deletes all event item records for a player.
     * @param player 玩家 / player
     */
    private void deleteItems(final Player player) {
        try {
            jdbcClient.sql(DELETE_QUERY)
                .param(player.getObjectId())
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.bf40ce06a63c", player.getObjectId()), e);
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
