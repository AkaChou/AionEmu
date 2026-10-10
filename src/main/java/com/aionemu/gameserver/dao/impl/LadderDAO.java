package com.aionemu.gameserver.dao.impl;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.Player;

import lombok.extern.slf4j.Slf4j;

/**
 * 天梯排行榜数据访问对象的 MySQL 8 实现。
 * MySQL 8 implementation of LadderDAO using JdbcClient.
 * @author wanke
 */
@Slf4j
public class LadderDAO extends com.aionemu.gameserver.dao.LadderDAO {

    /** 查询有战绩玩家的天梯数据 / Select ladder data for players with records */
    private static final String SELECT_PLAYER_DATA = "SELECT player_id, last_update, rating, wins, rank FROM ladder_player " + "WHERE wins > 0 OR losses > 0 OR leaves > 0 ORDER BY rating DESC, wins DESC, player_id ASC";

    /** 更新玩家当前排名 / Update player current rank */
    private static final String UPDATE_RANK = "UPDATE ladder_player SET rank = ? WHERE player_id = ?";

    /** 更新玩家上次排名与时间 / Update player last rank and timestamp */
    private static final String UPDATE_LAST_RANK = "UPDATE ladder_player SET last_rank = ?, last_update = ? WHERE player_id = ?";

    /** 查询玩家全部天梯数据 / Select all ladder data for a player */
    private static final String SELECT_GET_ALL = "SELECT * FROM ladder_player WHERE player_id = ?";

    /** 查询玩家上次更新时间 / Select player last update timestamp */
    private static final String SELECT_LAST_UPDATE = "SELECT last_update FROM ladder_player WHERE player_id = ?";

    /** 设置玩家上次更新时间 / Update player last update timestamp */
    private static final String UPDATE_LAST_UPDATE = "UPDATE ladder_player SET last_update = ? WHERE player_id = ?";

    private final JdbcClient jdbcClient;

    public LadderDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    public LadderDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 增加玩家天梯胜利场次。
     * Adds a ladder win for the player.
     * @param player 玩家 / player
     */
    @Override
    public void addWin(Player player) {
        addPlayerLadderData(player, "wins", 1);
    }

    /**
     * 增加玩家天梯失败场次。
     * Adds a ladder loss for the player.
     * @param player 玩家 / player
     */
    @Override
    public void addLoss(Player player) {
        addPlayerLadderData(player, "losses", 1);
    }

    /**
     * 增加玩家天梯离开（逃跑）场次。
     * Adds a ladder leave for the player.
     * @param player 玩家 / player
     */
    @Override
    public void addLeave(Player player) {
        addPlayerLadderData(player, "leaves", 1);
    }

    /**
     * 增加玩家天梯评分。
     * Adds rating points to the player's ladder score.
     * @param player 玩家 / player
     * @param rating 增加的评分 / rating delta to add
     */
    @Override
    public void addRating(Player player, int rating) {
        addPlayerLadderData(player, "rating", rating);
    }

    /**
     * 设置玩家天梯胜利场次。
     * Sets the player's ladder win count.
     * @param player 玩家 / player
     * @param wins 胜利场次 / win count
     */
    @Override
    public void setWins(Player player, int wins) {
        setPlayerLadderData(player, "wins", wins);
    }

    /**
     * 设置玩家天梯失败场次。
     * Sets the player's ladder loss count.
     * @param player 玩家 / player
     * @param losses 失败次数 / loss count
     */
    @Override
    public void setLosses(Player player, int losses) {
        setPlayerLadderData(player, "losses", losses);
    }

    /**
     * 设置玩家天梯离开场次。
     * Sets the player's ladder leave count.
     * @param player 玩家 / player
     * @param leaves 离开次数 / leave count
     */
    @Override
    public void setLeaves(Player player, int leaves) {
        setPlayerLadderData(player, "leaves", leaves);
    }

    /**
     * 设置玩家天梯评分。
     * Sets the player's ladder rating.
     * @param player 玩家 / player
     * @param rating 评级值 / rating value
     */
    @Override
    public void setRating(Player player, int rating) {
        setPlayerLadderData(player, "rating", rating);
    }

    /**
     * 获取玩家天梯胜利场次。
     * Returns the player's ladder win count.
     * @param player 玩家 / player
     * @return 胜利次数 / win count
     */
    @Override
    public int getWins(Player player) {
        return getPlayerLadderData(player, "wins");
    }

    /**
     * 获取玩家天梯失败场次。
     * Returns the player's ladder loss count.
     * @param player 玩家 / player
     * @return 失败次数 / loss count
     */
    @Override
    public int getLosses(Player player) {
        return getPlayerLadderData(player, "losses");
    }

    /**
     * 获取玩家天梯离开场次。
     * Returns the player's ladder leave count.
     * @param player 玩家 / player
     * @return 离开次数 / leave count
     */
    @Override
    public int getLeaves(Player player) {
        return getPlayerLadderData(player, "leaves");
    }

    /**
     * 获取玩家天梯评分；无记录时默认 1000。
     * Returns the player's ladder rating; defaults to 1000 when unset.
     * @param player 玩家 / player
     * @return 评级值 / rating value
     */
    @Override
    public int getRating(Player player) {
        int rating = getPlayerLadderData(player, "rating");
        return rating == 0 ? 1000 : rating;
    }

    /**
     * 获取玩家当前天梯排名。
     * Returns the player's current ladder rank.
     * @param player 玩家 / player
     * @return 排名位置 / rank position
     */
    @Override
    public int getRank(Player player) {
        return getPlayerLadderData(player, "rank");
    }

    /**
     * 重新计算并更新全部玩家的天梯排名。
     * Recalculates and updates ladder ranks for all players with records.
     */
    @Override
    public void updateRanks() {
        List<PlayerInfo> players = new ArrayList<>();

        try {
            jdbcClient.sql(SELECT_PLAYER_DATA)
                .query((rset, rowNum) -> new PlayerInfo(
                    rset.getInt("player_id"),
                    rset.getInt("rating"),
                    rset.getTimestamp("last_update"),
                    rset.getInt("wins"),
                    rset.getInt("rank")
                ))
                .list()
                .forEach(players::add);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.9a75439aa03e"), e);
            return;
        }

        Collections.sort(players, (o1, o2) -> {
            int result = Integer.compare(o2.rating(), o1.rating());
            if (result != 0) return result;
            result = Integer.compare(o2.wins(), o1.wins());
            if (result != 0) return result;
            return Integer.compare(o1.playerId(), o2.playerId());
        });

        if (players.isEmpty()) {
            return;
        }

        try {
            int i = 1;
            for (PlayerInfo plInfo : players) {
                int playerId = plInfo.playerId();
                Timestamp update = plInfo.lastUpdate();

                if (update == null || update.getTime() == 0 ||
                    (System.currentTimeMillis() - update.getTime()) > (24 * 60 * 60 * 1000)) {
                    jdbcClient.sql(UPDATE_LAST_RANK)
                        .param(plInfo.rank())
                        .param(new Timestamp(System.currentTimeMillis()))
                        .param(playerId)
                        .update();
                }

                jdbcClient.sql(UPDATE_RANK)
                    .param(i)
                    .param(playerId)
                    .update();
                i++;
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.7f7d50101121"), e);
        }
    }

    private void addPlayerLadderData(Player player, String column, int value) {
        int playerId = player.getObjectId();
        String query = "INSERT INTO ladder_player (player_id, " + column + ") VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE " + column + " = " + column + " + ?";
        try {
            jdbcClient.sql(query)
                .param(playerId)
                .param("rating".equals(column) ? 1000 + value : value)
                .param(value)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.58ec4d70c9e9", player.getObjectId()), e);
        }
    }

    private void setPlayerLadderData(Player player, String column, int value) {
        int playerId = player.getObjectId();
        String query = "INSERT INTO ladder_player (player_id, " + column + ") VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE " + column + " = VALUES(" + column + ")";
        try {
            jdbcClient.sql(query)
                .param(playerId)
                .param(value)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.4ea6502ae269", player.getObjectId()), e);
        }
    }

    public void setPlayerLadderData(Integer playerId, String column, int value) {
        try {
            jdbcClient.sql("UPDATE ladder_player SET " + column + " = ? WHERE player_id = ?")
                .param(value)
                .param(playerId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.181a09080739", playerId), e);
        }
    }

    private int getPlayerLadderData(Player player, String column) {
        int playerId = player.getObjectId();
        String query = "SELECT " + column + " FROM ladder_player WHERE player_id = ?";
        try {
            Integer val = jdbcClient.sql(query)
                .param(playerId)
                .query(Integer.class)
                .optional()
                .orElse(0);
            return val != null ? val : 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.960d9c9f47e1", player.getObjectId()), e);
            return 0;
        }
    }

    public int getPlayerLadderData(Integer playerId, String column) {
        String query = "SELECT " + column + " FROM ladder_player WHERE player_id = ?";
        try {
            Integer val = jdbcClient.sql(query)
                .param(playerId)
                .query(Integer.class)
                .optional()
                .orElse(0);
            return val != null ? val : 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.29e72ebe993e", playerId), e);
            return 0;
        }
    }

    public Timestamp getPlayerLadderUpdate(Player player) {
        try {
            return jdbcClient.sql(SELECT_LAST_UPDATE)
                .param(player.getObjectId())
                .query(Timestamp.class)
                .optional()
                .orElse(null);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.faf0c73a8eb8", player.getObjectId()), e);
            return null;
        }
    }

    public void setPlayerLadderUpdate(Player player, Timestamp value) {
        try {
            jdbcClient.sql(UPDATE_LAST_UPDATE)
                .param(value)
                .param(player.getObjectId())
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.052014ac9f3c", player.getObjectId()), e);
        }
    }

    public void setPlayerLadderUpdate(Integer playerId, Timestamp value) {
        try {
            jdbcClient.sql(UPDATE_LAST_UPDATE)
                .param(value)
                .param(playerId)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.461b6c4b9a36", playerId), e);
        }
    }

    public PlayerLadderData getPlayerLadderData(Player player) {
        try {
            PlayerLadderData data = jdbcClient.sql(SELECT_GET_ALL)
                .param(player.getObjectId())
                .query((rset, rowNum) -> new PlayerLadderData(
                    player,
                    rset.getInt("rating"),
                    rset.getInt("rank"),
                    rset.getInt("wins"),
                    rset.getInt("losses"),
                    rset.getInt("leaves"),
                    rset.getTimestamp("last_update")
                ))
                .optional()
                .orElse(null);

            return data != null ? data : new PlayerLadderData(player, 1000, 0, 0, 0, 0, new Timestamp(0));
        } catch (DataAccessException e) {
            log.error(I18n.get("log.af5e85661cbb", player.getObjectId()), e);
            return new PlayerLadderData(player, 1000, 0, 0, 0, 0, new Timestamp(0));
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return databaseName.toLowerCase().contains("mysql") && majorVersion >= 8;
    }

    private record PlayerInfo(int playerId, int rating, Timestamp lastUpdate, int wins, int rank) {}
}
