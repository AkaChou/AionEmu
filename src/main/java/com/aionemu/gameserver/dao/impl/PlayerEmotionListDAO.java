package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.emotion.Emotion;
import com.aionemu.gameserver.model.gameobjects.player.emotion.EmotionList;
import java.sql.*;

/**
 * 玩家表情列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerEmotionListDAO.
 *
 * @author Mr. Poke, Updated for MySQL 8
 */
@Slf4j
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

    /**
     * 加载玩家表情列表到内存，并跳过已过期表情。
     * Loads the player's emotion list into memory, skipping expired emotions.
     *
     * @param player 玩家 / player
     */
    @Override
    public void loadEmotions(Player player) {
        EmotionList emotions = new EmotionList(player);

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rset = stmt.executeQuery()) {
                long currentTime = System.currentTimeMillis();

                while (rset.next()) {
                    int emotionId = rset.getInt("emotion");
                    int remaining = rset.getInt("remaining");

                    // 跳过过期表情 / Skip expired emotions
                    if (remaining > 0 && remaining <= currentTime) {
                        deleteEmotion(player.getObjectId(), emotionId);
                        continue;
                    }

                    emotions.add(emotionId, remaining, false);
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.efdea0fd00c0", player.getObjectId(), e));
        }

        player.setEmotions(emotions);
    }

    /**
     * 插入或更新单个玩家表情。
     * Inserts or updates a single player emotion.
     *
     * 玩家 / player
     * emotion
     */
    @Override
    public void insertEmotion(Player player, Emotion emotion) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, player.getObjectId());
            stmt.setInt(2, emotion.getId());
            stmt.setInt(3, emotion.getExpireTime());
            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error(I18n.get("log.a9e6401899a7", player.getObjectId(), e));
        }
    }

    /**
     * 删除玩家的指定表情。
     * Deletes a specific emotion for the player.
     *
     * player id
     * emotion id
     */
    @Override
    public void deleteEmotion(int playerId, int emotionId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, emotionId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error(I18n.get("log.6015c7958d1b", playerId, emotionId, e));
        }
    }

    /**
     * 删除玩家的全部表情。
     * Deletes all emotions of the player.
     *
     * player id
     */
    public void deleteAllEmotions(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_ALL_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error(I18n.get("log.c241415a012d", playerId, e));
        }
    }

    /**
     * 批量清理已过期的表情记录。
     * Batch-deletes expired emotion records.
     */
    public void deleteExpiredEmotions() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXPIRED_QUERY)) {

            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                log.info(I18n.get("log.dd96ae8f46f9", deleted));
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.7659e80a0909", e));
        }
    }

    /**
     * 批量插入或更新玩家表情列表。
     * Batch-inserts or updates the player's emotion list.
     *
     * 玩家 / player
     * emotion list
     */
    public void insertEmotions(Player player, EmotionList emotions) {
        if (emotions == null || emotions.getEmotions().isEmpty()) {
            return;
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
                int batchCount = 0;

                for (Emotion emotion : emotions.getEmotions()) {
                    stmt.setInt(1, player.getObjectId());
                    stmt.setInt(2, emotion.getId());
                    stmt.setInt(3, emotion.getExpireTime());
                    stmt.addBatch();
                    batchCount++;

                    if (batchCount % 50 == 0) {
                        stmt.executeBatch();
                    }
                }

                if (batchCount % 50 != 0) {
                    stmt.executeBatch();
                }
            }

            con.commit();

        } catch (SQLException e) {
            log.error(I18n.get("log.615e4d6f5316", player.getObjectId(), e));
        }
    }

    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
     *
     * @param databaseName 数据库名称 / database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
