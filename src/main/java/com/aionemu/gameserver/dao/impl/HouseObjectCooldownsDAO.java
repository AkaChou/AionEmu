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
 * 房屋物件冷却 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of HouseObjectCooldownsDAO.
 *
 * Updated for MySQL 8.
 *
 * @author Rolandas
 */
@Slf4j
public class HouseObjectCooldownsDAO extends com.aionemu.gameserver.dao.HouseObjectCooldownsDAO {


    /** 插入或更新冷却 SQL / Insert or update cooldown SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `house_object_cooldowns` (`player_id`, `object_id`, `reuse_time`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `reuse_time` = VALUES(`reuse_time`)";
    /** 删除冷却按玩家 SQL / Delete cooldowns by player SQL*/
    private static final String DELETE_QUERY = "DELETE FROM `house_object_cooldowns` WHERE `player_id` = ?";
    /** 查询冷却 SQL / Select cooldowns SQL*/
    private static final String SELECT_QUERY = "SELECT `object_id`, `reuse_time` FROM `house_object_cooldowns` WHERE `player_id` = ?";
    /** 删除过期冷却 SQL / Delete expired cooldowns SQL */
    private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `house_object_cooldowns` WHERE `reuse_time` < ?";

    /**
     * 加载玩家房屋物件冷却。
     * Loads house object cooldowns for a player.
     *
     * @param player 玩家 / player
     */
    @Override
    public void loadHouseObjectCooldowns(Player player) {
        String query = SELECT_QUERY;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setInt(1, player.getObjectId());

            Map<Integer, Long> cooldowns = new HashMap<>();

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int objectId = rset.getInt("object_id");
                    long reuseTime = rset.getLong("reuse_time");

                    if (reuseTime > System.currentTimeMillis()) {
                        cooldowns.put(objectId, reuseTime);
                    }
                }
            }

            player.getHouseObjectCooldownList().setHouseObjectCooldowns(cooldowns);

        } catch (SQLException e) {
            log.error(I18n.get("log.fa08c06bca16", player.getObjectId(), e));
        }
    }

    /**
     * 保存玩家房屋物件冷却。
     * Stores house object cooldowns for a player.
     *
     * @param player 玩家 / player
     */
    @Override
    public void storeHouseObjectCooldowns(Player player) {
        Map<Integer, Long> cooldowns = player.getHouseObjectCooldownList().getHouseObjectCooldowns();

        if (cooldowns == null || cooldowns.isEmpty()) {
            return;
        }

        String deleteQuery = DELETE_QUERY;
        String insertQuery = INSERT_QUERY;

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement deleteStmt = con.prepareStatement(deleteQuery)) {
                deleteStmt.setInt(1, player.getObjectId());
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = con.prepareStatement(insertQuery)) {
                long currentTime = System.currentTimeMillis();
                int batchCount = 0;

                for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
                    if (entry.getValue() <= currentTime) {
                        continue;
                    }

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
            log.error(I18n.get("log.4f3f74a85b17", player.getObjectId(), e));
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

    /**
     * 删除已过期的房屋物件冷却。
     * Deletes expired house object cooldowns.
     */
    public void deleteExpiredCooldowns() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXPIRED_QUERY)) {

            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                log.info(I18n.get("log.2cdfa73a3757", deleted));
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.7fc2432f11b4", e));
        }
    }
}
