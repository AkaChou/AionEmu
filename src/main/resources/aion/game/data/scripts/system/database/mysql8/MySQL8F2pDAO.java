package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.F2pDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.f2p.F2p;
import com.aionemu.gameserver.model.gameobjects.player.f2p.F2pAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * MySQL 8 implementation of F2pDAO
 * @author Updated for MySQL 8
 */
public class MySQL8F2pDAO extends F2pDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8F2pDAO.class);

    private static final String INSERT_QUERY = "INSERT INTO `f2paccount` (`player_id`, `time`) VALUES (?, ?) " + "ON DUPLICATE KEY UPDATE `time` = VALUES(`time`)";
    private static final String SELECT_QUERY = "SELECT `time` FROM `f2paccount` WHERE `player_id` = ?";
    private static final String DELETE_QUERY = "DELETE FROM `f2paccount` WHERE `player_id` = ?";
    private static final String UPDATE_QUERY = "UPDATE `f2paccount` SET `time` = ? WHERE `player_id` = ?";

    @Override
    public void loadF2pInfo(Player player) {
        F2p f2p = new F2p(player);
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    int time = rset.getInt("time");
                    f2p.add(new F2pAccount(time), false);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to load F2P info for player: {}", player.getObjectId(), e);
        }
        
        player.setF2p(f2p);
    }

    @Override
    public boolean storeF2p(int objectId, int time) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, objectId);
            stmt.setInt(2, time);
            
            int result = stmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            log.error("Failed to store F2P for player: {}", objectId, e);
            return false;
        }
    }

    @Override
    public boolean updateF2p(int objectId, int time) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setInt(1, time);
            stmt.setInt(2, objectId);
            
            int updated = stmt.executeUpdate();
            
            if (updated == 0) {
                return storeF2p(objectId, time);
            }
            
            return true;
            
        } catch (SQLException e) {
            log.error("Failed to update F2P for player: {}", objectId, e);
            return false;
        }
    }

    @Override
    public boolean deleteF2p(int objectId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, objectId);
            
            int result = stmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            log.error("Failed to delete F2P for player: {}", objectId, e);
            return false;
        }
    }

    public boolean saveF2p(Player player) {
        F2p f2p = player.getF2p();
        
        if (f2p == null || f2p.getF2pAccount() == null) {
            return deleteF2p(player.getObjectId());
        }
        
        F2pAccount account = f2p.getF2pAccount();
        if (account != null) {
            return updateF2p(player.getObjectId(), account.getRemainingTime());
        }
        
        return true;
    }

    public int getF2pTime(int objectId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, objectId);
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt("time");
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to get F2P time for player: {}", objectId, e);
        }
        
        return 0;
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}