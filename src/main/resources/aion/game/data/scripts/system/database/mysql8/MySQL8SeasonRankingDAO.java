package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.SeasonRankingDAO;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.ranking.*;
import com.aionemu.gameserver.model.ranking.SeasonRankingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Created by Wnkrz on 24/07/2017.
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8SeasonRankingDAO extends SeasonRankingDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8SeasonRankingDAO.class);

    public static final String SELECT_PLAYERS_RANKING = "SELECT competition_ranking.rank, competition_ranking.last_rank, " + "competition_ranking.points, competition_ranking.player_id, players.name, " + "players.id, players.player_class, players.race FROM competition_ranking " + "INNER JOIN players ON competition_ranking.player_id = players.id " + "WHERE competition_ranking.table_id = ? AND competition_ranking.points > 0 " + "ORDER BY competition_ranking.points DESC LIMIT 300";

    public static final String SELECT_MY_HISTORY = "SELECT * FROM competition_ranking WHERE player_id = ? AND table_id = ?";

    public static final String INSERT_QUERY = "INSERT INTO competition_ranking (player_id, table_id, rank, last_rank, " + "points, last_points, high_points, low_points, position_match) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    public static final String UPDATE_QUERY = "UPDATE competition_ranking SET rank = ?, last_rank = ?, points = ?, " + "last_points = ?, high_points = ?, low_points = ?, position_match = ? " + "WHERE player_id = ? AND table_id = ?";

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
                        log.warn("Invalid player class: {}", playerClassStr);
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
            log.error("Error getting competition ranking players for table: {}", tableId, e);
        }

        return results;
    }

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
            log.error("Error loading gold arena rank for player {} table {}", playerId, tableId, e);
        }

        return arenaRank;
    }

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
            log.error("Error adding gold rank for player: {}", objectId, e);
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
            log.error("Error updating gold rank for player: {}", objectId, e);
            return false;
        }
    }

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
            log.error("Error adding tower rank for player: {}", objectId, e);
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
            log.error("Error updating tower rank for player: {}", objectId, e);
            return false;
        }
    }

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
            log.error("Error adding tenacity rank for player: {}", objectId, e);
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
            log.error("Error updating tenacity rank for player: {}", objectId, e);
            return false;
        }
    }

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
            log.error("Error adding 6v6 rank for player: {}", objectId, e);
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
            log.error("Error updating 6v6 rank for player: {}", objectId, e);
            return false;
        }
    }

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
            log.error("Error loading arena of tenacity rank for player {} table {}", playerId, tableId, e);
        }

        return ranking;
    }

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
            log.error("Error loading tower of challenge rank for player {} table {}", playerId, tableId, e);
        }

        return ranking;
    }

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
            log.error("Error loading arena 6v6 rank for player {} table {}", playerId, tableId, e);
        }

        return ranking;
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}