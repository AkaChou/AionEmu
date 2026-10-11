package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerTransformDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家变身 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerTransformDAO using JdbcClient.
 * Updated for MySQL 8.
 */
@Slf4j
@Repository
public class PlayerTransfoDAO extends PlayerTransformDAO {

    /** 插入或更新变形 SQL / Insert or update transform SQL */
    private static final String INSERT_QUERY = "INSERT INTO `player_transform` (`player_id`, `panel_id`, `item_id`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `panel_id` = VALUES(`panel_id`), `item_id` = VALUES(`item_id`)";
    /** 查询变形 SQL / Select transform SQL */
    private static final String SELECT_QUERY = "SELECT `panel_id`, `item_id` FROM `player_transform` WHERE `player_id` = ?";
    /** 删除变形 SQL / Delete transform SQL */
    private static final String DELETE_QUERY = "DELETE FROM `player_transform` WHERE `player_id` = ?";
    /** 更新变形 SQL / Update transform SQL */
    private static final String UPDATE_QUERY = "UPDATE `player_transform` SET `panel_id` = ?, `item_id` = ? WHERE `player_id` = ?";

    private final JdbcClient jdbcClient;

    public PlayerTransfoDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public PlayerTransfoDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 加载玩家变身数据。
     * Loads player transform data.
     * @param player 玩家 / player
     */
    @Override
    public void loadPlTransfo(Player player) {
        try {
            jdbcClient.sql(SELECT_QUERY)
                .param(player.getObjectId())
                .query((rset, _) -> {
                    int panelId = rset.getInt("panel_id");
                    int itemId = rset.getInt("item_id");

                    player.getTransformModel().setPanelId(panelId);
                    player.getTransformModel().setItemId(itemId);
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.81bdb310b4b8", player.getObjectId()), e);
        }
    }

    /**
     * 保存玩家变身数据。
     * Stores player transform data.
     * @param playerId 玩家 ID / player id
     * @param panelId 面板 ID / panel id
     * @param itemId 物品 ID / item id
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean storePlTransfo(int playerId, int panelId, int itemId) {
        try {
            return jdbcClient.sql(INSERT_QUERY)
                .param(playerId)
                .param(panelId)
                .param(itemId)
                .update() > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.0ec4dcbf87ba", playerId), e);
            return false;
        }
    }

    /**
     * 删除玩家变身数据。
     * Deletes player transform data.
     * @param playerId 玩家 ID / player id
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean deletePlTransfo(int playerId) {
        try {
            return jdbcClient.sql(DELETE_QUERY)
                .param(playerId)
                .update() > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.f096dcaaec6b", playerId), e);
            return false;
        }
    }

    /**
     * 更新玩家变身数据；若无记录则插入。
     * Updates player transform data; inserts if no row exists.
     * @param playerId 玩家 ID / player id
     * @param panelId 面板 ID / panel id
     * @param itemId 物品 ID / item id
     * @return 是否成功 / whether successful
     */
    public boolean updatePlTransfo(int playerId, int panelId, int itemId) {
        try {
            int updated = jdbcClient.sql(UPDATE_QUERY)
                .param(panelId)
                .param(itemId)
                .param(playerId)
                .update();

            if (updated == 0) {
                return storePlTransfo(playerId, panelId, itemId);
            }

            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.7aefe66a2cfb", playerId), e);
            return false;
        }
    }

    /**
     * 保存玩家当前变身模型状态。
     * Saves the player's current transform model state.
     * @param player 玩家 / player
     * @return 是否成功 / whether successful
     */
    
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
}
