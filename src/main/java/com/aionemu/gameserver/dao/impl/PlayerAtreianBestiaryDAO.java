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
import com.aionemu.gameserver.dao.PlayerABDAO;
import com.aionemu.gameserver.model.atreian_bestiary.PlayerABEntry;
import com.aionemu.gameserver.model.atreian_bestiary.PlayerABList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家阿特雷亚图鉴 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerABDAO (Atreian Bestiary) using JdbcClient.
 * @author Ranastic
 */
@Slf4j
@Repository
public class PlayerAtreianBestiaryDAO extends PlayerABDAO {

    /** 插入或更新图鉴 SQL / Insert or update bestiary SQL*/
    private static final String INSERT_OR_UPDATE = "INSERT INTO `player_atreian_bestiary` (`player_id`, `id`, `kill_count`, `level`, `claim_reward`) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE `id` = VALUES(`id`), `kill_count` = VALUES(`kill_count`), `level` = VALUES(`level`), `claim_reward` = VALUES(`claim_reward`)";
    /** 查询图鉴 SQL / Select bestiary SQL*/
    private static final String SELECT_QUERY = "SELECT `id`,`kill_count`,`level`,`claim_reward` FROM `player_atreian_bestiary` WHERE `player_id`=?";
    /** 删除图鉴条目 SQL / Delete bestiary entry SQL*/
    private static final String DELETE_QUERY = "DELETE FROM `player_atreian_bestiary` WHERE `player_id`=? AND `id`=?";
    /** 查询击杀数 SQL / Select kill count SQL */
    private static final String SELECT_KILL_COUNT_QUERY = "SELECT `kill_count` FROM `player_atreian_bestiary` WHERE `player_id`=? AND `id`=?";
    /** 查询等级 SQL / Select level SQL */
    private static final String SELECT_LEVEL_QUERY = "SELECT `level` FROM `player_atreian_bestiary` WHERE `player_id`=? AND `id`=?";
    /** 查询领奖标记 SQL / Select claim reward SQL */
    private static final String SELECT_CLAIM_REWARD_QUERY = "SELECT `claim_reward` FROM `player_atreian_bestiary` WHERE `player_id`=? AND `id`=?";

    private final JdbcClient jdbcClient;

    public PlayerAtreianBestiaryDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public PlayerAtreianBestiaryDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 加载玩家图鉴列表。
     * Loads player Atreian bestiary list.
     * @param player 玩家 / player
     * @return 图鉴列表 / bestiary list
     */
    @Override
    public PlayerABList load(Player player) {
        List<PlayerABEntry> cp = new ArrayList<>();

        try {
            cp = jdbcClient.sql(SELECT_QUERY)
                .param(player.getObjectId())
                .query((rset, _) -> {
                    int id = rset.getInt("id");
                    int kill_count = rset.getInt("kill_count");
                    int level = rset.getInt("level");
                    int claimReward = rset.getInt("claim_reward");

                    return new PlayerABEntry(id, kill_count, level, claimReward, PersistentState.UPDATED);
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.e26023dd4433", player.getObjectId()), e);
        }
        return new PlayerABList(cp);
    }

    /**
     * 存储图鉴条目。
     * Stores a bestiary entry.
     * @param objectId 玩家对象 ID / player object id
     * @param id 图鉴 ID / bestiary id
     * @param kill_count 击杀数 / kill count
     * @param level 等级 / level
     * @param claimReward 是否已领奖 / claim reward flag
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean store(int objectId, int id, int kill_count, int level, int claimReward) {
        try {
            jdbcClient.sql(INSERT_OR_UPDATE)
                .param(objectId)
                .param(id)
                .param(kill_count)
                .param(level)
                .param(claimReward)
                .update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.072ceacb9d9e", objectId), e);
            return false;
        }
    }

    /**
     * 删除图鉴条目。
     * Deletes a bestiary entry.
     * @param playerObjId 玩家对象 ID / player object id
     * @param id 图鉴 ID / bestiary id
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean delete(int playerObjId, int id) {
        try {
            jdbcClient.sql(DELETE_QUERY)
                .param(playerObjId)
                .param(id)
                .update();
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.e05ecfff5330", playerObjId), e);
            return false;
        }
    }

    /**
     * 按 ID 获取击杀数。
     * Gets kill count by id.
     * @param playerObjId 玩家对象 ID / player object id
     * @param id 图鉴 ID / bestiary id
     * @return 击杀数 / kill count
     */
    @Override
    public int getKillCountById(final int playerObjId, final int id) {
        try {
            Integer count = jdbcClient.sql(SELECT_KILL_COUNT_QUERY)
                .param(playerObjId)
                .param(id)
                .query((rs, _) -> rs.getInt("kill_count"))
                .optional()
                .orElse(0);
            return count;
        } catch (DataAccessException e) {
            log.debug("No kill count found for player {}, id {}", playerObjId, id);
            return 0;
        }
    }

    /**
     * 按 ID 获取等级。
     * Gets level by id.
     * @param playerObjId 玩家对象 ID / player object id
     * @param id 图鉴 ID / bestiary id
     * @return 等级 / level
     */
    @Override
    public int getLevelById(final int playerObjId, final int id) {
        try {
            Integer level = jdbcClient.sql(SELECT_LEVEL_QUERY)
                .param(playerObjId)
                .param(id)
                .query((rs, _) -> rs.getInt("level"))
                .optional()
                .orElse(0);
            return level;
        } catch (DataAccessException e) {
            log.debug("No level found for player {}, id {}", playerObjId, id);
            return 0;
        }
    }

    /**
     * 按 ID 获取领奖状态。
     * Gets claim reward flag by id.
     * @param playerObjId 玩家对象 ID / player object id
     * @param id 图鉴 ID / bestiary id
     * @return 领取奖励标记 / claim reward flag
     */
    @Override
    public int getClaimRewardById(int playerObjId, int id) {
        try {
            Integer claim = jdbcClient.sql(SELECT_CLAIM_REWARD_QUERY)
                .param(playerObjId)
                .param(id)
                .query((rs, _) -> rs.getInt("claim_reward"))
                .optional()
                .orElse(0);
            return claim;
        } catch (DataAccessException e) {
            log.debug("No claim reward found for player {}, id {}", playerObjId, id);
            return 0;
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
