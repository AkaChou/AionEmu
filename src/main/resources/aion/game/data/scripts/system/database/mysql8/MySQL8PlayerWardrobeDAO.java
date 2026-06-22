package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerWardrobeDAO;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeEntry;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ranastic
 */
public class MySQL8PlayerWardrobeDAO extends PlayerWardrobeDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerWardrobeDAO.class);
    
    private static final String INSERT_OR_UPDATE = "INSERT INTO `player_wardrobe` (`player_id`, `item_id`, `slot`, `reskin_count`) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE `item_id` = VALUES(`item_id`), `slot` = VALUES(`slot`)";
    
    private static final String SELECT_QUERY = "SELECT `item_id`,`slot`,`reskin_count` FROM `player_wardrobe` WHERE `player_id`=?";
    
    private static final String DELETE_QUERY = "DELETE FROM `player_wardrobe` WHERE `player_id`=? AND `item_id`=?";
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
    
    @Override
    public PlayerWardrobeList load(Player player) {
        List<PlayerWardrobeEntry> w = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int itemId = rset.getInt("item_id");
                    int slot = rset.getInt("slot");
                    int reskin = rset.getInt("reskin_count");
                    w.add(new PlayerWardrobeEntry(itemId, slot, reskin, PersistentState.UPDATED));
                }
            }
        } catch (SQLException e) {
            log.error("Could not restore Wardrobe for playerObjId: {} from DB", player.getObjectId(), e);
        }
        return new PlayerWardrobeList(w);
    }
    
    @Override
    public boolean store(int objectId, int itemId, int slot, int reskin) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_OR_UPDATE)) {
            
            stmt.setInt(1, objectId);
            stmt.setInt(2, itemId);
            stmt.setInt(3, slot);
            stmt.setInt(4, reskin);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Could not store Wardrobe for player {} from DB", objectId, e);
            return false;
        }
    }
    
    @Override
    public boolean delete(int objectId, int itemId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, objectId);
            stmt.setInt(2, itemId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Could not delete Wardrobe for player {} from DB", objectId, e);
            return false;
        }
    }
    
    @Override
    public int getItemSize(int playerObjId) {
        String query = "SELECT COUNT(*) AS `size` FROM `player_wardrobe` WHERE `player_id`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setInt(1, playerObjId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("size");
                }
            }
        } catch (SQLException e) {
            log.debug("Could not get item size for player {}", playerObjId, e);
        }
        return 0;
    }
    
    @Override
    public int getWardrobeItemBySlot(final int obj, int slot) {
        String query = "SELECT `item_id` FROM `player_wardrobe` WHERE `player_id`=? AND `slot`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {
            
            s.setInt(1, obj);
            s.setInt(2, slot);
            
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("item_id");
                }
            }
        } catch (SQLException e) {
            log.debug("No wardrobe item found for player {}, slot {}", obj, slot);
        }
        return 0;
    }
    
    @Override
    public int getReskinCountBySlot(final int obj, int slot) {
        String query = "SELECT `reskin_count` FROM `player_wardrobe` WHERE `player_id`=? AND `slot`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {
            
            s.setInt(1, obj);
            s.setInt(2, slot);
            
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("reskin_count");
                }
            }
        } catch (SQLException e) {
            log.debug("No reskin count found for player {}, slot {}", obj, slot);
        }
        return 0;
    }
    
    @Override
    public boolean setReskinCountBySlot(int obj, int slot, int reskin_count) {
        String query = "UPDATE player_wardrobe SET `reskin_count`=? WHERE `player_id`=? AND `slot`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setInt(1, reskin_count);
            stmt.setInt(2, obj);
            stmt.setInt(3, slot);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error setting reskin count for player {}, slot {}", obj, slot, e);
            return false;
        }
    }
}