package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerLunaShopDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerLunaShop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Created by wanke on 13/02/2017.
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8PlayerLunaShopDAO extends PlayerLunaShopDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerLunaShopDAO.class);
    
    private static final String ADD_QUERY = "INSERT INTO `player_luna_shop` (`player_id`, `free_under`, `free_munition`, `free_chest`) VALUES (?,?,?,?)";
    private static final String SELECT_QUERY = "SELECT * FROM `player_luna_shop` WHERE `player_id`=?";
    private static final String DELETE_QUERY = "DELETE FROM `player_luna_shop`";
    private static final String UPDATE_QUERY = "UPDATE player_luna_shop SET `free_under`=?, `free_munition`=?, `free_chest`=? WHERE `player_id`=?";

    @Override
    public void load(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    boolean under = rset.getBoolean("free_under");
                    boolean factory = rset.getBoolean("free_munition");
                    boolean chest = rset.getBoolean("free_chest");
                    
                    PlayerLunaShop pls = new PlayerLunaShop(under, factory, chest);
                    pls.setPersistentState(PersistentState.UPDATED);
                    player.setPlayerLunaShop(pls);
                }
            }
        } catch (SQLException e) {
            log.error("Could not restore PlayerLunaShop data for playerObjId: {} from DB", 
                player.getObjectId(), e);
        }
    }

    @Override
    public boolean add(final int playerId, final boolean freeUnderpath, final boolean freeFactory, final boolean freeChest) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(ADD_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setBoolean(2, freeUnderpath);
            stmt.setBoolean(3, freeFactory);
            stmt.setBoolean(4, freeChest);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error adding LunaShop for player {}", playerId, e);
            return false;
        }
    }

    @Override
    public boolean delete() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error deleting all LunaShop data", e);
            return false;
        }
    }

    @Override
    public boolean store(Player player) {
        boolean success = false;
        
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            PlayerLunaShop bind = player.getPlayerLunaShop();
            if (bind != null) {
                switch (bind.getPersistentState()) {
                    case UPDATE_REQUIRED:
                    case NEW:
                        success = updateLunaShop(con, player);
                        log.debug("DB updated for player {}", player.getObjectId());
                        break;
                    default:
                        success = true;
                        break;
                }
                if (success) {
                    bind.setPersistentState(PersistentState.UPDATED);
                }
            }
            con.commit();
        } catch (SQLException e) {
            log.error("Can't open connection to save player updateLunaShop: {}", player.getObjectId(), e);
        }
        return success;
    }

    private boolean updateLunaShop(Connection con, Player player) {
        try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            PlayerLunaShop lr = player.getPlayerLunaShop();
            if (lr == null) {
                return false;
            }
            
            stmt.setBoolean(1, lr.isFreeUnderpath());
            stmt.setBoolean(2, lr.isFreeFactory());
            stmt.setBoolean(3, lr.isFreeChest());
            stmt.setInt(4, player.getObjectId());
            
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Could not update PlayerLunaShop data for player {} from DB", player.getObjectId(), e);
            return false;
        }
    }

    @Override
    public boolean setLunaShopByObjId(int obj, final boolean freeUnderpath, final boolean freeFactory, final boolean freeChest) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setBoolean(1, freeUnderpath);
            stmt.setBoolean(2, freeFactory);
            stmt.setBoolean(3, freeChest);
            stmt.setInt(4, obj);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error setting LunaShop for obj {}", obj, e);
            return false;
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}