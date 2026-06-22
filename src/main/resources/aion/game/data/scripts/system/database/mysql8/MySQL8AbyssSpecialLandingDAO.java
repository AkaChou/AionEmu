package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.AbyssSpecialLandingDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.model.landing_special.LandingSpecialStateType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MySQL8AbyssSpecialLandingDAO extends AbyssSpecialLandingDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8AbyssSpecialLandingDAO.class);
	
    private static final String SELECT_QUERY = "SELECT * FROM `special_landing`";
    private static final String UPDATE_QUERY = "UPDATE `special_landing` SET `type` = ? WHERE `id` = ?";
    private static final String INSERT_QUERY = "INSERT INTO `special_landing` (`id`, `type`) VALUES(?, ?)";
	
    @Override
    public boolean loadLandingSpecialLocations(final Map<Integer, LandingSpecialLocation> locations) {
        boolean success = true;
        List<Integer> loaded = new ArrayList<Integer>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
             ResultSet resultSet = stmt.executeQuery()) {
            
            while (resultSet.next()) {
                LandingSpecialLocation loc = locations.get(resultSet.getInt("id"));
                if (loc != null) {
                    loc.setType(LandingSpecialStateType.valueOf(resultSet.getString("type")));
                    loaded.add(loc.getId());
                }
            }
        } catch (Exception e) {
            log.warn("Error loading Siege information from database", e);
            success = false;
        } 
        
        for (Map.Entry<Integer, LandingSpecialLocation> entry : locations.entrySet()) {
            LandingSpecialLocation sLoc = entry.getValue();
            if (!loaded.contains(sLoc.getId())) {
                insertLandingSpecialLocation(sLoc);
            }
        }
        return success;
    }
	
    @Override
    public void store(LandingSpecialLocation location) {
        updateLandingSpecialLocation(location);
    }
	
    @Override
    public boolean updateLandingSpecialLocation(final LandingSpecialLocation locations) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setString(1, locations.getType().toString());
            stmt.setInt(2, locations.getId());
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Error update special Landing Location: id: " + locations.getId(), e);
            return false;
        }
    }
	
    private boolean insertLandingSpecialLocation(final LandingSpecialLocation locations) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, locations.getId());
            stmt.setString(2, LandingSpecialStateType.NO_ACTIVE.toString());
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Error insert special Landing Location: " + locations.getId(), e);
            return false;
        }
    }
	
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}