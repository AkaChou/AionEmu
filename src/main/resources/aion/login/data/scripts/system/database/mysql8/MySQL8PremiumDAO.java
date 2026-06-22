package mysql8;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.dao.PremiumDAO;

/**
 * MySQL8 Premium DAO implementation
 * 
 * @author Updated for MySQL 8
 */
public class MySQL8PremiumDAO extends PremiumDAO {
    
    private final Logger log = LoggerFactory.getLogger("PREMIUM_CTRL");

    @Override
    public long getPoints(int accountId) {
        long points = 0;
        String query = "SELECT toll FROM account_data WHERE id = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setInt(1, accountId);
            
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    points = rs.getLong("toll");
                }
            }
        } catch (SQLException e) {
            log.error("getPoints [select points] " + accountId, e);
        }
        
        List<Integer> rewarded = new ArrayList<>();
        query = "SELECT uniqId, points FROM account_rewards WHERE accountId = ? AND rewarded = 0";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setInt(1, accountId);
            
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) {
                    int uniqId = rs.getInt("uniqId");
                    points += rs.getLong("points");
                    log.info("Account " + accountId + " has received uniqId #" + uniqId);
                    rewarded.add(uniqId);
                }
            }
        } catch (SQLException e) {
            log.error("getPoints [get rewards] " + accountId, e);
        }
        
        if (!rewarded.isEmpty()) {
            String updateQuery = "UPDATE account_rewards SET rewarded = 1, received = NOW() WHERE uniqId = ?";
            
            try (Connection con = DatabaseFactory.getConnection();
                 PreparedStatement stmt = con.prepareStatement(updateQuery)) {
                
                for (int uniqid : rewarded) {
                    stmt.setInt(1, uniqid);
                    stmt.addBatch();
                }
                
                stmt.executeBatch();
            } catch (SQLException e) {
                log.error("getPoints [update uniq] " + accountId, e);
            }
        }
        
        return points;
    }

    @Override
    public boolean updatePoints(int accountId, long points, long required) {
        String query = "UPDATE account_data SET toll = ? WHERE id = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setLong(1, points - required);
            stmt.setInt(2, accountId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("updatePoints " + accountId, e);
        }
        
        return false;
    }
    
    @Override
    public long getLuna(int accountId) {
        long luna = 0;
        String query = "SELECT luna FROM account_data WHERE id = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(query)) {
            
            st.setInt(1, accountId);
            
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    luna = rs.getLong("luna");
                }
            }
        } catch (SQLException e) {
            log.error("getLuna [select Luna] " + accountId, e);
        }
        
        return luna;
    }
    
    @Override
    public boolean updateLuna(int accountId, long luna) {
        String query = "UPDATE account_data SET luna = ? WHERE id = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setLong(1, luna);
            stmt.setInt(2, accountId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("updateLuna " + accountId, e);
        }
        
        return false;
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(database, majorVersion, minorVersion);
    }
}