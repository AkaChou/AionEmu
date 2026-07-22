package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.ranking.*;
import com.aionemu.gameserver.model.ranking.SeasonRankingResult;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * 赛季竞技排行数据访问对象的 MySQL 8 实现。
 * 覆盖黄金竞技场、挑战之塔、孤独竞技场、6v6 等 competition_ranking 表数据。
 * MySQL 8 implementation of SeasonRankingDAO.
 * Covers Gold Arena, Tower of Challenge, Arena of Tenacity, 6v6 and other competition ranking tables.
 *
 * @author Wnkrz
 */
@Slf4j
public class SeasonRankingDAO extends com.aionemu.gameserver.dao.SeasonRankingDAO {

    /** 查询排行榜前 300 名玩家 / Select top 300 competition ranking players */
    public static final String SELECT_PLAYERS_RANKING = "SELECT competition_ranking.rank, competition_ranking.last_rank, " + "competition_ranking.points, competition_ranking.player_id, players.name, " + "players.id, players.player_class, players.race FROM competition_ranking " + "INNER JOIN players ON competition_ranking.player_id = players.id " + "WHERE competition_ranking.table_id = ? AND competition_ranking.points > 0 " + "ORDER BY competition_ranking.points DESC LIMIT 300";

    /** 查询玩家某表排行记录 / Select a player's ranking row by table id */
    public static final String SELECT_MY_HISTORY = "SELECT * FROM competition_ranking WHERE player_id = ? AND table_id = ?";

    /** 插入排行记录 / Insert a competition ranking row */
    public static final String INSERT_QUERY = "INSERT INTO competition_ranking (player_id, table_id, rank, last_rank, " + "points, last_points, high_points, low_points, position_match) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /** 更新排行记录 / Update a competition ranking row */
    public static final String UPDATE_QUERY = "UPDATE competition_ranking SET rank = ?, last_rank = ?, points = ?, " + "last_points = ?, high_points = ?, low_points = ?, position_match = ? " + "WHERE player_id = ? AND table_id = ?";

    /**
     * 获取指定排行表的前 300 名竞技排行玩家。
     * Loads the top 300 competition ranking players for the given table id.
     *
     * ranking table id
     *
     * @param tableId
     * @return 排行结果列表 / ranking result list
     */
    @Override
    public ArrayList<SeasonRankingResult> getCompetitionRankingPlayers(int tableId) {
        ArrayList<SeasonRankingResult> results = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_PLAYERS_RANKING)) {

            stmt.setInt(1, tableId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    String name = resultSet.getString("players.name");
                    int rank = resultSet.getInt("competition_ranking.rank");
                    int last_rank = resultSet.getInt("competition_ranking.last_rank");
                    int points = resultSet.getInt("competition_ranking.points");
                    int playerId = resultSet.getInt("players.id");
                    String playerClassStr = resultSet.getString("players.player_class");

                    PlayerClass playerClass;
                    try {
                        playerClass = PlayerClass.getPlayerClassByString(playerClassStr);
                    } catch (IllegalArgumentException e) {
                        log.warn(I18n.get("log.9e67b184f360", playerClassStr));
                        continue;
                    }

                    String raceStr = resultSet.getString("players.race");
                    Race race = Race.getRaceByString(raceStr);

                    if (playerClass != null && race != null) {
                        SeasonRankingResult rsl = new SeasonRankingResult(name, last_rank, rank, points, playerClass, race.getRaceId(), playerId);
                        results.add(rsl);
                    }
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.2d8dc2963806", tableId, e), e);
        }

        return results;
    }

    /**
     * 加载玩家黄金竞技场排行数据；无记录时返回 NEW 状态的空对象。
     * Loads the player's Gold Arena rank; returns a NEW empty rank when none exists.
     *
     * player object id
     * ranking table id
     * @return 黄金竞技场排行 / Gold Arena rank
     */
    @Override
    public GoldArenaRank loadGoldArenaRank(int playerId, int tableId) {
        GoldArenaRank arenaRank = null;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_MY_HISTORY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, tableId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    int rank = resultSet.getInt("rank");
                    int best_rank = resultSet.getInt("last_rank");
                    int point = resultSet.getInt("points");
                    int last_point = resultSet.getInt("last_points");
                    int high_point = resultSet.getInt("high_points");
                    int low_point = resultSet.getInt("low_points");
                    int position_match = resultSet.getInt("position_match");

                    arenaRank = new GoldArenaRank(rank, best_rank, point, last_point, high_point, low_point, position_match);
                    arenaRank.setPersistentState(PersistentState.UPDATED);
                } else {
                    arenaRank = new GoldArenaRank(0, 0, 0, 0, 0, 0, 0);
                    arenaRank.setPersistentState(PersistentState.NEW);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.d516d865c7d9", playerId, tableId, e), e);
        }

        return arenaRank;
    }

    /**
     * 按持久化状态保存玩家黄金竞技场排行（table_id = 1）。
     * Stores the player's Gold Arena rank by persistent state (table_id = 1).
     *
     * @param player 玩家 / player
     * @return 是否保存成功；无排行对象时返回 false / whether store succeeded; false if rank is null
     */
    @Override
    public boolean storeGoldArenaRank(Player player) {
        GoldArenaRank rank = player.getArenaGoldRank();
        if (rank == null) {
            return false;
        }

        boolean result = false;

        switch (rank.getPersistentState()) {
            case NEW:
                result = addGoldRank(player.getObjectId(), rank);
                break;
            case UPDATE_REQUIRED:
                result = updateGoldRank(player.getObjectId(), rank);
                break;
            default:
                return true;
        }

        if (result) {
            rank.setPersistentState(PersistentState.UPDATED);
        }
        return result;
    }

    private boolean addGoldRank(final int objectId, final GoldArenaRank rank) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, objectId);
            stmt.setInt(2, 1);
            stmt.setInt(3, rank.getRank());
            stmt.setInt(4, rank.getBestRank());
            stmt.setInt(5, rank.getPoints());
            stmt.setInt(6, rank.getLastPoints());
            stmt.setInt(7, rank.getHighPoints());
            stmt.setInt(8, rank.getLowPoints());
            stmt.setInt(9, rank.getPossitionMatch());
            stmt.executeUpdate();

            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.892315d315f0", objectId, e), e);
            return false;
        }
    }

    private boolean updateGoldRank(final int objectId, GoldArenaRank rank) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setInt(1, rank.getRank());
            stmt.setInt(2, rank.getBestRank());
            stmt.setInt(3, rank.getPoints());
            stmt.setInt(4, rank.getLastPoints());
            stmt.setInt(5, rank.getHighPoints());
            stmt.setInt(6, rank.getLowPoints());
            stmt.setInt(7, rank.getPossitionMatch());
            stmt.setInt(8, objectId);
            stmt.setInt(9, 1);
            stmt.executeUpdate();

            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.8025a55d13a2", objectId, e), e);
            return false;
        }
    }

    /**
     * 按持久化状态保存玩家挑战之塔排行（table_id = 2）。
     * Stores the player's Tower of Challenge rank by persistent state (table_id = 2).
     *
     * @param player 玩家 / player
     * @return 是否保存成功；无排行对象时返回 false / whether store succeeded; false if rank is null
     */
    @Override
    public boolean storeTowerRank(Player player) {
        TowerOfChallengeRank rank = player.getTowerRank();
        if (rank == null) {
            return false;
        }

        boolean result = false;

        switch (rank.getPersistentState()) {
            case NEW:
                result = addTowerRank(player.getObjectId(), rank);
                break;
            case UPDATE_REQUIRED:
                result = updateTowerRank(player.getObjectId(), rank);
                break;
            default:
                return true;
        }

        if (result) {
            rank.setPersistentState(PersistentState.UPDATED);
        }
        return result;
    }

    private boolean addTowerRank(final int objectId, final TowerOfChallengeRank rank) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, objectId);
            stmt.setInt(2, 2);
            stmt.setInt(3, rank.getRank());
            stmt.setInt(4, rank.getBestRank());
            stmt.setInt(5, rank.getCurrentTime());
            stmt.setInt(6, rank.getLastTime());
            stmt.setInt(7, rank.getBestTime());
            stmt.setInt(8, rank.getLowRank());
            stmt.setInt(9, 0);
            stmt.executeUpdate();

            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.33e0d184fd35", objectId, e), e);
            return false;
        }
    }

    private boolean updateTowerRank(final int objectId, TowerOfChallengeRank rank) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setInt(1, rank.getRank());
            stmt.setInt(2, rank.getBestRank());
            stmt.setInt(3, rank.getCurrentTime());
            stmt.setInt(4, rank.getLastTime());
            stmt.setInt(5, rank.getBestTime());
            stmt.setInt(6, rank.getLowRank());
            stmt.setInt(7, 0);
            stmt.setInt(8, objectId);
            stmt.setInt(9, 2);
            stmt.executeUpdate();

            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.525c0a41a0d5", objectId, e), e);
            return false;
        }
    }

    /**
     * 按持久化状态保存玩家孤独竞技场排行（table_id = 541）。
     * Stores the player's Arena of Tenacity rank by persistent state (table_id = 541).
     *
     * @param player 玩家 / player
     * @return 是否保存成功；无排行对象时返回 false / whether store succeeded; false if rank is null
     */
    @Override
    public boolean storeTenacityRank(Player player) {
        ArenaOfTenacityRank rank = player.getTenacityRank();
        if (rank == null) {
            return false;
        }

        boolean result = false;

        switch (rank.getPersistentState()) {
            case NEW:
                result = addTenacityRank(player.getObjectId(), rank);
                break;
            case UPDATE_REQUIRED:
                result = updateTenacityRank(player.getObjectId(), rank);
                break;
            default:
                return true;
        }

        if (result) {
            rank.setPersistentState(PersistentState.UPDATED);
        }
        return result;
    }

    private boolean addTenacityRank(final int objectId, final ArenaOfTenacityRank rank) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, objectId);
            stmt.setInt(2, 541);
            stmt.setInt(3, rank.getRank());
            stmt.setInt(4, rank.getBestRank());
            stmt.setInt(5, rank.getPoints());
            stmt.setInt(6, rank.getLastPoints());
            stmt.setInt(7, rank.getHighPoints());
            stmt.setInt(8, rank.getLowPoints());
            stmt.setInt(9, rank.getPossitionMatch());
            stmt.executeUpdate();

            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.1833d5239851", objectId, e), e);
            return false;
        }
    }

    private boolean updateTenacityRank(final int objectId, ArenaOfTenacityRank rank) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setInt(1, rank.getRank());
            stmt.setInt(2, rank.getBestRank());
            stmt.setInt(3, rank.getPoints());
            stmt.setInt(4, rank.getLastPoints());
            stmt.setInt(5, rank.getHighPoints());
            stmt.setInt(6, rank.getLowPoints());
            stmt.setInt(7, rank.getPossitionMatch());
            stmt.setInt(8, objectId);
            stmt.setInt(9, 541);
            stmt.executeUpdate();

            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.b547e9af94b6", objectId, e), e);
            return false;
        }
    }

    /**
     * 按持久化状态保存玩家 6v6 竞技场排行（table_id = 3）。
     * Stores the player's 6v6 Arena rank by persistent state (table_id = 3).
     *
     * @param player 玩家 / player
     * @return 是否保存成功；无排行对象时返回 false / whether store succeeded; false if rank is null
     */
    @Override
    public boolean store6v6Rank(Player player) {
        Arena6V6Ranking rank = player.get6v6Rank();
        if (rank == null) {
            return false;
        }

        boolean result = false;

        switch (rank.getPersistentState()) {
            case NEW:
                result = add6v6Rank(player.getObjectId(), rank);
                break;
            case UPDATE_REQUIRED:
                result = update6v6Rank(player.getObjectId(), rank);
                break;
            default:
                return true;
        }

        if (result) {
            rank.setPersistentState(PersistentState.UPDATED);
        }
        return result;
    }

    private boolean add6v6Rank(final int objectId, final Arena6V6Ranking rank) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, objectId);
            stmt.setInt(2, 3);
            stmt.setInt(3, rank.getRank());
            stmt.setInt(4, rank.getBestRank());
            stmt.setInt(5, rank.getPoints());
            stmt.setInt(6, rank.getLastPoints());
            stmt.setInt(7, rank.getHighPoints());
            stmt.setInt(8, rank.getLowPoints());
            stmt.setInt(9, rank.getPossitionMatch());
            stmt.executeUpdate();

            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.688e68aa0f83", objectId, e), e);
            return false;
        }
    }

    private boolean update6v6Rank(final int objectId, Arena6V6Ranking rank) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setInt(1, rank.getRank());
            stmt.setInt(2, rank.getBestRank());
            stmt.setInt(3, rank.getPoints());
            stmt.setInt(4, rank.getLastPoints());
            stmt.setInt(5, rank.getHighPoints());
            stmt.setInt(6, rank.getLowPoints());
            stmt.setInt(7, rank.getPossitionMatch());
            stmt.setInt(8, objectId);
            stmt.setInt(9, 3);
            stmt.executeUpdate();

            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.2027d7f241eb", objectId, e), e);
            return false;
        }
    }

    /**
     * 加载玩家孤独竞技场排行数据；无记录时返回 NEW 状态的空对象。
     * Loads the player's Arena of Tenacity rank; returns a NEW empty rank when none exists.
     *
     * player object id
     * ranking table id
     * @return 孤独竞技场排行 / Arena of Tenacity rank
     */
    @Override
    public ArenaOfTenacityRank loadArenaOfTenacityRank(int playerId, int tableId) {
        ArenaOfTenacityRank ranking = null;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_MY_HISTORY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, tableId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    int rank = resultSet.getInt("rank");
                    int best_rank = resultSet.getInt("last_rank");
                    int point = resultSet.getInt("points");
                    int last_point = resultSet.getInt("last_points");
                    int high_point = resultSet.getInt("high_points");
                    int low_point = resultSet.getInt("low_points");
                    int position_match = resultSet.getInt("position_match");

                    ranking = new ArenaOfTenacityRank(rank, best_rank, point, last_point, high_point, low_point, position_match);
                    ranking.setPersistentState(PersistentState.UPDATED);
                } else {
                    ranking = new ArenaOfTenacityRank(0, 0, 0, 0, 0, 0, 0);
                    ranking.setPersistentState(PersistentState.NEW);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.1c6ce1dd5b7e", playerId, tableId, e), e);
        }

        return ranking;
    }

    /**
     * 加载玩家挑战之塔排行数据；无记录时返回 NEW 状态的空对象。
     * Loads the player's Tower of Challenge rank; returns a NEW empty rank when none exists.
     *
     * player object id
     * ranking table id
     * @return 挑战之塔排行 / Tower of Challenge rank
     */
    @Override
    public TowerOfChallengeRank loadTowerOfChallengeRank(int playerId, int tableId) {
        TowerOfChallengeRank ranking = null;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_MY_HISTORY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, tableId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    int rank = resultSet.getInt("rank");
                    int best_rank = resultSet.getInt("last_rank");
                    int point = resultSet.getInt("points");
                    int last_point = resultSet.getInt("last_points");
                    int high_point = resultSet.getInt("high_points");
                    int low_point = resultSet.getInt("low_points");

                    ranking = new TowerOfChallengeRank(rank, best_rank, point, last_point, high_point, low_point);
                    ranking.setPersistentState(PersistentState.UPDATED);
                } else {
                    ranking = new TowerOfChallengeRank(0, 0, 0, 0, 0, 0);
                    ranking.setPersistentState(PersistentState.NEW);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.a937504b345f", playerId, tableId, e), e);
        }

        return ranking;
    }

    /**
     * 加载玩家 6v6 竞技场排行数据；无记录时返回 NEW 状态的空对象。
     * Loads the player's 6v6 Arena rank; returns a NEW empty rank when none exists.
     *
     * player object id
     * ranking table id
     * @return 6v6 竞技场排行 / 6v6 Arena rank
     */
    @Override
    public Arena6V6Ranking loadArena6v6Rank(int playerId, int tableId) {
        Arena6V6Ranking ranking = null;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_MY_HISTORY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, tableId);

            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    int rank = resultSet.getInt("rank");
                    int best_rank = resultSet.getInt("last_rank");
                    int point = resultSet.getInt("points");
                    int last_point = resultSet.getInt("last_points");
                    int high_point = resultSet.getInt("high_points");
                    int low_point = resultSet.getInt("low_points");
                    int position_match = resultSet.getInt("position_match");

                    ranking = new Arena6V6Ranking(rank, best_rank, point, last_point, high_point, low_point, position_match);
                    ranking.setPersistentState(PersistentState.UPDATED);
                } else {
                    ranking = new Arena6V6Ranking(0, 0, 0, 0, 0, 0, 0);
                    ranking.setPersistentState(PersistentState.NEW);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.a8b4326794d1", playerId, tableId, e), e);
        }

        return ranking;
    }

    /**
     * 判断当前数据库是否受本 DAO 支持（MySQL 8）。
     * Checks whether the given database is supported by this DAO (MySQL 8).
     *
     * @param databaseName 数据库产品名 / database product name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}