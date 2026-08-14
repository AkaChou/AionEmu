package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerLunaShop;
import java.sql.*;

/**
 * 玩家 Luna 商店 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerLunaShopDAO.
 *
 * Created by wanke on 13/02/2017.
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
public class PlayerLunaShopDAO extends com.aionemu.gameserver.dao.PlayerLunaShopDAO {


    /** 插入露娜商店记录 SQL / Insert Luna shop record SQL */
    private static final String ADD_QUERY = "INSERT INTO `player_luna_shop` (`player_id`, `free_under`, `free_munition`, `free_chest`) VALUES (?,?,?,?)";
    /** 查询露娜商店 SQL / Select Luna shop SQL */
    private static final String SELECT_QUERY = "SELECT * FROM `player_luna_shop` WHERE `player_id`=?";
    /** 删除全部露娜商店 SQL / Delete all Luna shop SQL */
    private static final String DELETE_QUERY = "DELETE FROM `player_luna_shop`";
    /** 更新露娜商店 SQL / Update Luna shop SQL */
    private static final String UPDATE_QUERY = "UPDATE player_luna_shop SET `free_under`=?, `free_munition`=?, `free_chest`=? WHERE `player_id`=?";

    /**
     * 加载玩家 Luna 商店数据。
     * Loads player Luna shop data.
     *
     * @param player 玩家 / player
     */
    @Override
    public void load(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    boolean under = rset.getBoolean("free_under");
                    boolean factory = rset.getBoolean("free_munition");
                    boolean chest = rset.getBoolean("free_chest");

                    PlayerLunaShop pls = new PlayerLunaShop(under, factory, chest);
                    pls.setPersistentState(PersistentState.UPDATED);
                    player.setPlayerLunaShop(pls);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.064f9d2d26d3", player.getObjectId(), e));
        }
    }

    /**
     * 新增玩家 Luna 商店记录。
     * Adds a player Luna shop record.
     *
     * @param playerId 玩家 ID / player id
     * @param freeUnderpath 免费地下通道 / free underpath
     * @param freeFactory free factory
     * @param freeChest free chest
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean add(final int playerId, final boolean freeUnderpath, final boolean freeFactory, final boolean freeChest) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(ADD_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setBoolean(2, freeUnderpath);
            stmt.setBoolean(3, freeFactory);
            stmt.setBoolean(4, freeChest);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.44819cc1f7de", playerId, e));
            return false;
        }
    }

    /**
     * 清空全部 Luna 商店记录。
     * Deletes all Luna shop records.
     *
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean delete() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.4cc438cef8a8", e));
            return false;
        }
    }

    /**
     * 持久化玩家 Luna 商店数据。
     * Stores player Luna shop data.
     *
     * @param player 玩家 / player
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean store(Player player) {
        boolean success = false;

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            PlayerLunaShop bind = player.getPlayerLunaShop();
            if (bind != null) {
                switch (bind.getPersistentState()) {
                    case UPDATE_REQUIRED:
                    case NEW:
                        success = updateLunaShop(con, player);
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
            con.commit();
        } catch (SQLException e) {
            log.error(I18n.get("log.3148132d5775", player.getObjectId(), e));
        }
        return success;
    }

    /**
     * 更新玩家 Luna 商店记录。
     * Updates player Luna shop record.
     *
     * @param con 数据库连接 / database connection
     * @param player 玩家 / player
     * @return 是否成功 / whether succeeded
     */
    private boolean updateLunaShop(Connection con, Player player) {
        try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            PlayerLunaShop lr = player.getPlayerLunaShop();
            if (lr == null) {
                return false;
            }

            stmt.setBoolean(1, lr.isFreeUnderpath());
            stmt.setBoolean(2, lr.isFreeFactory());
            stmt.setBoolean(3, lr.isFreeChest());
            stmt.setInt(4, player.getObjectId());

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.500d6b5a68a9", player.getObjectId(), e));
            return false;
        }
    }

    /**
     * 按对象 ID 设置 Luna 商店数据。
     * Sets Luna shop data by object id.
     *
     * @param obj 玩家对象 ID / player object id
     * @param freeUnderpath 免费地下通道 / free underpath
     * @param freeFactory free factory
     * @param freeChest free chest
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean setLunaShopByObjId(int obj, final boolean freeUnderpath, final boolean freeFactory, final boolean freeChest) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setBoolean(1, freeUnderpath);
            stmt.setBoolean(2, freeFactory);
            stmt.setBoolean(3, freeChest);
            stmt.setInt(4, obj);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.a8a27eb03080", obj, e));
            return false;
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     *
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
