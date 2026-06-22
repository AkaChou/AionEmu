package mysql8;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.loginserver.dao.SvStatsDAO;

/**
 * MySQL8 Server Statistics DAO implementation
 * 
 * @author Updated for MySQL 8
 */
public class MySQL8SvStatsDAO extends SvStatsDAO {
    
    private static final Logger log = LoggerFactory.getLogger(MySQL8SvStatsDAO.class);
    
    private static final String UPDATE_ONLINE = "UPDATE `svstats` SET status = ?, current = ?, max = ?, last_update = NOW() WHERE server = ?";
    private static final String UPDATE_OFFLINE = "UPDATE `svstats` SET status = ?, current = ?, last_update = NOW() WHERE server = ?";
    private static final String UPDATE_ALL_OFFLINE = "UPDATE `svstats` SET status = ?, current = ?, last_update = NOW()";

    @Override
    public void update_SvStats_Online(int server, int status, int current, int max) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_ONLINE)) {
            
            stmt.setInt(1, status);
            stmt.setInt(2, current);
            stmt.setInt(3, max);
            stmt.setInt(4, server);
            
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                insertServerStats(server, status, current, max);
            }
        } catch (SQLException e) {
            log.error("Cannot update online server stats for server: " + server, e);
        }
    }
    
    private void insertServerStats(int server, int status, int current, int max) {
        String insertQuery = "INSERT INTO svstats (server, status, current, max, last_update) VALUES (?, ?, ?, ?, NOW())";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(insertQuery)) {
            
            stmt.setInt(1, server);
            stmt.setInt(2, status);
            stmt.setInt(3, current);
            stmt.setInt(4, max);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Cannot insert server stats for server: " + server, e);
        }
    }
    
    @Override
    public void update_SvStats_Offline(int server, int status, int current) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_OFFLINE)) {
            
            stmt.setInt(1, status);
            stmt.setInt(2, current);
            stmt.setInt(3, server);
            
            int updated = stmt.executeUpdate();
            if (updated == 0) {
                insertServerStats(server, status, current, 0);
            }
        } catch (SQLException e) {
            log.error("Cannot update offline server stats for server: " + server, e);
        }
    }
    
    @Override
    public void update_SvStats_All_Offline(int status, int current) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_ALL_OFFLINE)) {
            
            stmt.setInt(1, status);
            stmt.setInt(2, current);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Cannot update all servers offline stats", e);
        }
    }

    @Override
    public boolean supports(String database, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(database, majorVersion, minorVersion);
    }
}