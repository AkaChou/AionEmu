package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerTransformDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * MySQL 8 implementation of PlayerTransformDAO
 * @author Updated for MySQL 8
 */
public class MySQL8PlayerTransfoDAO extends PlayerTransformDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerTransfoDAO.class);

    private static final String INSERT_QUERY = "INSERT INTO `player_transform` (`player_id`, `panel_id`, `item_id`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `panel_id` = VALUES(`panel_id`), `item_id` = VALUES(`item_id`)";
    private static final String SELECT_QUERY = "SELECT `panel_id`, `item_id` FROM `player_transform` WHERE `player_id` = ?";
    private static final String DELETE_QUERY = "DELETE FROM `player_transform` WHERE `player_id` = ?";
    private static final String UPDATE_QUERY = "UPDATE `player_transform` SET `panel_id` = ?, `item_id` = ? WHERE `player_id` = ?";

    @Override
    public void loadPlTransfo(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    int panelId = rset.getInt("panel_id");
                    int itemId = rset.getInt("item_id");
                    
                    player.getTransformModel().setPanelId(panelId);
                    player.getTransformModel().setItemId(itemId);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to load transform data for player: {}", player.getObjectId(), e);
        }
    }

    @Override
    public boolean storePlTransfo(int playerId, int panelId, int itemId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setInt(2, panelId);
            stmt.setInt(3, itemId);
            
            int result = stmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            log.error("Failed to store transform data for player: {}", playerId, e);
            return false;
        }
    }

    @Override
    public boolean deletePlTransfo(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, playerId);
            
            int result = stmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            log.error("Failed to delete transform data for player: {}", playerId, e);
            return false;
        }
    }

    public boolean updatePlTransfo(int playerId, int panelId, int itemId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setInt(1, panelId);
            stmt.setInt(2, itemId);
            stmt.setInt(3, playerId);
            
            int updated = stmt.executeUpdate();
            
            if (updated == 0) {
                return storePlTransfo(playerId, panelId, itemId);
            }
            
            return true;
            
        } catch (SQLException e) {
            log.error("Failed to update transform data for player: {}", playerId, e);
            return false;
        }
    }

    public boolean savePlTransfo(Player player) {
        int panelId = player.getTransformModel().getPanelId();
        int itemId = player.getTransformModel().getItemId();
        
        if (panelId == 0 && itemId == 0) {
            return deletePlTransfo(player.getObjectId());
        }
        
        return updatePlTransfo(player.getObjectId(), panelId, itemId);
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}