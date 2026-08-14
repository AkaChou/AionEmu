package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 玩家技能冷却 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerCooldownsDAO.
 *
 * @author nrg, Updated for MySQL 8
 */
@Slf4j
public class PlayerCooldownsDAO extends com.aionemu.gameserver.dao.PlayerCooldownsDAO {


    /** 插入或更新冷却 / Insert or update cooldown */
    private static final String INSERT_QUERY = "INSERT INTO `player_cooldowns` (`player_id`, `cooldown_id`, `reuse_delay`) " + "VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `reuse_delay` = VALUES(`reuse_delay`)";

    /** 删除玩家全部冷却 / Delete all cooldowns of a player */
    private static final String DELETE_QUERY = "DELETE FROM `player_cooldowns` WHERE `player_id` = ?";

    /** 查询玩家冷却列表 / Select player cooldowns */
    private static final String SELECT_QUERY = "SELECT `cooldown_id`, `reuse_delay` FROM `player_cooldowns` WHERE `player_id` = ?";

    /** 删除已过期冷却 / Delete expired cooldowns */
    private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `player_cooldowns` WHERE `reuse_delay` < ?";

    /** 统计玩家有效冷却数量 / Count active cooldowns for a player */
    private static final String COUNT_QUERY = "SELECT COUNT(*) FROM `player_cooldowns` WHERE `player_id` = ? AND `reuse_delay` > ?";

    /** 冷却可持久化判定（剩余时间大于 28 秒） / Predicate for persistable cooldowns (remaining > 28s) */
    private static final Predicate<Long> COOLDOWN_PREDICATE = reuseDelay -> reuseDelay != null && reuseDelay - System.currentTimeMillis() > 28000;

    /**
     * 从数据库加载玩家技能冷却并应用到玩家对象。
     * Loads player skill cooldowns from the database and applies them to the player.
     *
     * @param player 玩家 / player
     */
    @Override
    public void loadPlayerCooldowns(Player player) {
        Map<Integer, Long> validCooldowns = new ConcurrentHashMap<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rset = stmt.executeQuery()) {
                long currentTime = System.currentTimeMillis();

                while (rset.next()) {
                    int cooldownId = rset.getInt("cooldown_id");
                    long reuseDelay = rset.getLong("reuse_delay");

                    if (reuseDelay > currentTime && DataManager.SKILL_DATA.shouldPersistCooldown(cooldownId)) {
                        validCooldowns.put(cooldownId, reuseDelay);
                    }
                }
            }

            for (Map.Entry<Integer, Long> entry : validCooldowns.entrySet()) {
                player.setSkillCoolDown(entry.getKey(), entry.getValue());
            }

            log.debug("Loaded {} cooldowns for player {}", validCooldowns.size(), player.getObjectId());

        } catch (SQLException e) {
            log.error(I18n.get("log.11da0c8c55d9", player.getObjectId(), e));
        }
    }

    /**
     * 将玩家当前有效技能冷却持久化到数据库。
     * Persists the player's currently valid skill cooldowns to the database.
     *
     * @param player 玩家 / player
     */
    @Override
    public void storePlayerCooldowns(Player player) {
        Map<Integer, Long> cooldowns = player.getSkillCoolDowns();

        if (cooldowns == null || cooldowns.isEmpty()) {
            deletePlayerCooldowns(player);
            return;
        }

        Map<Integer, Long> validCooldowns = new ConcurrentHashMap<>();
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
            Long reuseDelay = entry.getValue();
            if (COOLDOWN_PREDICATE.test(reuseDelay) && DataManager.SKILL_DATA.shouldPersistCooldown(entry.getKey())) {
                validCooldowns.put(entry.getKey(), reuseDelay);
            }
        }

        if (validCooldowns.isEmpty()) {
            deletePlayerCooldowns(player);
            return;
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement deleteStmt = con.prepareStatement(DELETE_QUERY)) {
                deleteStmt.setInt(1, player.getObjectId());
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = con.prepareStatement(INSERT_QUERY)) {
                int batchCount = 0;
                final int BATCH_SIZE = 100;

                for (Map.Entry<Integer, Long> entry : validCooldowns.entrySet()) {
                    insertStmt.setInt(1, player.getObjectId());
                    insertStmt.setInt(2, entry.getKey());
                    insertStmt.setLong(3, entry.getValue());
                    insertStmt.addBatch();
                    batchCount++;

                    if (batchCount % BATCH_SIZE == 0) {
                        int[] results = insertStmt.executeBatch();
                        log.debug("Executed batch insert of {} cooldowns for player {}", results.length, player.getObjectId());
                    }
                }

                if (batchCount % BATCH_SIZE != 0) {
                    int[] results = insertStmt.executeBatch();
                    log.debug("Executed final batch insert of {} cooldowns for player {}", results.length, player.getObjectId());
                }
            }

            con.commit();
            log.debug("Stored {} cooldowns for player {}", validCooldowns.size(), player.getObjectId());

        } catch (SQLException e) {
            log.error(I18n.get("log.5f4dd6d0855d", player.getObjectId(), e));
        }
    }

    /**
     * 删除玩家的全部冷却记录。
     * Deletes all cooldown records for the player.
     *
     * @param player 玩家 / player
     */
    private void deletePlayerCooldowns(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, player.getObjectId());
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                log.debug("Deleted {} cooldowns for player {}", deleted, player.getObjectId());
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.4b0c2a651d40", player.getObjectId(), e));
        }
    }

    /**
     * 删除全局已过期的冷却记录。
     * Deletes globally expired cooldown records.
     *
     * @return 删除行数 / deleted row count
     */
    public int deleteExpiredCooldowns() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXPIRED_QUERY)) {

            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                log.info(I18n.get("log.607cffacb3e9", deleted));
            }

            return deleted;

        } catch (SQLException e) {
            log.error(I18n.get("log.0a6ba1f18c8f", e));
            return 0;
        }
    }

    /**
     * 获取玩家当前仍有效的冷却数量。
     * Returns the count of currently active cooldowns for a player.
     *
     * player id
     *
     * @param playerId 玩家 ID / player id
     * @return 有效冷却数量 / active cooldown count
     */
    public int getActiveCooldownsCount(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(COUNT_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setLong(2, System.currentTimeMillis());

            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt(1);
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.5eff82579a26", playerId, e));
        }

        return 0;
    }

    /**
     * 删除玩家的指定冷却条目。
     * Deletes a specific cooldown entry for a player.
     *
     * @param playerId 玩家 ID / player id
     * @param cooldownId 冷却 ID / cooldown id
     *
     * @return 是否删除成功 / whether deleted
     */
    public boolean deletePlayerCooldown(int playerId, int cooldownId) {
        String deleteSpecificQuery = "DELETE FROM `player_cooldowns` WHERE `player_id` = ? AND `cooldown_id` = ?";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(deleteSpecificQuery)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, cooldownId);

            int deleted = stmt.executeUpdate();
            return deleted > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.a6fbc235f8b8", cooldownId, playerId, e));
            return false;
        }
    }

    /**
     * 判断玩家是否仍持有指定冷却。
     * Checks whether the player still has the specified active cooldown.
     *
     * @param playerId 玩家 ID / player id
     * @param cooldownId 冷却 ID / cooldown id
     * @return 是否存在 / whether present
     */
    public boolean hasCooldown(int playerId, int cooldownId) {
        String checkQuery = "SELECT COUNT(*) FROM `player_cooldowns` WHERE `player_id` = ? AND `cooldown_id` = ? AND `reuse_delay` > ?";

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(checkQuery)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, cooldownId);
            stmt.setLong(3, System.currentTimeMillis());

            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.5f9103b23516", cooldownId, playerId, e));
        }

        return false;
    }

    /**
     * 批量存储多名玩家的冷却数据。
     * Batch-stores cooldowns for multiple players.
     *
     * @param playersCooldowns 玩家到冷却映射 / player-to-cooldown map
     */
    public void storeMultiplePlayersCooldowns(Map<Player, Map<Integer, Long>> playersCooldowns) {
        if (playersCooldowns == null || playersCooldowns.isEmpty()) {
            return;
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement deleteStmt = con.prepareStatement(DELETE_QUERY)) {
                for (Player player : playersCooldowns.keySet()) {
                    deleteStmt.setInt(1, player.getObjectId());
                    deleteStmt.addBatch();
                }
                deleteStmt.executeBatch();
            }

            try (PreparedStatement insertStmt = con.prepareStatement(INSERT_QUERY)) {
                int batchCount = 0;

                for (Map.Entry<Player, Map<Integer, Long>> playerEntry : playersCooldowns.entrySet()) {
                    Player player = playerEntry.getKey();
                    Map<Integer, Long> cooldowns = playerEntry.getValue();

                    if (cooldowns == null || cooldowns.isEmpty()) {
                        continue;
                    }

                    long currentTime = System.currentTimeMillis();

                    for (Map.Entry<Integer, Long> cooldownEntry : cooldowns.entrySet()) {
                        Long reuseDelay = cooldownEntry.getValue();
                        if (reuseDelay != null && reuseDelay > currentTime
                            && DataManager.SKILL_DATA.shouldPersistCooldown(cooldownEntry.getKey())) {
                            insertStmt.setInt(1, player.getObjectId());
                            insertStmt.setInt(2, cooldownEntry.getKey());
                            insertStmt.setLong(3, reuseDelay);
                            insertStmt.addBatch();
                            batchCount++;

                            if (batchCount % 1000 == 0) {
                                insertStmt.executeBatch();
                            }
                        }
                    }
                }

                if (batchCount % 1000 != 0) {
                    insertStmt.executeBatch();
                }
            }

            con.commit();
            log.info(I18n.get("log.8d5799510bef", playersCooldowns.size()));

        } catch (SQLException e) {
            log.error(I18n.get("log.815f20825b79", e));
        }
    }

    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
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
