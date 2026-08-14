package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;
import java.sql.*;

/**
 * 玩家生命状态 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerLifeStatsDAO.
 *
 * Updated for MySQL 8.
 *
 * @author Mr. Poke
 */
@Slf4j
public class PlayerLifeStatsDAO extends com.aionemu.gameserver.dao.PlayerLifeStatsDAO {


    /** 插入或更新生命状态 SQL / Insert or update life stats SQL */
    private static final String INSERT_QUERY = "INSERT INTO `player_life_stats` (`player_id`, `hp`, `mp`, `fp`) VALUES (?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE `hp` = VALUES(`hp`), `mp` = VALUES(`mp`), `fp` = VALUES(`fp`)";
    /** 查询生命状态 SQL / Select life stats SQL */
    private static final String SELECT_QUERY = "SELECT `hp`, `mp`, `fp` FROM `player_life_stats` WHERE `player_id` = ?";
    /** 更新生命状态 SQL / Update life stats SQL */
    private static final String UPDATE_QUERY = "UPDATE `player_life_stats` SET `hp` = ?, `mp` = ?, `fp` = ? WHERE `player_id` = ?";

    /**
     * 加载玩家生命状态。
     * Loads player life stats.
     *
     * @param player 玩家 / player
     */
    @Override
    public void loadPlayerLifeStat(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    PlayerLifeStats lifeStats = player.getLifeStats();
                    lifeStats.setCurrentHp(rset.getInt("hp"));
                    lifeStats.setCurrentMp(rset.getInt("mp"));
                    lifeStats.setCurrentFp(rset.getInt("fp"));
                } else {
                    insertPlayerLifeStat(player);
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.df17e20d3575", player.getObjectId(), e));
            insertPlayerLifeStat(player);
        }
    }

    /**
     * 插入玩家生命状态。
     * Inserts player life stats.
     *
     * @param player 玩家 / player
     */
    @Override
    public void insertPlayerLifeStat(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            PlayerLifeStats lifeStats = player.getLifeStats();

            stmt.setInt(1, player.getObjectId());
            stmt.setInt(2, lifeStats.getCurrentHp());
            stmt.setInt(3, lifeStats.getCurrentMp());
            stmt.setInt(4, lifeStats.getCurrentFp());

            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error(I18n.get("log.57de73400d87", player.getObjectId(), e));
        }
    }

    /**
     * 更新玩家生命状态。
     * Updates player life stats.
     *
     * @param player 玩家 / player
     */
    @Override
    public void updatePlayerLifeStat(Player player) {
        PlayerLifeStats lifeStats = player.getLifeStats();
        int hp = lifeStats.getCurrentHp();
        int mp = lifeStats.getCurrentMp();
        int fp = lifeStats.getCurrentFp();

        if (hp < 0) hp = 0;
        if (mp < 0) mp = 0;
        if (fp < 0) fp = 0;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setInt(1, hp);
            stmt.setInt(2, mp);
            stmt.setInt(3, fp);
            stmt.setInt(4, player.getObjectId());

            int updated = stmt.executeUpdate();

            if (updated == 0) {
                insertPlayerLifeStat(player);
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.78b2c8b3d4cc", player.getObjectId(), e));
        }
    }

    /**
     * 批量更新玩家生命状态。
     * Batch updates player life stats.
     *
     * @param players 玩家集合 / players
     */
    public void updatePlayerLifeStats(Iterable<Player> players) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
                int batchCount = 0;

                for (Player player : players) {
                    if (player == null || !player.isOnline()) {
                        continue;
                    }

                    PlayerLifeStats lifeStats = player.getLifeStats();
                    int hp = Math.max(lifeStats.getCurrentHp(), 0);
                    int mp = Math.max(lifeStats.getCurrentMp(), 0);
                    int fp = Math.max(lifeStats.getCurrentFp(), 0);

                    stmt.setInt(1, hp);
                    stmt.setInt(2, mp);
                    stmt.setInt(3, fp);
                    stmt.setInt(4, player.getObjectId());
                    stmt.addBatch();
                    batchCount++;

                    if (batchCount % 100 == 0) {
                        stmt.executeBatch();
                    }
                }

                if (batchCount % 100 != 0) {
                    stmt.executeBatch();
                }
            }

            con.commit();

        } catch (SQLException e) {
            log.error(I18n.get("log.29e5d532c9e9", e));
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
