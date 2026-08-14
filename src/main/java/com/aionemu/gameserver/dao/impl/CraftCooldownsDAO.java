package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 制作冷却 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of CraftCooldownsDAO.
 *
 * Updated for MySQL 8.
 *
 * @author synchro2
 */
@Slf4j
public class CraftCooldownsDAO extends com.aionemu.gameserver.dao.CraftCooldownsDAO {


    /** 插入更新制作冷却 SQL / Insert or update craft cooldown SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `craft_cooldowns` (`player_id`, `delay_id`, `reuse_time`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `reuse_time` = VALUES(`reuse_time`)";
 /** 按玩家 SQL 的删除制作冷却 / Delete craft cooldowns by player SQL*/
    private static final String DELETE_QUERY = "DELETE FROM `craft_cooldowns` WHERE `player_id` = ?";
    /** 查询制作冷却 SQL / Select craft cooldowns SQL*/
    private static final String SELECT_QUERY = "SELECT `delay_id`, `reuse_time` FROM `craft_cooldowns` WHERE `player_id` = ?";
    /** 删除过期制作冷却 SQL / Delete expired craft cooldowns SQL */
    private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `craft_cooldowns` WHERE `reuse_time` < ?";

    /**
     * 加载玩家制作冷却。
     * Loads craft cooldowns for a player.
     *
     * @param player 玩家 / player
     */
    @Override
    public void loadCraftCooldowns(Player player) {
        Map<Integer, Long> cooldowns = new HashMap<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rset = stmt.executeQuery()) {
                long currentTime = System.currentTimeMillis();

                while (rset.next()) {
                    int delayId = rset.getInt("delay_id");
                    long reuseTime = rset.getLong("reuse_time");

                    if (reuseTime > currentTime) {
                        cooldowns.put(delayId, reuseTime);
                    }
                }
            }

            player.getCraftCooldownList().setCraftCoolDowns(cooldowns);

        } catch (SQLException e) {
            log.error(I18n.get("log.a61919a3fe85", player.getObjectId(), e));
        }
    }

    /**
     * 保存玩家制作冷却。
     * Stores craft cooldowns for a player.
     *
     * @param player 玩家 / player
     */
    @Override
    public void storeCraftCooldowns(Player player) {
        Map<Integer, Long> cooldowns = player.getCraftCooldownList().getCraftCoolDowns();

        if (cooldowns == null || cooldowns.isEmpty()) {
            return;
        }

        Map<Integer, Long> validCooldowns = new ConcurrentHashMap<>();
        long currentTime = System.currentTimeMillis();

        for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
            if (entry.getValue() > currentTime) {
                validCooldowns.put(entry.getKey(), entry.getValue());
            }
        }

        if (validCooldowns.isEmpty()) {
            deleteCraftCoolDowns(player);
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

                for (Map.Entry<Integer, Long> entry : validCooldowns.entrySet()) {
                    insertStmt.setInt(1, player.getObjectId());
                    insertStmt.setInt(2, entry.getKey());
                    insertStmt.setLong(3, entry.getValue());
                    insertStmt.addBatch();
                    batchCount++;

                    if (batchCount % 100 == 0) {
                        insertStmt.executeBatch();
                    }
                }

                if (batchCount % 100 != 0) {
                    insertStmt.executeBatch();
                }
            }

            con.commit();

        } catch (SQLException e) {
            log.error(I18n.get("log.0b287ae80175", player.getObjectId(), e));
        }
    }

    /**
     * 删除玩家全部制作冷却。
     * Deletes all craft cooldowns for a player.
     *
     * @param player 玩家 / player
     */
    private void deleteCraftCoolDowns(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, player.getObjectId());
            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error(I18n.get("log.01f57aee7b11", player.getObjectId(), e));
        }
    }

    /**
     * 删除已过期的制作冷却。
     * Deletes expired craft cooldowns.
     */
    public void deleteExpiredCraftCooldowns() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXPIRED_QUERY)) {

            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                log.info(I18n.get("log.ed15fea2671c", deleted));
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.234aa73d4c36", e));
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
