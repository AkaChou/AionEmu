package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.f2p.F2p;
import com.aionemu.gameserver.model.gameobjects.player.f2p.F2pAccount;
import java.sql.*;

/**
 * F2P 账号 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of F2pDAO.
 *
 * Updated for MySQL 8.
 */
@Slf4j
public class F2pDAO extends com.aionemu.gameserver.dao.F2pDAO {


 /** 插入更新 f2p SQL / Insert or update F2P SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `f2paccount` (`player_id`, `time`) VALUES (?, ?) " + "ON DUPLICATE KEY UPDATE `time` = VALUES(`time`)";
 /** 查询 f2p SQL / Select F2P SQL*/
    private static final String SELECT_QUERY = "SELECT `time` FROM `f2paccount` WHERE `player_id` = ?";
 /** 删除 f2p SQL / Delete F2P SQL*/
    private static final String DELETE_QUERY = "DELETE FROM `f2paccount` WHERE `player_id` = ?";
 /** 更新 f2p SQL / Update F2P SQL*/
    private static final String UPDATE_QUERY = "UPDATE `f2paccount` SET `time` = ? WHERE `player_id` = ?";

    /**
     * 加载玩家 F2P 信息。
     * Loads F2P info for a player.
     *
     * @param player 玩家 / player
     */
    @Override
    public void loadF2pInfo(Player player) {
        F2p f2p = new F2p(player);

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    int time = rset.getInt("time");
                    f2p.add(new F2pAccount(time), false);
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.b700281835c0", player.getObjectId(), e), e);
        }

        player.setF2p(f2p);
    }

    /**
     * 存储 F2P 账号时间。
     * Stores F2P account time.
     *
     * player object id
     * @param time 剩余时间 / remaining time
     * whether succeeded
     */
    @Override
    public boolean storeF2p(int objectId, int time) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, objectId);
            stmt.setInt(2, time);

            int result = stmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.2830d63e3475", objectId, e), e);
            return false;
        }
    }

    /**
     * 更新 F2P 账号时间。
     * Updates F2P account time.
     *
     * player object id
     * @param time 剩余时间 / remaining time
     * whether succeeded
     */
    @Override
    public boolean updateF2p(int objectId, int time) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setInt(1, time);
            stmt.setInt(2, objectId);

            int updated = stmt.executeUpdate();

            if (updated == 0) {
                return storeF2p(objectId, time);
            }

            return true;

        } catch (SQLException e) {
            log.error(I18n.get("log.4295e27fcc73", objectId, e), e);
            return false;
        }
    }

    /**
     * 删除 F2P 账号记录。
     * Deletes F2P account record.
     *
     * player object id
     * whether succeeded
     */
    @Override
    public boolean deleteF2p(int objectId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, objectId);

            int result = stmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.34f50ac7e4cb", objectId, e), e);
            return false;
        }
    }

    /**
     * 保存玩家 F2P 数据。
     * Saves player F2P data.
     *
     * 玩家 / player
     * whether succeeded
     */
    public boolean saveF2p(Player player) {
        F2p f2p = player.getF2p();

        if (f2p == null || f2p.getF2pAccount() == null) {
            return deleteF2p(player.getObjectId());
        }

        F2pAccount account = f2p.getF2pAccount();
        if (account != null) {
            return updateF2p(player.getObjectId(), account.getRemainingTime());
        }

        return true;
    }

    /**
     * 获取 F2P 剩余时间。
     * Gets F2P remaining time.
     *
     * player object id
     * remaining time
     */
    public int getF2pTime(int objectId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, objectId);

            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt("time");
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.efc1390e42c9", objectId, e), e);
        }

        return 0;
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     *
     * database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
