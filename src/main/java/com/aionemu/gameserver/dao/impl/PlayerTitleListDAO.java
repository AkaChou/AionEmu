package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.title.Title;
import com.aionemu.gameserver.model.gameobjects.player.title.TitleList;

import lombok.extern.slf4j.Slf4j;

/**
 * 玩家称号列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerTitleListDAO using JdbcClient.
 * @author xavier, Updated for MySQL 8
 */
@Slf4j
@Repository
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

    private final JdbcClient jdbcClient;
    private final JdbcTemplate jdbcTemplate;

    public PlayerTitleListDAO() {
        this(DatabaseFactory.getJdbcClient(), DatabaseFactory.getJdbcTemplate());
    }

    @Autowired
    public PlayerTitleListDAO(JdbcClient jdbcClient, JdbcTemplate jdbcTemplate) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    public PlayerTitleListDAO(JdbcClient jdbcClient) {
        this(jdbcClient, DatabaseFactory.getJdbcTemplate());
    }

    /**
     * 加载玩家称号列表，并跳过已过期称号。
     * Loads the player's title list, skipping expired titles.
     * @param playerId 玩家 ID / player id
     * @return 称号列表 / title list
     */
    @Override
    public TitleList loadTitleList(int playerId) {
        TitleList titleList = new TitleList();
        long currentTime = System.currentTimeMillis();

        try {
            jdbcClient.sql(LOAD_QUERY)
                .param(playerId)
                .query((rset, _) -> {
                    int id = rset.getInt("title_id");
                    int remaining = rset.getInt("remaining");

                    // 跳过过期头衔 / Skip expired titles
                    if (remaining > 0 && remaining <= currentTime) {
                        removeTitle(playerId, id);
                        return null;
                    }

                    titleList.addEntry(id, remaining);
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.7f727baa8f02", playerId), e);
        }

        return titleList;
    }

    /**
     * 在调用方事务连接上保存玩家一条称号，与任务状态同事务提交/回滚。
     * Stores a title on the caller-owned transaction connection.
     * @param con 调用方事务连接 / caller-owned transaction connection
     * @param playerId 玩家 object id / player object id
     * @param titleId 称号 ID / title id
     * @param remaining 剩余毫秒数，0 表示永久 / remaining ms, 0 for permanent
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
     * @param player 玩家 / player
     * @param entry 称号条目 / title entry
     * @return 是否成功 / whether successful
     */
    @Override
    public void storeTitles(Player player, Title entry) {
        try {
                        jdbcClient.sql(INSERT_QUERY)
                .param(player.getObjectId())
                .param(entry.id())
                .param(entry.getExpireTime())
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.8893f821f5fc", player.getObjectId(), entry.id()), e);
                        ;
        }
    }

    /**
     * 删除玩家的指定称号。
     * Removes a specific title from the player.
     * @param playerId 玩家 ID / player id
     * @param titleId 称号 ID / title id
     * @return 是否成功 / whether successful
     */
    @Override
    public void removeTitle(int playerId, int titleId) {
        try {
                        jdbcClient.sql(DELETE_QUERY)
                .param(playerId)
                .param(titleId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.4ff4bda7010a", playerId, titleId), e);
                        ;
        }
    }

    /**
     * 删除玩家的全部称号。
     * Removes all titles of the player.
     * @param playerId 玩家 ID / player id
     * @return 是否成功 / whether successful
     */

    /**
     * 批量保存玩家称号列表。
     * Batch-stores the player's title list.
     * @param player 玩家 / player
     * @param titles 称号列表 / title list
     * @return 是否成功 / whether successful
     */

    /**
     * 批量清理已过期的称号记录。
     * Batch-deletes expired title records.
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
