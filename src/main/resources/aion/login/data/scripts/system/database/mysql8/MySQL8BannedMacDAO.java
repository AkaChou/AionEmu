package mysql8;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.util.FastMap;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.dao.BannedMacDAO;
import com.aionemu.loginserver.model.base.BannedMacEntry;

/**
 * MySQL8 BannedMac DAO implementation
 * 
 * @author Updated for MySQL 8
 */
public class MySQL8BannedMacDAO extends BannedMacDAO {

    private static Logger log = LoggerFactory.getLogger(MySQL8BannedMacDAO.class);

    @Override
    public Map<String, BannedMacEntry> load() {
        Map<String, BannedMacEntry> map = new FastMap<>();
        String query = "SELECT `address`, `time`, `details` FROM `banned_mac`";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                String address = rs.getString("address");
                map.put(address, new BannedMacEntry(address, rs.getTimestamp("time"), rs.getString("details")));
            }
        } catch (SQLException e) {
            log.error("Error loading banned MAC addresses", e);
        }
        
        return map;
    }

    @Override
    public boolean update(BannedMacEntry entry) {
        String query = "REPLACE INTO `banned_mac` (`address`, `time`, `details`) VALUES (?, ?, ?)";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            
            ps.setString(1, entry.getMac());
            ps.setTimestamp(2, entry.getTime());
            ps.setString(3, entry.getDetails());
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error storing BannedMacEntry " + entry.getMac(), e);
        }
        
        return false;
    }

    @Override
    public boolean remove(String address) {
        String query = "DELETE FROM `banned_mac` WHERE address = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            
            ps.setString(1, address);
            
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error removing BannedMacEntry " + address, e);
        }
        
        return false;
    }

    @Override
    public void cleanExpiredBans() {
        String query = "DELETE FROM `banned_mac` WHERE time < CURDATE()";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement ps = con.prepareStatement(query)) {
            
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                log.info("Cleaned " + deleted + " expired MAC bans");
            }
        } catch (SQLException e) {
            log.error("Error cleaning expired MAC bans", e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}