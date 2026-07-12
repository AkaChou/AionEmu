package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.motion.Motion;
import com.aionemu.gameserver.model.gameobjects.player.motion.MotionList;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家动作（Motion）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of MotionDAO.
 *
 * @author MrPoke
 * @rework MATTY
 */
@Slf4j
public class MotionDAO extends com.aionemu.gameserver.dao.MotionDAO {

    /** 插入动作 SQL / Insert motion SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `player_motions` (`player_id`, `motion_id`, `active`, `time`) VALUES (?,?,?,?)";
    /** 查询动作列表 SQL / Select motion list SQL*/
    private static final String SELECT_QUERY = "SELECT `motion_id`, `active`, `time` FROM `player_motions` WHERE `player_id`=?";
    /** 删除动作 SQL / Delete motion SQL*/
    private static final String DELETE_QUERY = "DELETE FROM `player_motions` WHERE `player_id`=? AND `motion_id`=?";
    /** 更新动作 SQL / Update motion SQL*/
    private static final String UPDATE_QUERY = "UPDATE `player_motions` SET `active`=? WHERE `player_id`=? AND `motion_id`=?";

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

    /**
     * 加载玩家动作列表并设置到玩家对象。
     * Loads the player's motion list and assigns it to the player.
     *
     * @param player 玩家 / player
     */
    @Override
    public void loadMotionList(Player player) {
        MotionList motions = new MotionList(player);
        List<Motion> loadedMotions = loadMotions(player.getObjectId());

        if (loadedMotions != null) {
            for (Motion motion : loadedMotions) {
                motions.add(motion, false);
            }
        }
        player.setMotions(motions);
    }

    /**
     * 按玩家 ID 加载动作列表。
     * Loads the motion list by player id.
     *
     * player id
     * motion list
     */
    @Override
    public List<Motion> loadMotions(Integer playerId) {
        List<Motion> motions = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, playerId);
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int motionId = rset.getInt("motion_id");
                    int time = rset.getInt("time");
                    boolean isActive = rset.getBoolean("active");
                    motions.add(new Motion(motionId, time, isActive));
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.f1ef66964c92", playerId, e));
        }
        return motions;
    }

    /**
     * 保存动作。
     * Stores a motion.
     *
     * player object id
     * motion
     * whether successful
     */
    @Override
    public boolean storeMotion(int objectId, Motion motion) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, objectId);
            stmt.setInt(2, motion.getId());
            stmt.setBoolean(3, motion.isActive());
            stmt.setInt(4, motion.getExpireTime());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.ee15090dda12", objectId, e));
            return false;
        }
    }

    /**
     * 删除动作。
     * Deletes a motion.
     *
     * player object id
     * motion id
     * whether successful
     */
    @Override
    public boolean deleteMotion(int objectId, int motionId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, objectId);
            stmt.setInt(2, motionId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.1bf60097aea6", objectId, e));
            return false;
        }
    }

    /**
     * 更新动作激活状态。
     * Updates the motion active state.
     *
     * player object id
     * motion
     * whether successful
     */
    @Override
    public boolean updateMotion(int objectId, Motion motion) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setBoolean(1, motion.isActive());
            stmt.setInt(2, objectId);
            stmt.setInt(3, motion.getId());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.91b2b22576c2", objectId, e));
            return false;
        }
    }
}
