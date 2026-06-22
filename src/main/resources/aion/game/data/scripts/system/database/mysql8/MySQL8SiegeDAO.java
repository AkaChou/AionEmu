package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.SiegeDAO;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeRace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MySQL 8 implementation of SiegeDAO
 * @author Updated for MySQL 8
 */
public class MySQL8SiegeDAO extends SiegeDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8SiegeDAO.class);

    private static final String SELECT_QUERY = "SELECT `id`, `race`, `legion_id` FROM `siege_locations` ORDER BY `id`";
    private static final String INSERT_QUERY = "INSERT INTO `siege_locations` (`id`, `race`, `legion_id`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `race` = VALUES(`race`), `legion_id` = VALUES(`legion_id`)";
    private static final String UPDATE_QUERY = "UPDATE `siege_locations` SET `race` = ?, `legion_id` = ? WHERE `id` = ?";
    private static final String SELECT_BY_RACE_QUERY = "SELECT `id`, `race`, `legion_id` FROM `siege_locations` WHERE `race` = ?";

    @Override
    public boolean loadSiegeLocations(Map<Integer, SiegeLocation> locations) {
        boolean success = true;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
             ResultSet rset = stmt.executeQuery()) {
            
            Map<Integer, Boolean> loadedIds = new ConcurrentHashMap<>();
            
            while (rset.next()) {
                int id = rset.getInt("id");
                SiegeLocation loc = locations.get(id);
                
                if (loc != null) {
                    loc.setRace(SiegeRace.valueOf(rset.getString("race")));
                    loc.setLegionId(rset.getInt("legion_id"));
                    loadedIds.put(id, true);
                }
            }
            
            for (Map.Entry<Integer, SiegeLocation> entry : locations.entrySet()) {
                if (!loadedIds.containsKey(entry.getKey())) {
                    insertSiegeLocation(entry.getValue());
                }
            }
            
        } catch (SQLException e) {
            log.error("Error loading siege locations from database", e);
            success = false;
        }
        
        return success;
    }

    @Override
    public boolean updateSiegeLocation(SiegeLocation siegeLocation) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setString(1, siegeLocation.getRace().toString());
            stmt.setInt(2, siegeLocation.getLegionId());
            stmt.setInt(3, siegeLocation.getLocationId());
            
            int updated = stmt.executeUpdate();
            return updated > 0;
            
        } catch (SQLException e) {
            log.error("Error updating siege location: {}", siegeLocation.getLocationId(), e);
            return false;
        }
    }

    private boolean insertSiegeLocation(SiegeLocation siegeLocation) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, siegeLocation.getLocationId());
            stmt.setString(2, siegeLocation.getRace().toString());
            stmt.setInt(3, siegeLocation.getLegionId());
            
            int inserted = stmt.executeUpdate();
            return inserted > 0;
            
        } catch (SQLException e) {
            log.error("Error inserting siege location: {}", siegeLocation.getLocationId(), e);
            return false;
        }
    }

    public boolean updateSiegeLocations(Map<Integer, SiegeLocation> locations) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
                for (SiegeLocation loc : locations.values()) {
                    stmt.setString(1, loc.getRace().toString());
                    stmt.setInt(2, loc.getLegionId());
                    stmt.setInt(3, loc.getLocationId());
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
            log.error("Error batch updating siege locations", e);
            return false;
        }
    }

    public Map<Integer, SiegeLocation> loadSiegeLocationsByRace(SiegeRace race) {
        Map<Integer, SiegeLocation> locations = new ConcurrentHashMap<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_BY_RACE_QUERY)) {
            
            stmt.setString(1, race.toString());
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("id");
                    SiegeLocation loc = locations.get(id);
                    loc.setRace(SiegeRace.valueOf(rset.getString("race")));
                    loc.setLegionId(rset.getInt("legion_id"));
                    locations.put(id, loc);
                }
            }
            
        } catch (SQLException e) {
            log.error("Error loading siege locations by race: {}", race, e);
        }
        
        return locations;
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}