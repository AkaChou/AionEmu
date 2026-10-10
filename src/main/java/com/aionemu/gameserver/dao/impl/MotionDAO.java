package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.motion.Motion;
import com.aionemu.gameserver.model.gameobjects.player.motion.MotionList;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家动作（Motion）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of MotionDAO using JdbcClient.
 * @author MrPoke
 */
@Slf4j
@Repository
public class MotionDAO extends com.aionemu.gameserver.dao.MotionDAO {

    /** 插入动作 SQL / Insert motion SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `player_motions` (`player_id`, `motion_id`, `active`, `time`) VALUES (?,?,?,?)";
    /** 查询动作列表 SQL / Select motion list SQL*/
    private static final String SELECT_QUERY = "SELECT `motion_id`, `active`, `time` FROM `player_motions` WHERE `player_id`=?";
    /** 删除动作 SQL / Delete motion SQL*/
    private static final String DELETE_QUERY = "DELETE FROM `player_motions` WHERE `player_id`=? AND `motion_id`=?";
    /** 更新动作 SQL / Update motion SQL*/
    private static final String UPDATE_QUERY = "UPDATE `player_motions` SET `active`=? WHERE `player_id`=? AND `motion_id`=?";

    private final JdbcClient jdbcClient;

    public MotionDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public MotionDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
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
     * 加载玩家动作列表并设置到玩家对象。
     * Loads the player's motion list and assigns it to the player.
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
     * @param playerId 玩家 ID / player id
     * @return 动作列表 / motion list
     */
    @Override
    public List<Motion> loadMotions(Integer playerId) {
        List<Motion> motions = new ArrayList<>();

        try {
            motions = jdbcClient.sql(SELECT_QUERY)
                .param(playerId)
                .query((rset, rowNum) -> {
                    int motionId = rset.getInt("motion_id");
                    int time = rset.getInt("time");
                    boolean isActive = rset.getBoolean("active");
                    return new Motion(motionId, time, isActive);
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.f1ef66964c92", playerId), e);
        }
        return motions;
    }

    /**
     * 保存动作。
     * Stores a motion.
     * @param objectId 玩家对象 ID / player object id
     * @param motion 动作 / motion
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean storeMotion(int objectId, Motion motion) {
        try {
            jdbcClient.sql(INSERT_QUERY)
                .param(objectId)
                .param(motion.getId())
                .param(motion.isActive())
                .param(motion.getExpireTime())
                .update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.ee15090dda12", objectId), e);
            return false;
        }
    }

    /**
     * 删除动作。
     * Deletes a motion.
     * @param objectId 玩家对象 ID / player object id
     * @param motionId 动作 ID / motion id
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean deleteMotion(int objectId, int motionId) {
        try {
            jdbcClient.sql(DELETE_QUERY)
                .param(objectId)
                .param(motionId)
                .update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.1bf60097aea6", objectId), e);
            return false;
        }
    }

    /**
     * 更新动作激活状态。
     * Updates the motion active state.
     * @param objectId 玩家对象 ID / player object id
     * @param motion 动作 / motion
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean updateMotion(int objectId, Motion motion) {
        try {
            jdbcClient.sql(UPDATE_QUERY)
                .param(motion.isActive())
                .param(objectId)
                .param(motion.getId())
                .update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.91b2b22576c2", objectId), e);
            return false;
        }
    }
}
