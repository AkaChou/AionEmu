package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.LadderDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by wanke on 12/02/2017.
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8LadderDAO extends LadderDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8LadderDAO.class);
    
    private static final String SELECT_PLAYER_DATA = "SELECT player_id, last_update, rating, wins, rank FROM ladder_player " + "WHERE wins > 0 OR losses > 0 OR leaves > 0 ORDER BY rating DESC, wins DESC, player_id ASC";
    
    private static final String UPDATE_RANK = "UPDATE ladder_player SET rank = ? WHERE player_id = ?";
    
    private static final String UPDATE_LAST_RANK = "UPDATE ladder_player SET last_rank = ?, last_update = ? WHERE player_id = ?";
    
    private static final String SELECT_CHECK_EXISTS = "SELECT 1 FROM ladder_player WHERE player_id = ? LIMIT 1";
    
    private static final String SELECT_GET_DATA = "SELECT ? FROM ladder_player WHERE player_id = ?";
    
    private static final String SELECT_GET_ALL = "SELECT * FROM ladder_player WHERE player_id = ?";
    
    private static final String UPDATE_ADD_DATA = "UPDATE ladder_player SET ? = ? + ? WHERE player_id = ?";
    
    private static final String UPDATE_SET_DATA = "UPDATE ladder_player SET ? = ? WHERE player_id = ?";
    
    private static final String INSERT_PLAYER = "INSERT INTO ladder_player (player_id, ?) VALUES (?, ?)";
    
    private static final String SELECT_LAST_UPDATE = "SELECT last_update FROM ladder_player WHERE player_id = ?";
    
    private static final String UPDATE_LAST_UPDATE = "UPDATE ladder_player SET last_update = ? WHERE player_id = ?";
    
    @Override
    public void addWin(Player player) {
        addPlayerLadderData(player, "wins", 1);
    }
    
    @Override
    public void addLoss(Player player) {
        addPlayerLadderData(player, "losses", 1);
    }
    
    @Override
    public void addLeave(Player player) {
        addPlayerLadderData(player, "leaves", 1);
    }
    
    @Override
    public void addRating(Player player, int rating) {
        addPlayerLadderData(player, "rating", rating);
    }
    
    @Override
    public void setWins(Player player, int wins) {
        setPlayerLadderData(player, "wins", wins);
    }
    
    @Override
    public void setLosses(Player player, int losses) {
        setPlayerLadderData(player, "losses", losses);
    }
    
    @Override
    public void setLeaves(Player player, int leaves) {
        setPlayerLadderData(player, "leaves", leaves);
    }
    
    @Override
    public void setRating(Player player, int rating) {
        setPlayerLadderData(player, "rating", rating);
    }
    
    @Override
    public int getWins(Player player) {
        return getPlayerLadderData(player, "wins");
    }
    
    @Override
    public int getLosses(Player player) {
        return getPlayerLadderData(player, "losses");
    }
    
    @Override
    public int getLeaves(Player player) {
        return getPlayerLadderData(player, "leaves");
    }
    
    @Override
    public int getRating(Player player) {
        int rating = getPlayerLadderData(player, "rating");
        return rating == 0 ? 1000 : rating;
    }
    
    @Override
    public int getRank(Player player) {
        return getPlayerLadderData(player, "rank");
    }
    
    @Override
    public void updateRanks() {
        List<PlayerInfo> players = new ArrayList<>();
        
        // Load all players
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
            log.error("Error loading ladder players for rank update", e);
            return;
        }

        // Sort players
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
        
        // Update ranks
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
                log.error("Error executing batch updates for ladder ranks", e);
            }
        } catch (SQLException e) {
            log.error("Error updating ladder ranks", e);
        }
    }
    
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
            log.error("Error adding player ladder data for player: " + player.getObjectId(), e);
        }
    }
    
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
            log.error("Error setting player ladder data for player: " + player.getObjectId(), e);
        }
    }
    
    public void setPlayerLadderData(Integer playerId, String column, int value) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement("UPDATE ladder_player SET " + column + " = ? WHERE player_id = ?")) {
            
            stmt.setInt(1, value);
            stmt.setInt(2, playerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error setting player ladder data for player ID: " + playerId, e);
        }
    }
    
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
            log.error("Error getting player ladder data for player: " + player.getObjectId(), e);
        }
        
        return value;
    }
    
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
            log.error("Error getting player ladder data for player ID: " + playerId, e);
        }
        
        return value;
    }
    
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
            log.error("Error getting player ladder update time for player: " + player.getObjectId(), e);
        }
        return value;
    }
    
    public void setPlayerLadderUpdate(Player player, Timestamp value) {
        int playerId = player.getObjectId();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_LAST_UPDATE)) {
            
            stmt.setTimestamp(1, value);
            stmt.setInt(2, playerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error setting player ladder update time for player: " + player.getObjectId(), e);
        }
    }
    
    public void setPlayerLadderUpdate(Integer playerId, Timestamp value) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_LAST_UPDATE)) {
            
            stmt.setTimestamp(1, value);
            stmt.setInt(2, playerId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error setting player ladder update time for player ID: " + playerId, e);
        }
    }
    
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
            log.error("Error getting complete player ladder data for player: " + player.getObjectId(), e);
        }
        
        if (data == null) {
            data = new PlayerLadderData(player, 1000, 0, 0, 0, 0, new Timestamp(0));
        }
        return data;
    }
    
    private boolean checkExists(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_CHECK_EXISTS)) {
            
            stmt.setInt(1, playerId);
            try (ResultSet rset = stmt.executeQuery()) {
                return rset.next();
            }
        } catch (SQLException e) {
            log.error("Error checking if player exists in ladder: " + playerId, e);
            return false;
        }
    }
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return databaseName.toLowerCase().contains("mysql") && majorVersion >= 8;
    }
    
    private static class PlayerInfo {
        private final int playerId;
        private final int rating;
        private final Timestamp lastUpdate;
        private final int wins;
        private final int rank;
        
        public PlayerInfo(int playerId, int rating, Timestamp lastUpdate, int wins, int rank) {
            this.playerId = playerId;
            this.rating = rating;
            this.lastUpdate = lastUpdate;
            this.wins = wins;
            this.rank = rank;
        }
        
        public int getPlayerId() { return playerId; }
        public int getRating() { return rating; }
        public Timestamp getLastUpdate() { return lastUpdate; }
        public int getWins() { return wins; }
        public int getRank() { return rank; }
    }
}