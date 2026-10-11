package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerLunaShop;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家 Luna 商店 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerLunaShopDAO using JdbcClient.
 * Created by wanke on 13/02/2017.
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
@Repository
public class PlayerLunaShopDAO extends com.aionemu.gameserver.dao.PlayerLunaShopDAO {

    /** 插入露娜商店记录 SQL / Insert Luna shop record SQL */
    private static final String ADD_QUERY = "INSERT INTO `player_luna_shop` (`player_id`, `free_under`, `free_munition`, `free_chest`) VALUES (?,?,?,?)";
    /** 查询露娜商店 SQL / Select Luna shop SQL */
    private static final String SELECT_QUERY = "SELECT * FROM `player_luna_shop` WHERE `player_id`=?";
    /** 删除全部露娜商店 SQL / Delete all Luna shop SQL */
    private static final String DELETE_QUERY = "DELETE FROM `player_luna_shop`";
    /** 更新露娜商店 SQL / Update Luna shop SQL */
    private static final String UPDATE_QUERY = "UPDATE player_luna_shop SET `free_under`=?, `free_munition`=?, `free_chest`=? WHERE `player_id`=?";

    private final JdbcClient jdbcClient;

    public PlayerLunaShopDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public PlayerLunaShopDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 加载玩家 Luna 商店数据。
     * Loads player Luna shop data.
     * @param player 玩家 / player
     */
    @Override
    public void load(Player player) {
        try {
            jdbcClient.sql(SELECT_QUERY)
                .param(player.getObjectId())
                .query((rset, _) -> {
                    boolean under = rset.getBoolean("free_under");
                    boolean factory = rset.getBoolean("free_munition");
                    boolean chest = rset.getBoolean("free_chest");

                    PlayerLunaShop pls = new PlayerLunaShop(under, factory, chest);
                    pls.setPersistentState(PersistentState.UPDATED);
                    player.setPlayerLunaShop(pls);
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.064f9d2d26d3", player.getObjectId()), e);
        }
    }

    /**
     * 新增玩家 Luna 商店记录。
     * Adds a player Luna shop record.
     * @param playerId 玩家 ID / player id
     * @param freeUnderpath 免费地下通道 / free underpath
     * @param freeFactory free factory
     * @param freeChest free chest
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean add(final int playerId, final boolean freeUnderpath, final boolean freeFactory, final boolean freeChest) {
        try {
            jdbcClient.sql(ADD_QUERY)
                .param(playerId)
                .param(freeUnderpath)
                .param(freeFactory)
                .param(freeChest)
                .update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.44819cc1f7de", playerId), e);
            return false;
        }
    }

    /**
     * 清空全部 Luna 商店记录。
     * Deletes all Luna shop records.
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean delete() {
        try {
            jdbcClient.sql(DELETE_QUERY).update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.4cc438cef8a8"), e);
            return false;
        }
    }

    /**
     * 持久化玩家 Luna 商店数据。
     * Stores player Luna shop data.
     * @param player 玩家 / player
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean store(Player player) {
        boolean success = false;
        PlayerLunaShop bind = player.getPlayerLunaShop();
        if (bind != null) {
            switch (bind.getPersistentState()) {
                case UPDATE_REQUIRED:
                case NEW:
                    success = updateLunaShop(player);
                    log.debug("DB updated for player {}", player.getObjectId());
                    break;
                default:
                    success = true;
                    break;
            }
            if (success) {
                bind.setPersistentState(PersistentState.UPDATED);
            }
        }
        return success;
    }

    /**
     * 更新玩家 Luna 商店记录。
     * Updates player Luna shop record.
     * @param player 玩家 / player
     * @return 是否成功 / whether succeeded
     */
    private boolean updateLunaShop(Player player) {
        PlayerLunaShop lr = player.getPlayerLunaShop();
        if (lr == null) {
            return false;
        }

        try {
            jdbcClient.sql(UPDATE_QUERY)
                .param(lr.isFreeUnderpath())
                .param(lr.isFreeFactory())
                .param(lr.isFreeChest())
                .param(player.getObjectId())
                .update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.500d6b5a68a9", player.getObjectId()), e);
            return false;
        }
    }

    /**
     * 按对象 ID 设置 Luna 商店数据。
     * Sets Luna shop data by object id.
     * @param obj 玩家对象 ID / player object id
     * @param freeUnderpath 免费地下通道 / free underpath
     * @param freeFactory free factory
     * @param freeChest free chest
     */
    @Override
    public void setLunaShopByObjId(int obj, final boolean freeUnderpath, final boolean freeFactory, final boolean freeChest) {
        try {
            jdbcClient.sql(UPDATE_QUERY)
                .param(freeUnderpath)
                .param(freeFactory)
                .param(freeChest)
                .param(obj)
                .update();
            return;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.a8a27eb03080", obj), e);
            return;
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
}
