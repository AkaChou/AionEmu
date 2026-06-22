package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.BaseDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.base.BaseLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MySQL 8 implementation of BaseDAO
 * @author Updated for MySQL 8
 */
public class MySQL8BaseDAO extends BaseDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8BaseDAO.class);

    private static final String SELECT_QUERY = "SELECT `id`, `race` FROM `base_location` ORDER BY `id`";
    private static final String UPDATE_QUERY = "UPDATE `base_location` SET `race` = ? WHERE `id` = ?";
    private static final String INSERT_QUERY = "INSERT INTO `base_location` (`id`, `race`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `race` = VALUES(`race`)";
    private static final String SELECT_BY_RACE_QUERY = "SELECT `id`, `race` FROM `base_location` WHERE `race` = ?";

    @Override
    public boolean loadBaseLocations(Map<Integer, BaseLocation> locations) {
        boolean success = true;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
             ResultSet rset = stmt.executeQuery()) {
            
            Map<Integer, Boolean> loadedIds = new ConcurrentHashMap<>();
            
            while (rset.next()) {
                int id = rset.getInt("id");
                BaseLocation loc = locations.get(id);
                
                if (loc != null) {
                    loc.setRace(Race.valueOf(rset.getString("race")));
                    loadedIds.put(id, true);
                }
            }
            
            for (Map.Entry<Integer, BaseLocation> entry : locations.entrySet()) {
                if (!loadedIds.containsKey(entry.getKey())) {
                    insertBaseLocation(entry.getValue());
                }
            }
            
        } catch (SQLException e) {
            log.error("Error loading base locations from database", e);
            success = false;
        }
        
        return success;
    }

    @Override
    public boolean updateBaseLocation(BaseLocation location) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setString(1, location.getRace().toString());
            stmt.setInt(2, location.getId());
            
            int updated = stmt.executeUpdate();
            return updated > 0;
            
        } catch (SQLException e) {
            log.error("Error updating base location: {}", location.getId(), e);
            return false;
        }
    }

    private boolean insertBaseLocation(BaseLocation location) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, location.getId());
            stmt.setString(2, Race.NPC.toString());
            
            int inserted = stmt.executeUpdate();
            return inserted > 0;
            
        } catch (SQLException e) {
            log.error("Error inserting base location: {}", location.getId(), e);
            return false;
        }
    }

    public boolean updateBaseLocations(Map<Integer, BaseLocation> locations) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
                for (BaseLocation loc : locations.values()) {
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
            log.error("Error batch updating base locations", e);
            return false;
        }
    }

    public Map<Integer, BaseLocation> loadBaseLocationsByRace(Race race) {
        Map<Integer, BaseLocation> locations = new ConcurrentHashMap<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_BY_RACE_QUERY)) {
            
            stmt.setString(1, race.toString());
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("id");
                    BaseLocation loc = locations.get(id);
                    if (loc != null) {
                        loc.setRace(Race.valueOf(rset.getString("race")));
                        locations.put(id, loc);
                    }
                }
            }
            
        } catch (SQLException e) {
            log.error("Error loading base locations by race: {}", race, e);
        }
        
        return locations;
    }

    public boolean resetBaseLocations() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            con.setAutoCommit(false);
            
            try (PreparedStatement selectStmt = con.prepareStatement(SELECT_QUERY);
                 ResultSet rset = selectStmt.executeQuery()) {
                
                while (rset.next()) {
                    int id = rset.getInt("id");
                    stmt.setString(1, Race.NPC.toString());
                    stmt.setInt(2, id);
                    stmt.addBatch();
                }
                
                int[] results = stmt.executeBatch();
                con.commit();
                
                return true;
            }
            
        } catch (SQLException e) {
            log.error("Error resetting base locations", e);
            return false;
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}