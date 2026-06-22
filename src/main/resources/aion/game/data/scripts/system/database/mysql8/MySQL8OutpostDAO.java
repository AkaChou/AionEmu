package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.OutpostDAO;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.outpost.OutpostLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MySQL 8 implementation of OutpostDAO
 * Created by Wnkrz on 27/08/2017. Updated for MySQL 8.
 */
public class MySQL8OutpostDAO extends OutpostDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8OutpostDAO.class);

    private static final String SELECT_QUERY = "SELECT `id`, `race` FROM `outpost_location` ORDER BY `id`";
    private static final String UPDATE_QUERY = "UPDATE `outpost_location` SET `race` = ? WHERE `id` = ?";
    private static final String INSERT_QUERY = "INSERT INTO `outpost_location` (`id`, `race`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `race` = VALUES(`race`)";

    @Override
    public boolean loadOutposLocations(Map<Integer, OutpostLocation> locations) {
        boolean success = true;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
             ResultSet rset = stmt.executeQuery()) {
            
            Map<Integer, Boolean> loadedIds = new ConcurrentHashMap<>();
            
            while (rset.next()) {
                int id = rset.getInt("id");
                OutpostLocation loc = locations.get(id);
                
                if (loc != null) {
                    loc.setRace(Race.valueOf(rset.getString("race")));
                    loadedIds.put(id, true);
                }
            }
            
            for (Map.Entry<Integer, OutpostLocation> entry : locations.entrySet()) {
                if (!loadedIds.containsKey(entry.getKey())) {
                    insertOutpostLocation(entry.getValue());
                }
            }
            
        } catch (SQLException e) {
            log.error("Error loading outpost locations from database", e);
            success = false;
        }
        
        return success;
    }

    @Override
    public boolean updateOutpostLocation(OutpostLocation location) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setString(1, location.getRace().toString());
            stmt.setInt(2, location.getId());
            
            int updated = stmt.executeUpdate();
            return updated > 0;
            
        } catch (SQLException e) {
            log.error("Error updating outpost location: {}", location.getId(), e);
            return false;
        }
    }

    private boolean insertOutpostLocation(OutpostLocation location) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, location.getId());
            stmt.setString(2, Race.NPC.toString());
            
            int inserted = stmt.executeUpdate();
            return inserted > 0;
            
        } catch (SQLException e) {
            log.error("Error inserting outpost location: {}", location.getId(), e);
            return false;
        }
    }

    public boolean updateOutpostLocations(Map<Integer, OutpostLocation> locations) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
                for (OutpostLocation loc : locations.values()) {
                    stmt.setString(1, loc.getRace().toString());
                    stmt.setInt(2, loc.getId());
                    stmt.addBatch();
                }
                
                int[] results = stmt.executeBatch();
                con.commit();
                
                for (int result : results) {
                    if (result == PreparedStatement.EXECUTE_FAILED) {
                        return false;
                    }
                }
                return true;
            }
            
        } catch (SQLException e) {
            log.error("Error batch updating outpost locations", e);
            return false;
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}