package com.aionemu.gameserver.dao.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家技能冷却 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerCooldownsDAO using JdbcClient.
 * @author nrg, Updated for MySQL 8
 */
@Slf4j
@Repository
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

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public PlayerCooldownsDAO() {
        this(DatabaseFactory.getJdbcClient(), DatabaseFactory.getJdbcTemplate(), DatabaseFactory.getTransactionTemplate());
    }

    @Autowired
    public PlayerCooldownsDAO(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate must not be null");
    }

    public PlayerCooldownsDAO(JdbcClient jdbcClient) {
        this(jdbcClient, DatabaseFactory.getJdbcTemplate(), DatabaseFactory.getTransactionTemplate());
    }

    /**
     * 从数据库加载玩家技能冷却并应用到玩家对象。
     * Loads player skill cooldowns from the database and applies them to the player.
     * @param player 玩家 / player
     */
    @Override
    public void loadPlayerCooldowns(Player player) {
        Map<Integer, Long> validCooldowns = new ConcurrentHashMap<>();

        try {
            long currentTime = System.currentTimeMillis();

            jdbcClient.sql(SELECT_QUERY)
                .param(player.getObjectId())
                .query((rset, _) -> {
                    int cooldownId = rset.getInt("cooldown_id");
                    long reuseDelay = rset.getLong("reuse_delay");

                    if (reuseDelay > currentTime && DataManager.SKILL_DATA.shouldPersistCooldown(cooldownId)) {
                        validCooldowns.put(cooldownId, reuseDelay);
                    }
                    return null;
                })
                .list();

            for (Map.Entry<Integer, Long> entry : validCooldowns.entrySet()) {
                player.setSkillCoolDown(entry.getKey(), entry.getValue());
            }

            log.debug("Loaded {} cooldowns for player {}", validCooldowns.size(), player.getObjectId());

        } catch (DataAccessException e) {
            log.error(I18n.get("log.11da0c8c55d9", player.getObjectId()), e);
        }
    }

    /**
     * 将玩家当前有效技能冷却持久化到数据库。
     * Persists the player's currently valid skill cooldowns to the database.
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

        try {
            transactionTemplate.executeWithoutResult(_ -> {
                jdbcClient.sql(DELETE_QUERY)
                    .param(player.getObjectId())
                    .update();

                List<Map.Entry<Integer, Long>> entryList = new ArrayList<>(validCooldowns.entrySet());
                jdbcTemplate.batchUpdate(INSERT_QUERY, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Map.Entry<Integer, Long> entry = entryList.get(i);
                        ps.setInt(1, player.getObjectId());
                        ps.setInt(2, entry.getKey());
                        ps.setLong(3, entry.getValue());
                    }

                    @Override
                    public int getBatchSize() {
                        return entryList.size();
                    }
                });
            });
            log.debug("Stored {} cooldowns for player {}", validCooldowns.size(), player.getObjectId());
        } catch (RuntimeException e) {
            log.error(I18n.get("log.5f4dd6d0855d", player.getObjectId()), e);
        }
    }

    /**
     * 删除玩家的全部冷却记录。
     * Deletes all cooldown records for the player.
     * @param player 玩家 / player
     */
    private void deletePlayerCooldowns(Player player) {
        try {
            int deleted = jdbcClient.sql(DELETE_QUERY)
                .param(player.getObjectId())
                .update();

            if (deleted > 0) {
                log.debug("Deleted {} cooldowns for player {}", deleted, player.getObjectId());
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.4b0c2a651d40", player.getObjectId()), e);
        }
    }

    /**
     * 删除全局已过期的冷却记录。
     * Deletes globally expired cooldown records.
     * @return 删除行数 / deleted row count
     */
    
    /**
     * 获取玩家当前仍有效的冷却数量。
     * Returns the count of currently active cooldowns for a player.
     * @param playerId 玩家 ID / player id
     * @return 有效冷却数量 / active cooldown count
     */
    
    /**
     * 删除玩家的指定冷却条目。
     * Deletes a specific cooldown entry for a player.
     * @param playerId 玩家 ID / player id
     * @param cooldownId 冷却 ID / cooldown id
     * @return 是否删除成功 / whether deleted
     */
    
    /**
     * 判断玩家是否仍持有指定冷却。
     * Checks whether the player still has the specified active cooldown.
     * @param playerId 玩家 ID / player id
     * @param cooldownId 冷却 ID / cooldown id
     * @return 是否存在 / whether present
     */
    
    /**
     * 批量存储多名玩家的冷却数据。
     * Batch-stores cooldowns for multiple players.
     * @param playersCooldowns 玩家到冷却映射 / player-to-cooldown map
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
