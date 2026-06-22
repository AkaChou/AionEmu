package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerShugoSweepDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerSweep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Created by Wnkrz on 24/10/2017.
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8PlayerShugoSweepDAO extends PlayerShugoSweepDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerShugoSweepDAO.class);
    
    private static final String ADD_QUERY = "INSERT INTO `player_shugo_sweep` (`player_id`, `free_dice`, `sweep_step`, `board_id`) VALUES (?,?,?,?)";
    private static final String SELECT_QUERY = "SELECT * FROM `player_shugo_sweep` WHERE `player_id`=?";
    private static final String DELETE_QUERY = "DELETE FROM `player_shugo_sweep`";
    private static final String UPDATE_QUERY = "UPDATE player_shugo_sweep SET `free_dice`=?, `sweep_step`=?, `board_id`=? WHERE `player_id`=?";
    
    @Override
    public void load(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    int dice = rset.getInt("free_dice");
                    int step = rset.getInt("sweep_step");
                    int boardId = rset.getInt("board_id");
                    
                    PlayerSweep ps = new PlayerSweep(step, dice, boardId);
                    ps.setPersistentState(PersistentState.UPDATED);
                    player.setPlayerShugoSweep(ps);
                }
            }
        } catch (SQLException e) {
            log.error("Could not restore PlayerSweep data for playerObjId: {} from DB", 
                player.getObjectId(), e);
        }
    }
    
    @Override
    public boolean add(final int playerId, final int dice, final int step, final int boardId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(ADD_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setInt(2, dice);
            stmt.setInt(3, step);
            stmt.setInt(4, boardId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error adding ShugoSweep for player {}", playerId, e);
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
            log.error("Error deleting all ShugoSweep data", e);
            return false;
        }
    }
    
    @Override
    public boolean store(Player player) {
        boolean success = false;
        
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            PlayerSweep bind = player.getPlayerShugoSweep();
            if (bind != null) {
                switch (bind.getPersistentState()) {
                    case UPDATE_REQUIRED:
                    case NEW:
                        success = updatePlayerSweep(con, player);
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
            log.error("Can't open connection to save player updateSweep: {}", player.getObjectId(), e);
        }
        return success;
    }
    
    private boolean updatePlayerSweep(Connection con, Player player) {
        try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            PlayerSweep lr = player.getPlayerShugoSweep();
            if (lr == null) {
                return false;
            }
            
            stmt.setInt(1, lr.getFreeDice());
            stmt.setInt(2, lr.getStep());
            stmt.setInt(3, lr.getBoardId());
            stmt.setInt(4, player.getObjectId());
            
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Could not update PlayerSweep data for player {} from DB", player.getObjectId(), e);
            return false;
        }
    }
    
    @Override
    public boolean setShugoSweepByObjId(int obj, final int freeDice, final int step, int boardId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setInt(1, freeDice);
            stmt.setInt(2, step);
            stmt.setInt(3, boardId);
            stmt.setInt(4, obj);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error setting ShugoSweep for obj {}", obj, e);
            return false;
        }
    }
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}