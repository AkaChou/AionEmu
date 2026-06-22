package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.HouseScriptsDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.gameobjects.player.PlayerScripts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * MySQL 8 implementation of HouseScriptsDAO
 * @author Rolandas, Updated for MySQL 8
 */
public class MySQL8HouseScriptsDAO extends HouseScriptsDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8HouseScriptsDAO.class);

    private static final String MERGE_QUERY = "INSERT INTO `house_scripts` (`house_id`, `index`, `script`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `script` = VALUES(`script`)";
    private static final String DELETE_QUERY = "DELETE FROM `house_scripts` WHERE `house_id` = ? AND `index` = ?";
    private static final String DELETE_ALL_QUERY = "DELETE FROM `house_scripts` WHERE `house_id` = ?";
    private static final String SELECT_QUERY = "SELECT `index`, `script` FROM `house_scripts` WHERE `house_id` = ? ORDER BY `index`";
    private static final String SELECT_COUNT_QUERY = "SELECT COUNT(*) FROM `house_scripts` WHERE `house_id` = ?";

    @Override
    public void addScript(int houseId, int position, String scriptXML) {
        updateScript(houseId, position, scriptXML); // Use merge approach
    }

    @Override
    public PlayerScripts getPlayerScripts(int houseId) {
        PlayerScripts scripts = new PlayerScripts(houseId);
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, houseId);
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int position = rset.getInt("index");
                    String scriptXML = rset.getString("script");
                    scripts.addScript(position, scriptXML);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to load scripts for houseId: {}", houseId, e);
        }

        return scripts;
    }

    @Override
    public void updateScript(int houseId, int position, String scriptXML) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(MERGE_QUERY)) {
            
            stmt.setInt(1, houseId);
            stmt.setInt(2, position);
            
            if (scriptXML == null || scriptXML.isEmpty()) {
                stmt.setNull(3, Types.LONGVARCHAR);
            } else {
                stmt.setString(3, scriptXML);
            }
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Failed to update script for houseId: {}, position: {}", houseId, position, e);
        }
    }

    @Override
    public void deleteScript(int houseId, int position) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, houseId);
            stmt.setInt(2, position);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Failed to delete script for houseId: {}, position: {}", houseId, position, e);
        }
    }

    public void deleteAllScripts(int houseId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_ALL_QUERY)) {
            
            stmt.setInt(1, houseId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Failed to delete all scripts for houseId: {}", houseId, e);
        }
    }

    public int getScriptCount(int houseId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_COUNT_QUERY)) {
            
            stmt.setInt(1, houseId);
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to get script count for houseId: {}", houseId, e);
        }
        
        return 0;
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}