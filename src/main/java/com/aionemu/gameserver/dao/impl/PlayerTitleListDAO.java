package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.title.Title;
import com.aionemu.gameserver.model.gameobjects.player.title.TitleList;
import java.sql.*;

/**
 * 玩家称号列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerTitleListDAO.
 *
 * @author xavier, Updated for MySQL 8
 */
@Slf4j
public class PlayerTitleListDAO extends com.aionemu.gameserver.dao.PlayerTitleListDAO {


    /** 加载玩家称号列表 / Load player title list */
    private static final String LOAD_QUERY = "SELECT `title_id`, `remaining` FROM `player_titles` WHERE `player_id` = ? ORDER BY `title_id`";
    /** 插入或更新玩家称号 / Insert or update a player title */
    private static final String INSERT_QUERY = "INSERT INTO `player_titles` (`player_id`, `title_id`, `remaining`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `remaining` = VALUES(`remaining`)";
    /** 删除指定玩家称号 / Delete a specific player title */
    private static final String DELETE_QUERY = "DELETE FROM `player_titles` WHERE `player_id` = ? AND `title_id` = ?";
    /** 删除玩家全部称号 / Delete all titles of a player */
    private static final String DELETE_ALL_QUERY = "DELETE FROM `player_titles` WHERE `player_id` = ?";
    /** 删除已过期称号 / Delete expired titles */
    private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `player_titles` WHERE `remaining` > 0 AND `remaining` < ?";

    /**
     * 加载玩家称号列表，并跳过已过期称号。
     * Loads the player's title list, skipping expired titles.
     *
     * @param playerId 玩家 ID / player id
     * @return 称号列表 / title list
     */
    @Override
    public TitleList loadTitleList(int playerId) {
        TitleList titleList = new TitleList();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(LOAD_QUERY)) {

            stmt.setInt(1, playerId);

            try (ResultSet rset = stmt.executeQuery()) {
                long currentTime = System.currentTimeMillis();

                while (rset.next()) {
                    int id = rset.getInt("title_id");
                    int remaining = rset.getInt("remaining");

                    // 跳过过期头衔 / Skip expired titles
                    if (remaining > 0 && remaining <= currentTime) {
                        removeTitle(playerId, id);
                        continue;
                    }

                    titleList.addEntry(id, remaining);
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.7f727baa8f02", playerId, e));
        }

        return titleList;
    }

    /**
     * 在调用方事务连接上保存玩家一条称号，与任务状态同事务提交/回滚。
     * Stores a title on the caller-owned transaction connection.
     *
     * 调用方事务连接 / caller-owned transaction connection
     * @param con 玩家 object id / player object id
     * @param playerId 称号 ID / title id
     * 剩余毫秒数，0 表示永久 / remaining ms, 0 for permanent
     */
    @Override
    public void storeInTransaction(Connection con, int playerId, int titleId, int remaining) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            stmt.setInt(1, playerId);
            stmt.setInt(2, titleId);
            stmt.setInt(3, remaining);
            stmt.executeUpdate();
        }
    }

    /**
     * 保存单个玩家称号。
     * Stores a single player title.
     *
     * @param player 玩家 / player
     * @param entry 称号条目 / title entry
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean storeTitles(Player player, Title entry) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, player.getObjectId());
            stmt.setInt(2, entry.getId());
            stmt.setInt(3, entry.getExpireTime());

            int result = stmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.8893f821f5fc", player.getObjectId(), entry.getId(), e));
            return false;
        }
    }

    /**
     * 删除玩家的指定称号。
     * Removes a specific title from the player.
     *
     * @param playerId 玩家 ID / player id
     * @param titleId 称号 ID / title id
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean removeTitle(int playerId, int titleId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, titleId);

            int result = stmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.4ff4bda7010a", playerId, titleId, e));
            return false;
        }
    }

    /**
     * 删除玩家的全部称号。
     * Removes all titles of the player.
     *
     * @param playerId 玩家 ID / player id
     * @return 是否成功 / whether successful
     */
    public boolean removeAllTitles(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_ALL_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            log.error(I18n.get("log.a782e260641e", playerId, e));
            return false;
        }
    }

    /**
     * 批量保存玩家称号列表。
     * Batch-stores the player's title list.
     *
     * @param player 玩家 / player
     * @param titles 称号列表 / title list
     * @return 是否成功 / whether successful
     */
    public boolean storeTitles(Player player, TitleList titles) {
        if (titles == null || titles.size() == 0) {
            return true;
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
                int batchCount = 0;

                for (Title title : titles.getTitles()) {
                    stmt.setInt(1, player.getObjectId());
                    stmt.setInt(2, title.getId());
                    stmt.setInt(3, title.getExpireTime());
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
            return true;

        } catch (SQLException e) {
            log.error(I18n.get("log.83d8bd1f33ce", player.getObjectId(), e));
            return false;
        }
    }

    /**
     * 批量清理已过期的称号记录。
     * Batch-deletes expired title records.
     */
    public void deleteExpiredTitles() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXPIRED_QUERY)) {

            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();

            if (deleted > 0) {
                log.info(I18n.get("log.3d05ef4ea399", deleted));
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.1470750923e1", e));
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
