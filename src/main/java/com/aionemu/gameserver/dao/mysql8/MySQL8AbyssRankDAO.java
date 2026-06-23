package com.aionemu.gameserver.dao.mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dao.AbyssRankDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.AbyssRankingResult;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.AbyssRank;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL 8 implementation of AbyssRankDAO
 * Fixed connection leaks
 */
public class MySQL8AbyssRankDAO extends AbyssRankDAO {
    
    private static final Logger log = LoggerFactory.getLogger(MySQL8AbyssRankDAO.class);
    
    public static final String SELECT_QUERY = "SELECT daily_ap, daily_gp, weekly_ap, weekly_gp, ap, gp, `rank`, " + "top_ranking, daily_kill, weekly_kill, all_kill, max_rank, last_kill, " + "last_ap, last_gp, last_update FROM abyss_rank WHERE player_id = ?";
    
    public static final String INSERT_QUERY = "INSERT INTO abyss_rank (player_id, daily_ap, daily_gp, weekly_ap, " + "weekly_gp, ap, gp, `rank`, top_ranking, daily_kill, weekly_kill, " + "all_kill, max_rank, last_kill, last_ap, last_gp, last_update) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    public static final String UPDATE_QUERY = "UPDATE abyss_rank SET daily_ap = ?, daily_gp = ?, weekly_ap = ?, " + "weekly_gp = ?, ap = ?, gp = ?, `rank` = ?, top_ranking = ?, daily_kill = ?, " +  "weekly_kill = ?, all_kill = ?, max_rank = ?, last_kill = ?, last_ap = ?, " + "last_gp = ?, last_update = ? WHERE player_id = ?";
    
    public static final String SELECT_PLAYERS_RANKING = "SELECT abyss_rank.`rank`, abyss_rank.ap, abyss_rank.gp, abyss_rank.old_rank_pos, " + "abyss_rank.rank_pos, players.name, legions.name, players.id, players.title_id, " + "players.player_class, players.exp FROM abyss_rank INNER JOIN players " + "ON abyss_rank.player_id = players.id LEFT JOIN legion_members " + "ON legion_members.player_id = players.id LEFT JOIN legions " + "ON legions.id = legion_members.legion_id WHERE players.race = ? " + "AND abyss_rank.gp > 1243 ORDER BY abyss_rank.gp DESC LIMIT 300";
    
    public static final String SELECT_LEGIONS_RANKING = "SELECT legions.id, legions.name, legions.contribution_points, " + "legions.level as lvl, legions.old_rank_pos, legions.rank_pos " + "FROM legions, legion_members, players WHERE players.race = ? " + "AND legion_members.`rank` = 'BRIGADE_GENERAL' AND legion_members.player_id = players.id " + "AND legion_members.legion_id = legions.id AND legions.contribution_points > 0 " + "GROUP BY id ORDER BY legions.contribution_points DESC LIMIT 50";
    
    public static final String SELECT_AP_PLAYER = "SELECT player_id, ap FROM abyss_rank, players WHERE " + "abyss_rank.player_id = players.id AND players.race = ? AND ap > ? " + "ORDER BY ap DESC";
    
    public static final String SELECT_AP_PLAYER_ACTIVE_ONLY =  "SELECT player_id, ap FROM abyss_rank, players WHERE " + "abyss_rank.player_id = players.id AND players.race = ? AND ap > ? " + "AND UNIX_TIMESTAMP(CURDATE()) - UNIX_TIMESTAMP(players.last_online) <= ? * 24 * 60 * 60 " + "ORDER BY ap DESC";
    
    public static final String SELECT_GP_PLAYER = "SELECT player_id, gp FROM abyss_rank, players WHERE " + "abyss_rank.player_id = players.id AND players.race = ? AND gp > ? " + "ORDER BY gp DESC";
    
    public static final String SELECT_GP_PLAYER_ACTIVE_ONLY =  "SELECT player_id, gp FROM abyss_rank, players WHERE " + "abyss_rank.player_id = players.id AND players.race = ? AND gp > ? " + "AND UNIX_TIMESTAMP(CURDATE()) - UNIX_TIMESTAMP(players.last_online) <= ? * 24 * 60 * 60 " + "ORDER BY gp DESC";
    
    public static final String UPDATE_RANK = "UPDATE abyss_rank SET `rank` = ?, top_ranking = ? WHERE player_id = ?";
    
    public static final String SELECT_LEGION_COUNT = "SELECT COUNT(player_id) as players FROM legion_members WHERE legion_id = ?";
    
    public static final String UPDATE_PLAYER_RANK_LIST = "UPDATE abyss_rank SET abyss_rank.old_rank_pos = abyss_rank.rank_pos, " + "abyss_rank.rank_pos = @a:=@a+1 WHERE player_id IN " + "(SELECT id FROM players WHERE race = ?) ORDER BY gp DESC " + (GSConfig.ABYSSRANKING_SMALL_CACHE ? "LIMIT 500" : "");
    
    public static final String UPDATE_LEGION_RANK_LIST = "UPDATE legions SET legions.old_rank_pos = legions.rank_pos, " + "legions.rank_pos = @a:=@a+1 WHERE id IN " + "(SELECT legion_id FROM legion_members, players WHERE legion_members.`rank` = 'BRIGADE_GENERAL' " + "AND players.id = legion_members.player_id AND players.race = ?) " + "ORDER BY legions.contribution_points DESC " + (GSConfig.ABYSSRANKING_SMALL_CACHE ? "LIMIT 75" : "");
    
    public static final String DELETE_QUERY = "DELETE FROM `abyss_rank` WHERE player_id = ?";
    
    @Override
    public AbyssRank loadAbyssRank(int playerId) {
        AbyssRank abyssRank = null;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, playerId);
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                if (resultSet.next()) {
                    int daily_ap = resultSet.getInt("daily_ap");
                    int daily_gp = resultSet.getInt("daily_gp");
                    int weekly_ap = resultSet.getInt("weekly_ap");
                    int weekly_gp = resultSet.getInt("weekly_gp");
                    int ap = resultSet.getInt("ap");
                    int gp = resultSet.getInt("gp");
                    int rank = resultSet.getInt("rank");
                    int top_ranking = resultSet.getInt("top_ranking");
                    int daily_kill = resultSet.getInt("daily_kill");
                    int weekly_kill = resultSet.getInt("weekly_kill");
                    int all_kill = resultSet.getInt("all_kill");
                    int max_rank = resultSet.getInt("max_rank");
                    int last_kill = resultSet.getInt("last_kill");
                    int last_ap = resultSet.getInt("last_ap");
                    int last_gp = resultSet.getInt("last_gp");
                    long last_update = resultSet.getLong("last_update");
                    
                    abyssRank = new AbyssRank(daily_ap, daily_gp, weekly_ap, weekly_gp, ap, gp, rank, top_ranking, daily_kill, weekly_kill, all_kill, max_rank, last_kill, last_ap, last_gp, last_update);
                    
                    abyssRank.setPersistentState(PersistentState.UPDATED);
                } else {
                    abyssRank = new AbyssRank(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, System.currentTimeMillis());
                    abyssRank.setPersistentState(PersistentState.NEW);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading abyss rank for player: {}", playerId, e);
        }
        
        return abyssRank;
    }
    
    @Override
    public void loadAbyssRank(final Player player) {
        AbyssRank rank = loadAbyssRank(player.getObjectId());
        player.setAbyssRank(rank);
    }
    
    @Override
    public boolean storeAbyssRank(Player player) {
        AbyssRank rank = player.getAbyssRank();
        boolean result = false;
        
        switch (rank.getPersistentState()) {
            case NEW:
                result = addRank(player.getObjectId(), rank);
                break;
            case UPDATE_REQUIRED:
                result = updateRank(player.getObjectId(), rank);
                break;
            default:
                return true;
        }
        
        rank.setPersistentState(PersistentState.UPDATED);
        return result;
    }
    
    private boolean addRank(final int objectId, final AbyssRank rank) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, objectId);
            stmt.setInt(2, rank.getDailyAP());
            stmt.setInt(3, rank.getDailyGP());
            stmt.setInt(4, rank.getWeeklyAP());
            stmt.setInt(5, rank.getWeeklyGP());
            stmt.setInt(6, rank.getAp());
            stmt.setInt(7, rank.getGp());
            stmt.setInt(8, rank.getRank().getId());
            stmt.setInt(9, rank.getTopRanking());
            stmt.setInt(10, rank.getDailyKill());
            stmt.setInt(11, rank.getWeeklyKill());
            stmt.setInt(12, rank.getAllKill());
            stmt.setInt(13, rank.getMaxRank());
            stmt.setInt(14, rank.getLastKill());
            stmt.setInt(15, rank.getLastAP());
            stmt.setInt(16, rank.getLastGP());
            stmt.setLong(17, rank.getLastUpdate());
            stmt.executeUpdate();
            
            return true;
        } catch (SQLException e) {
            log.error("Error adding abyss rank for player: {}", objectId, e);
            return false;
        }
    }
    
    private boolean updateRank(final int objectId, final AbyssRank rank) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setInt(1, rank.getDailyAP());
            stmt.setInt(2, rank.getDailyGP());
            stmt.setInt(3, rank.getWeeklyAP());
            stmt.setInt(4, rank.getWeeklyGP());
            stmt.setInt(5, rank.getAp());
            stmt.setInt(6, rank.getGp());
            stmt.setInt(7, rank.getRank().getId());
            stmt.setInt(8, rank.getTopRanking());
            stmt.setInt(9, rank.getDailyKill());
            stmt.setInt(10, rank.getWeeklyKill());
            stmt.setInt(11, rank.getAllKill());
            stmt.setInt(12, rank.getMaxRank());
            stmt.setInt(13, rank.getLastKill());
            stmt.setInt(14, rank.getLastAP());
            stmt.setInt(15, rank.getLastGP());
            stmt.setLong(16, rank.getLastUpdate());
            stmt.setInt(17, objectId);
            stmt.executeUpdate();
            
            return true;
        } catch (SQLException e) {
            log.error("Error updating abyss rank for player: {}", objectId, e);
            return false;
        }
    }
    
    @Override
    public ArrayList<AbyssRankingResult> getAbyssRankingPlayers(final Race race) {
        ArrayList<AbyssRankingResult> results = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_PLAYERS_RANKING)) {
            
            stmt.setString(1, race.toString());
            
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    String name = resultSet.getString("players.name");
                    int playerAbyssRank = resultSet.getInt("abyss_rank.rank");
                    int ap = resultSet.getInt("abyss_rank.ap");
                    int gp = resultSet.getInt("abyss_rank.gp");
                    int playerTitle = resultSet.getInt("players.title_id");
                    int playerId = resultSet.getInt("players.id");
                    String playerClassStr = resultSet.getString("players.player_class");
                    int playerLevel = DataManager.PLAYER_EXPERIENCE_TABLE.getLevelForExp(resultSet.getLong("players.exp"));
                    String playerLegion = resultSet.getString("legions.name");
                    int oldRankPos = resultSet.getInt("old_rank_pos");
                    int rankPos = resultSet.getInt("rank_pos");
                    
                    PlayerClass playerClass;
                    try {
                        playerClass = PlayerClass.getPlayerClassByString(playerClassStr);
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid player class: {}", playerClassStr);
                        continue;
                    }
                    
                    if (playerClass != null) {
                        AbyssRankingResult rsl = new AbyssRankingResult(name, playerAbyssRank, playerId, ap, gp, playerTitle, playerClass, playerLevel, playerLegion, oldRankPos, rankPos);
                        results.add(rsl);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error getting abyss ranking players for race: {}", race, e);
        }
        
        return results;
    }
    
    @Override
    public ArrayList<AbyssRankingResult> getAbyssRankingLegions(final Race race) {
        final ArrayList<AbyssRankingResult> results = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGIONS_RANKING)) {
            
            stmt.setString(1, race.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("legions.name");
                    long cp = rs.getLong("legions.contribution_points");
                    int legionId = rs.getInt("legions.id");
                    int legionLevel = rs.getInt("lvl");
                    int legionMembers = getLegionMembersCount(legionId);
                    int oldRankPos = rs.getInt("old_rank_pos");
                    int rankPos = rs.getInt("rank_pos");
                    
                    AbyssRankingResult rsl = new AbyssRankingResult(cp, name, legionId, legionLevel, legionMembers, oldRankPos, rankPos);
                    results.add(rsl);
                }
            }
        } catch (SQLException e) {
            log.error("Error getting abyss ranking legions for race: {}", race, e);
        }
        
        return results;
    }
    
    private int getLegionMembersCount(final int legionId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGION_COUNT)) {
            
            stmt.setInt(1, legionId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("players");
                }
            }
        } catch (SQLException e) {
            log.error("Error getting legion members count for legion: {}", legionId, e);
        }
        
        return 0;
    }
    
    @Override
    public Map<Integer, Integer> loadPlayersAp(final Race race, final int lowerApLimit, final int maxOfflineDays) {
        final Map<Integer, Integer> results = new HashMap<>();
        String query = maxOfflineDays > 0 ? SELECT_AP_PLAYER_ACTIVE_ONLY : SELECT_AP_PLAYER;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setString(1, race.toString());
            stmt.setInt(2, lowerApLimit);
            
            if (maxOfflineDays > 0) {
                stmt.setInt(3, maxOfflineDays);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int playerId = rs.getInt("player_id");
                    int ap = rs.getInt("ap");
                    results.put(playerId, ap);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading players AP for race: {}", race, e);
        }
        
        return results;
    }

    @Override
    public Map<Integer, Integer> loadPlayersGp(final Race race, final int lowerGpLimit, final int maxOfflineDays) {
        final Map<Integer, Integer> results = new HashMap<>();
        String query = maxOfflineDays > 0 ? SELECT_GP_PLAYER_ACTIVE_ONLY : SELECT_GP_PLAYER;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setString(1, race.toString());
            stmt.setInt(2, lowerGpLimit);
            
            if (maxOfflineDays > 0) {
                stmt.setInt(3, maxOfflineDays);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int playerId = rs.getInt("player_id");
                    int gp = rs.getInt("gp");
                    results.put(playerId, gp);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading players GP for race: {}", race, e);
        }
        
        return results;
    }
    
    @Override
    public void updateAbyssRank(int playerId, AbyssRankEnum rankEnum) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_RANK)) {
            
            stmt.setInt(1, rankEnum.getId());
            stmt.setInt(2, rankEnum.getQuota());
            stmt.setInt(3, playerId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Error updating abyss rank for player: {}", playerId, e);
        }
    }
    
    @Override
    public void updateRankList() {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            // Update Elyos player ranks
            try (PreparedStatement stmt1 = con.prepareStatement("SET @a:=0");
                 PreparedStatement stmt2 = con.prepareStatement(UPDATE_PLAYER_RANK_LIST)) {
                stmt1.execute();
                stmt2.setString(1, "ELYOS");
                stmt2.executeUpdate();
            }
            
            // Update Asmodian player ranks
            try (PreparedStatement stmt1 = con.prepareStatement("SET @a:=0");
                 PreparedStatement stmt2 = con.prepareStatement(UPDATE_PLAYER_RANK_LIST)) {
                stmt1.execute();
                stmt2.setString(1, "ASMODIANS");
                stmt2.executeUpdate();
            }
            
            // Update Elyos legion ranks
            try (PreparedStatement stmt1 = con.prepareStatement("SET @a:=0");
                 PreparedStatement stmt2 = con.prepareStatement(UPDATE_LEGION_RANK_LIST)) {
                stmt1.execute();
                stmt2.setString(1, "ELYOS");
                stmt2.executeUpdate();
            }
            
            // Update Asmodian legion ranks
            try (PreparedStatement stmt1 = con.prepareStatement("SET @a:=0");
                 PreparedStatement stmt2 = con.prepareStatement(UPDATE_LEGION_RANK_LIST)) {
                stmt1.execute();
                stmt2.setString(1, "ASMODIANS");
                stmt2.executeUpdate();
            }
            
            con.commit();
        } catch (SQLException e) {
            log.error("Error updating rank lists", e);
            try (Connection con = DatabaseFactory.getConnection()) {
                con.rollback();
            } catch (SQLException ex) {
                log.error("Error rolling back rank list update", ex);
            }
        }
    }
    
    @Override
    public void removePlayer(List<Player> listP) {
        if (listP == null || listP.isEmpty()) {
            return;
        }
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            con.setAutoCommit(false);
            
            for (Player player : listP) {
                stmt.setInt(1, player.getObjectId());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
            con.commit();
        } catch (Exception e) {
            log.error("Error while deleting players from Abyss Rank", e);
            try (Connection con = DatabaseFactory.getConnection()) {
                con.rollback();
            } catch (SQLException ex) {
                log.error("Error rolling back player deletion", ex);
            }
        }
    }
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}