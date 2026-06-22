package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.AbyssLandingDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.landing.LandingLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MySQL8AbyssLandingDAO extends AbyssLandingDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8AbyssLandingDAO.class);
    
    private static final String SELECT_QUERY = "SELECT * FROM `abyss_landing`";
    private static final String UPDATE_QUERY = "UPDATE `abyss_landing` SET `level` = ?, `siege` = ?, `commander` = ?, `artefact` = ?, `base` = ?, `monuments` = ?, `quest` = ?, `facility` = ?, `points` = ? WHERE `id` = ?";
    private static final String INSERT_QUERY = "INSERT INTO `abyss_landing` (`id`, `level`, `siege`, `commander`, `artefact`, `base`, `monuments`, `quest`, `facility`, `level_up_date`, `race`, `points`) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    @Override
    public void store(LandingLocation location) {
        updateLandingLocation(location);
    }
    
    @Override
    public boolean loadLandingLocations(final Map<Integer, LandingLocation> locations) {
        boolean success = true;
        List<Integer> loaded = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
             ResultSet resultSet = stmt.executeQuery()) {
            
            while (resultSet.next()) {
                int locationId = resultSet.getInt("id");
                LandingLocation loc = locations.get(locationId);
                if (loc != null) {
                    loc.setLevel(resultSet.getInt("level"));
                    loc.setPoints(resultSet.getInt("points"));
                    loc.setArtifactPoints(resultSet.getInt("artefact"));
                    loc.setBasePoints(resultSet.getInt("base"));
                    loc.setCommanderPoints(resultSet.getInt("commander"));
                    loc.setQuestPoints(resultSet.getInt("quest"));
                    loc.setFacilityPoints(resultSet.getInt("facility"));
                    loc.setSiegePoints(resultSet.getInt("siege"));
                    loc.setMonumentsPoints(resultSet.getInt("monuments"));
                    loaded.add(locationId);
                }
            }
        } catch (Exception e) {
            log.warn("Error loading Landing information from database", e);
            success = false;
        }
        
        for (Map.Entry<Integer, LandingLocation> entry : locations.entrySet()) {
            LandingLocation sLoc = entry.getValue();
            if (!loaded.contains(sLoc.getId())) {
                insertLandingLocation(sLoc);
            }
        }
        return success;
    }
    
    @Override
    public boolean updateLandingLocation(final LandingLocation locations) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setInt(1, locations.getLevel());
            stmt.setInt(2, locations.getSiegePoints());
            stmt.setInt(3, locations.getCommanderPoints());
            stmt.setInt(4, locations.getArtifactPoints());
            stmt.setInt(5, locations.getBasePoints());
            stmt.setInt(6, locations.getMonumentsPoints());
            stmt.setInt(7, locations.getQuestPoints());
            stmt.setInt(8, locations.getFacilityPoints());
            stmt.setInt(9, locations.getPoints());
            stmt.setInt(10, locations.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Error update Abyss Landing Location: id: {}", locations.getId(), e);
            return false;
        }
    }
    
    private boolean insertLandingLocation(final LandingLocation locations) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, locations.getId());
            stmt.setInt(2, locations.getLevel());
            stmt.setInt(3, locations.getSiegePoints());
            stmt.setInt(4, locations.getCommanderPoints());
            stmt.setInt(5, locations.getArtifactPoints());
            stmt.setInt(6, locations.getBasePoints());
            stmt.setInt(7, locations.getMonumentsPoints());
            stmt.setInt(8, locations.getQuestPoints());
            stmt.setInt(9, locations.getFacilityPoints());
            stmt.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
            stmt.setString(11, locations.getTemplate().getRace().toString());
            stmt.setInt(12, locations.getPoints());
            
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Error insert Abyss Landing Location: {}", locations.getId(), e);
            return false;
        }
    }
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}