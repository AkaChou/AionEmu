package com.aionemu.gameserver.dao.impl;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.emotion.Emotion;
import com.aionemu.gameserver.model.gameobjects.player.emotion.EmotionList;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家表情列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerEmotionListDAO using JdbcClient.
 * @author Mr. Poke, Updated for MySQL 8
 */
@Slf4j
@Repository
public class PlayerEmotionListDAO extends com.aionemu.gameserver.dao.PlayerEmotionListDAO {

    /** 插入或更新玩家表情 / Insert or update a player emotion */
    private static final String INSERT_QUERY = "INSERT INTO `player_emotions` (`player_id`, `emotion`, `remaining`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `remaining` = VALUES(`remaining`)";
    /** 查询玩家全部表情 / Select all emotions for a player */
    private static final String SELECT_QUERY = "SELECT `emotion`, `remaining` FROM `player_emotions` WHERE `player_id` = ?";
    /** 删除指定玩家表情 / Delete a specific player emotion */
    private static final String DELETE_QUERY = "DELETE FROM `player_emotions` WHERE `player_id` = ? AND `emotion` = ?";
    /** 删除玩家全部表情 / Delete all emotions of a player */
    private static final String DELETE_ALL_QUERY = "DELETE FROM `player_emotions` WHERE `player_id` = ?";
    /** 删除已过期表情 / Delete expired emotions */
    private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `player_emotions` WHERE `remaining` > 0 AND `remaining` < ?";

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public PlayerEmotionListDAO() {
        this(DatabaseFactory.getJdbcClient(), DatabaseFactory.getJdbcTemplate());
    }

    @Autowired
    public PlayerEmotionListDAO(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    public PlayerEmotionListDAO(JdbcClient jdbcClient) {
        this(jdbcClient, DatabaseFactory.getJdbcTemplate());
    }

    /**
     * 加载玩家表情列表到内存，并跳过已过期表情。
     * Loads the player's emotion list into memory, skipping expired emotions.
     * @param player 玩家 / player
     */
    @Override
    public void loadEmotions(Player player) {
        EmotionList emotions = new EmotionList(player);
        long currentTime = System.currentTimeMillis();

        try {
            jdbcClient.sql(SELECT_QUERY)
                .param(player.getObjectId())
                .query((rset, _) -> {
                    int emotionId = rset.getInt("emotion");
                    int remaining = rset.getInt("remaining");

                    // 跳过过期表情 / Skip expired emotions
                    if (remaining > 0 && remaining <= currentTime) {
                        deleteEmotion(player.getObjectId(), emotionId);
                        return null;
                    }

                    emotions.add(emotionId, remaining, false);
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.efdea0fd00c0", player.getObjectId()), e);
        }

        player.setEmotions(emotions);
    }

    /**
     * 插入或更新单个玩家表情。
     * Inserts or updates a single player emotion.
     * @param player 玩家 / player
     * @param emotion 表情 / emotion
     */
    @Override
    public void insertEmotion(Player player, Emotion emotion) {
        try {
            jdbcClient.sql(INSERT_QUERY)
                .param(player.getObjectId())
                .param(emotion.id())
                .param(emotion.getExpireTime())
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.a9e6401899a7", player.getObjectId()), e);
        }
    }

    /**
     * 删除玩家的指定表情。
     * Deletes a specific emotion for the player.
     * @param playerId 玩家 ID / player id
     * @param emotionId 表情 ID / emotion id
     */
    @Override
    public void deleteEmotion(int playerId, int emotionId) {
        try {
            jdbcClient.sql(DELETE_QUERY)
                .param(playerId)
                .param(emotionId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.6015c7958d1b", playerId, emotionId), e);
        }
    }

    /**
     * 删除玩家的全部表情。
     * Deletes all emotions of the player.
     * @param playerId 玩家 ID / player id
     */

    /**
     * 批量清理已过期的表情记录。
     * Batch-deletes expired emotion records.
     */

    /**
     * 批量插入或更新玩家表情列表。
     * Batch-inserts or updates the player's emotion list.
     * @param player 玩家 / player
     * @param emotions 表情列表 / emotion list
     */

    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
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
