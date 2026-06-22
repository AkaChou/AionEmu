package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.HouseBidsDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.house.PlayerHouseBid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * MySQL 8 implementation of HouseBidsDAO
 * @author Rolandas, Updated for MySQL 8
 */
public class MySQL8HouseBidsDAO extends HouseBidsDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8HouseBidsDAO.class);

    private static final String LOAD_QUERY = "SELECT * FROM `house_bids` ORDER BY `house_id`, `bid` DESC";
    private static final String INSERT_QUERY = "INSERT INTO `house_bids` (`player_id`, `house_id`, `bid`, `bid_time`) VALUES (?, ?, ?, ?)";
    private static final String DELETE_QUERY = "DELETE FROM `house_bids` WHERE `house_id` = ?";
    private static final String DELETE_PLAYER_QUERY = "DELETE FROM `house_bids` WHERE `player_id` = ? AND `house_id` = ?";
    private static final String UPDATE_QUERY = "UPDATE `house_bids` SET `bid` = ?, `bid_time` = ? WHERE `player_id` = ? AND `house_id` = ?";
    private static final String SELECT_HIGHEST_BID_QUERY = "SELECT MAX(`bid`) FROM `house_bids` WHERE `house_id` = ?";
    private static final String SELECT_PLAYER_BID_QUERY = "SELECT `bid`, `bid_time` FROM `house_bids` WHERE `player_id` = ? AND `house_id` = ?";

    @Override
    public Set<PlayerHouseBid> loadBids() {
        Set<PlayerHouseBid> bids = new HashSet<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(LOAD_QUERY);
             ResultSet rset = stmt.executeQuery()) {
            
            while (rset.next()) {
                int playerId = rset.getInt("player_id");
                int houseId = rset.getInt("house_id");
                long bidOffer = rset.getLong("bid");
                Timestamp time = rset.getTimestamp("bid_time");
                
                PlayerHouseBid bid = new PlayerHouseBid(playerId, houseId, bidOffer, time);
                bids.add(bid);
            }
            
        } catch (SQLException e) {
            log.error("Failed to load house bids", e);
        }
        
        return bids;
    }

    @Override
    public boolean addBid(int playerId, int houseId, long bidOffer, Timestamp time) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setInt(2, houseId);
            stmt.setLong(3, bidOffer);
            stmt.setTimestamp(4, time);
            
            int result = stmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            log.error("Failed to add house bid - player: {}, house: {}", playerId, houseId, e);
            return false;
        }
    }

    @Override
    public void changeBid(int playerId, int houseId, long newBidOffer, Timestamp time) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setLong(1, newBidOffer);
            stmt.setTimestamp(2, time);
            stmt.setInt(3, playerId);
            stmt.setInt(4, houseId);
            
            int updated = stmt.executeUpdate();
            
            if (updated == 0) {
                addBid(playerId, houseId, newBidOffer, time);
            }
            
        } catch (SQLException e) {
            log.error("Failed to change house bid - player: {}, house: {}", playerId, houseId, e);
        }
    }

    @Override
    public void deleteHouseBids(int houseId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, houseId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Failed to delete bids for house: {}", houseId, e);
        }
    }

    public void deletePlayerBid(int playerId, int houseId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_PLAYER_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setInt(2, houseId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Failed to delete bid - player: {}, house: {}", playerId, houseId, e);
        }
    }

    public long getHighestBid(int houseId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_HIGHEST_BID_QUERY)) {
            
            stmt.setInt(1, houseId);
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getLong(1);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to get highest bid for house: {}", houseId, e);
        }
        
        return 0;
    }

    public PlayerHouseBid getPlayerBid(int playerId, int houseId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_PLAYER_BID_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setInt(2, houseId);
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    long bid = rset.getLong("bid");
                    Timestamp time = rset.getTimestamp("bid_time");
                    return new PlayerHouseBid(playerId, houseId, bid, time);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to get player bid - player: {}, house: {}", playerId, houseId, e);
        }
        
        return null;
    }

    public void deleteHouseBids(Set<Integer> houseIds) {
        if (houseIds == null || houseIds.isEmpty()) {
            return;
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            try (PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
                for (int houseId : houseIds) {
                    stmt.setInt(1, houseId);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
            
            con.commit();
            
        } catch (SQLException e) {
            log.error("Failed to batch delete house bids", e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}