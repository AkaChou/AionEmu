package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.LadderDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 天梯排行榜数据访问对象的 MySQL 8 实现，已修复连接泄漏。
 * MySQL 8 implementation of LadderDAO with connection leak fixes.
 *
 * @author wanke
 */
@Slf4j
public class MySQL8LadderDAO extends LadderDAO {


    /** 查询有战绩玩家的天梯数据 / Select ladder data for players with records */
    private static final String SELECT_PLAYER_DATA = "SELECT player_id, last_update, rating, wins, rank FROM ladder_player " + "WHERE wins > 0 OR losses > 0 OR leaves > 0 ORDER BY rating DESC, wins DESC, player_id ASC";

    /** 更新玩家当前排名 / Update player current rank */
    private static final String UPDATE_RANK = "UPDATE ladder_player SET rank = ? WHERE player_id = ?";

    /** 更新玩家上次排名与时间 / Update player last rank and timestamp */
    private static final String UPDATE_LAST_RANK = "UPDATE ladder_player SET last_rank = ?, last_update = ? WHERE player_id = ?";

    /** 检查玩家天梯记录是否存在 / Check whether a player ladder row exists */
    private static final String SELECT_CHECK_EXISTS = "SELECT 1 FROM ladder_player WHERE player_id = ? LIMIT 1";

    /** 按列查询玩家天梯字段（占位模板） / Select a ladder column by player (placeholder template) */
    private static final String SELECT_GET_DATA = "SELECT ? FROM ladder_player WHERE player_id = ?";

    /** 查询玩家全部天梯数据 / Select all ladder data for a player */
    private static final String SELECT_GET_ALL = "SELECT * FROM ladder_player WHERE player_id = ?";

    /** 累加天梯字段（占位模板） / Increment a ladder column (placeholder template) */
    private static final String UPDATE_ADD_DATA = "UPDATE ladder_player SET ? = ? + ? WHERE player_id = ?";

    /** 设置天梯字段（占位模板） / Set a ladder column (placeholder template) */
    private static final String UPDATE_SET_DATA = "UPDATE ladder_player SET ? = ? WHERE player_id = ?";

    /** 插入玩家天梯记录（占位模板） / Insert player ladder row (placeholder template) */
    private static final String INSERT_PLAYER = "INSERT INTO ladder_player (player_id, ?) VALUES (?, ?)";

    /** 查询玩家上次更新时间 / Select player last update timestamp */
    private static final String SELECT_LAST_UPDATE = "SELECT last_update FROM ladder_player WHERE player_id = ?";

    /** 设置玩家上次更新时间 / Update player last update timestamp */
    private static final String UPDATE_LAST_UPDATE = "UPDATE ladder_player SET last_update = ? WHERE player_id = ?";

    /**
     * 增加玩家天梯胜利场次。
     * Adds a ladder win for the player.
     *
     * @param player 玩家 / player
     */
    @Override
    public void addWin(Player player) {
        addPlayerLadderData(player, "wins", 1);
    }

    /**
     * 增加玩家天梯失败场次。
     * Adds a ladder loss for the player.
     *
     * @param player 玩家 / player
     */
    @Override
    public void addLoss(Player player) {
        addPlayerLadderData(player, "losses", 1);
    }

    /**
     * 增加玩家天梯离开（逃跑）场次。
     * Adds a ladder leave for the player.
     *
     * @param player 玩家 / player
     */
    @Override
    public void addLeave(Player player) {
        addPlayerLadderData(player, "leaves", 1);
    }

    /**
     * 增加玩家天梯评分。
     * Adds rating points to the player's ladder score.
     *
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
     *
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
     *
     * 玩家 / player
     * loss count
     */
    @Override
    public void setLosses(Player player, int losses) {
        setPlayerLadderData(player, "losses", losses);
    }

    /**
     * 设置玩家天梯离开场次。
     * Sets the player's ladder leave count.
     *
     * 玩家 / player
     * leave count
     */
    @Override
    public void setLeaves(Player player, int leaves) {
        setPlayerLadderData(player, "leaves", leaves);
    }

    /**
     * 设置玩家天梯评分。
     * Sets the player's ladder rating.
     *
     * 玩家 / player
     * rating value
     */
    @Override
    public void setRating(Player player, int rating) {
        setPlayerLadderData(player, "rating", rating);
    }

    /**
     * 获取玩家天梯胜利场次。
     * Returns the player's ladder win count.
     *
     * 玩家 / player
     * win count
     */
    @Override
    public int getWins(Player player) {
        return getPlayerLadderData(player, "wins");
    }

    /**
     * 获取玩家天梯失败场次。
     * Returns the player's ladder loss count.
     *
     * 玩家 / player
     * loss count
     */
    @Override
    public int getLosses(Player player) {
        return getPlayerLadderData(player, "losses");
    }

    /**
     * 获取玩家天梯离开场次。
     * Returns the player's ladder leave count.
     *
     * 玩家 / player
     * leave count
     */
    @Override
    public int getLeaves(Player player) {
        return getPlayerLadderData(player, "leaves");
    }

    /**
     * 获取玩家天梯评分；无记录时默认 1000。
     * Returns the player's ladder rating; defaults to 1000 when unset.
     *
     * 玩家 / player
     * rating value
     */
    @Override
    public int getRating(Player player) {
        int rating = getPlayerLadderData(player, "rating");
        return rating == 0 ? 1000 : rating;
    }

    /**
     * 获取玩家当前天梯排名。
     * Returns the player's current ladder rank.
     *
     * 玩家 / player
     * rank position
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

        // 加载全部玩家 / Load all players
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_PLAYER_DATA);
             ResultSet rset = stmt.executeQuery()) {

            while (rset.next()) {
                PlayerInfo plInfo = new PlayerInfo(
                    rset.getInt("player_id"),
                    rset.getInt("rating"),
                    rset.getTimestamp("last_update"),
                    rset.getInt("wins"),
                    rset.getInt("rank")
                );
                players.add(plInfo);
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.9a75439aa03e", e));
            return;
        }

        // 排序玩家 / Sort players
        Collections.sort(players, (o1, o2) -> {
            int result = Integer.compare(o2.getRating(), o1.getRating());
            if (result != 0) return result;
            result = Integer.compare(o2.getWins(), o1.getWins());
            if (result != 0) return result;
            return Integer.compare(o1.getPlayerId(), o2.getPlayerId());
        });

        if (players.isEmpty()) {
            return;
        }

        // 更新军阶 / Update ranks
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement stmtRank = con.prepareStatement(UPDATE_RANK);
                 PreparedStatement stmtLast = con.prepareStatement(UPDATE_LAST_RANK)) {

                int i = 1;
                for (PlayerInfo plInfo : players) {
                    int playerId = plInfo.getPlayerId();
                    Timestamp update = plInfo.getLastUpdate();

                    if (update == null || update.getTime() == 0 ||
                        (System.currentTimeMillis() - update.getTime()) > (24 * 60 * 60 * 1000)) {
                        stmtLast.setInt(1, plInfo.getRank());
                        stmtLast.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                        stmtLast.setInt(3, playerId);
                        stmtLast.addBatch();
                    }

                    stmtRank.setInt(1, i);
                    stmtRank.setInt(2, playerId);
                    stmtRank.addBatch();
                    i++;
                }

                stmtRank.executeBatch();
                stmtLast.executeBatch();
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                log.error(I18n.get("log.dbf61ab14760", e));
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.7f7d50101121", e));
        }
    }

    /**
     * 累加玩家天梯指定列的值；不存在则插入。
     * Increments a ladder column for the player, inserting a row when missing.
     *
     * 玩家 / player
     * column name
     * value to add
     */
    private void addPlayerLadderData(Player player, String column, int value) {
        int playerId = player.getObjectId();

        try (Connection con = DatabaseFactory.getConnection()) {
            if (checkExists(playerId)) {
                String query = "UPDATE ladder_player SET " + column + " = " + column + " + ? WHERE player_id = ?";
                try (PreparedStatement stmt = con.prepareStatement(query)) {
                    stmt.setInt(1, value);
                    stmt.setInt(2, playerId);
                    stmt.executeUpdate();
                }
            } else {
                String query = "INSERT INTO ladder_player (player_id, " + column + ") VALUES (?, ?)";
                try (PreparedStatement stmt = con.prepareStatement(query)) {
                    stmt.setInt(1, playerId);
                    stmt.setInt(2, "rating".equals(column) ? 1000 + value : value);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.58ec4d70c9e9", player.getObjectId(), e));
        }
    }

    /**
     * 设置玩家天梯指定列的值；不存在则插入。
     * Sets a ladder column for the player, inserting a row when missing.
     *
     * 玩家 / player
     * column name
     * value to set
     */
    private void setPlayerLadderData(Player player, String column, int value) {
        int playerId = player.getObjectId();

        try (Connection con = DatabaseFactory.getConnection()) {
            if (checkExists(playerId)) {
                String query = "UPDATE ladder_player SET " + column + " = ? WHERE player_id = ?";
                try (PreparedStatement stmt = con.prepareStatement(query)) {
                    stmt.setInt(1, value);
                    stmt.setInt(2, playerId);
                    stmt.executeUpdate();
                }
            } else {
                String query = "INSERT INTO ladder_player (player_id, " + column + ") VALUES (?, ?)";
                try (PreparedStatement stmt = con.prepareStatement(query)) {
                    stmt.setInt(1, playerId);
                    stmt.setInt(2, value);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.4ea6502ae269", player.getObjectId(), e));
        }
    }

    /**
     * 按玩家 ID 设置天梯指定列的值（仅更新）。
     * Sets a ladder column by player id (update only).
     *
     * player id
     * column name
     * value to set
     */
    public void setPlayerLadderData(Integer playerId, String column, int value) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement("UPDATE ladder_player SET " + column + " = ? WHERE player_id = ?")) {

            stmt.setInt(1, value);
            stmt.setInt(2, playerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.181a09080739", playerId, e));
        }
    }

    /**
     * 读取玩家天梯指定列的整数值。
     * Reads an integer ladder column for the player.
     *
     * 玩家 / player
     * column name
     * @return 列值，不存在则为 0 / column value, or 0 if missing
     */
    private int getPlayerLadderData(Player player, String column) {
        int playerId = player.getObjectId();
        int value = 0;

        String query = "SELECT " + column + " FROM ladder_player WHERE player_id = ?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setInt(1, playerId);
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    value = rset.getInt(column);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.960d9c9f47e1", player.getObjectId(), e));
        }

        return value;
    }

    /**
     * 按玩家 ID 读取天梯指定列的整数值。
     * Reads an integer ladder column by player id.
     *
     * player id
     * column name
     * @return 列值，不存在则为 0 / column value, or 0 if missing
     */
    public int getPlayerLadderData(Integer playerId, String column) {
        int value = 0;

        String query = "SELECT " + column + " FROM ladder_player WHERE player_id = ?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setInt(1, playerId);
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    value = rset.getInt(column);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.29e72ebe993e", playerId, e));
        }

        return value;
    }

    /**
     * 获取玩家天梯上次更新时间。
     * Returns the player's ladder last-update timestamp.
     *
     * @param player 玩家 / player
     * @return 上次更新时间，不存在则为 null / last update time, or null if missing
     */
    public Timestamp getPlayerLadderUpdate(Player player) {
        int playerId = player.getObjectId();
        Timestamp value = null;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LAST_UPDATE)) {

            stmt.setInt(1, playerId);
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    value = rset.getTimestamp("last_update");
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.faf0c73a8eb8", player.getObjectId(), e));
        }
        return value;
    }

    /**
     * 设置玩家天梯上次更新时间。
     * Sets the player's ladder last-update timestamp.
     *
     * @param player 玩家 / player
     * @param value 更新时间 / update timestamp
     */
    public void setPlayerLadderUpdate(Player player, Timestamp value) {
        int playerId = player.getObjectId();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_LAST_UPDATE)) {

            stmt.setTimestamp(1, value);
            stmt.setInt(2, playerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.052014ac9f3c", player.getObjectId(), e));
        }
    }

    /**
     * 按玩家 ID 设置天梯上次更新时间。
     * Sets the ladder last-update timestamp by player id.
     *
     * player id
     * @param value 更新时间 / update timestamp
     */
    public void setPlayerLadderUpdate(Integer playerId, Timestamp value) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_LAST_UPDATE)) {

            stmt.setTimestamp(1, value);
            stmt.setInt(2, playerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.461b6c4b9a36", playerId, e));
        }
    }

    /**
     * 加载玩家完整天梯数据；无记录时返回默认值。
     * Loads full ladder data for the player; returns defaults when missing.
     *
     * 玩家 / player
     * ladder data
     */
    public PlayerLadderData getPlayerLadderData(Player player) {
        int playerId = player.getObjectId();
        PlayerLadderData data = null;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_GET_ALL)) {

            stmt.setInt(1, playerId);
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    data = new PlayerLadderData(
                        player,
                        rset.getInt("rating"),
                        rset.getInt("rank"),
                        rset.getInt("wins"),
                        rset.getInt("losses"),
                        rset.getInt("leaves"),
                        rset.getTimestamp("last_update")
                    );
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.af5e85661cbb", player.getObjectId(), e));
        }

        if (data == null) {
            data = new PlayerLadderData(player, 1000, 0, 0, 0, 0, new Timestamp(0));
        }
        return data;
    }

    /**
     * 检查玩家是否已有天梯记录。
     * Checks whether a ladder row exists for the player.
     *
     * player id
     *
     * @param playerId 若 the row exists 则为 true / true if the row exists
     */
    private boolean checkExists(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_CHECK_EXISTS)) {

            stmt.setInt(1, playerId);
            try (ResultSet rset = stmt.executeQuery()) {
                return rset.next();
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.c59d3338d886", playerId, e));
            return false;
        }
    }

    /**
     * 是否支持该数据库。
     * Whether the database is supported.
     *
     * @param databaseName 数据库名称 / database name
     * major version
     * minor version
     *
     * @return 若 supported 则为 true / true if supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return databaseName.toLowerCase().contains("mysql") && majorVersion >= 8;
    }

    /**
     * 排名重算用的玩家临时信息。
     * Temporary player info used while recalculating ranks.
     */
    private static class PlayerInfo {
        private final int playerId;
        private final int rating;
        private final Timestamp lastUpdate;
        private final int wins;
        private final int rank;

        /**
         * player id
         * rating
         * @param lastUpdate 上次更新时间 / last update time
         * wins
         * @param rank 当前名次 / current rank
         */
        public PlayerInfo(int playerId, int rating, Timestamp lastUpdate, int wins, int rank) {
            this.playerId = playerId;
            this.rating = rating;
            this.lastUpdate = lastUpdate;
            this.wins = wins;
            this.rank = rank;
        }

 /** 玩家 ID / player id */
        public int getPlayerId() { return playerId; }
        /** 评分 / rating */
        public int getRating() { return rating; }
        /** 上次更新时间 / last update time */
        public Timestamp getLastUpdate() { return lastUpdate; }
        /** 胜场 / wins */
        public int getWins() { return wins; }
        /** 当前排名 / current rank */
        public int getRank() { return rank; }
    }
}
