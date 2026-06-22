package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerABDAO;
import com.aionemu.gameserver.model.atreian_bestiary.PlayerABEntry;
import com.aionemu.gameserver.model.atreian_bestiary.PlayerABList;
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
public class MySQL8PlayerAtreianBestiaryDAO extends PlayerABDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerAtreianBestiaryDAO.class);
    
    private static final String INSERT_OR_UPDATE = "INSERT INTO `player_atreian_bestiary` (`player_id`, `id`, `kill_count`, `level`, `claim_reward`) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE `id` = VALUES(`id`), `kill_count` = VALUES(`kill_count`), `level` = VALUES(`level`), `claim_reward` = VALUES(`claim_reward`)";
    
    private static final String SELECT_QUERY = "SELECT `id`,`kill_count`,`level`,`claim_reward` FROM `player_atreian_bestiary` WHERE `player_id`=?";
    
    private static final String DELETE_QUERY = "DELETE FROM `player_atreian_bestiary` WHERE `player_id`=? AND `id`=?";
    
    @Override
    public PlayerABList load(Player player) {
        List<PlayerABEntry> cp = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("id");
                    int kill_count = rset.getInt("kill_count");
                    int level = rset.getInt("level");
                    int claimReward = rset.getInt("claim_reward");
                    
                    cp.add(new PlayerABEntry(id, kill_count, level, claimReward, PersistentState.UPDATED));
                }
            }
        } catch (SQLException e) {
            log.error("Could not restore Atreian Bestiary for playerObjId: {} from DB", 
                player.getObjectId(), e);
        }
        return new PlayerABList(cp);
    }
    
    @Override
    public boolean store(int objectId, int id, int kill_count, int level, int claimReward) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_OR_UPDATE)) {
            
            stmt.setInt(1, objectId);
            stmt.setInt(2, id);
            stmt.setInt(3, kill_count);
            stmt.setInt(4, level);
            stmt.setInt(5, claimReward);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Could not store Atreian Bestiary for player {} from DB", objectId, e);
            return false;
        }
    }
    
    @Override
    public boolean delete(int playerObjId, int id) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, playerObjId);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Could not delete Atreian Bestiary for player {} from DB", playerObjId, e);
            return false;
        }
    }
    
    @Override
    public int getKillCountById(final int playerObjId, final int id) {
        String query = "SELECT `kill_count` FROM `player_atreian_bestiary` WHERE `player_id`=? AND `id`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {
            
            s.setInt(1, playerObjId);
            s.setInt(2, id);
            
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("kill_count");
                }
            }
        } catch (SQLException e) {
            log.debug("No kill count found for player {}, id {}", playerObjId, id);
        }
        return 0;
    }
    
    @Override
    public int getLevelById(final int playerObjId, final int id) {
        String query = "SELECT `level` FROM `player_atreian_bestiary` WHERE `player_id`=? AND `id`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {
            
            s.setInt(1, playerObjId);
            s.setInt(2, id);
            
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("level");
                }
            }
        } catch (SQLException e) {
            log.debug("No level found for player {}, id {}", playerObjId, id);
        }
        return 0;
    }
    
    @Override
    public int getClaimRewardById(int playerObjId, int id) {
        String query = "SELECT `claim_reward` FROM `player_atreian_bestiary` WHERE `player_id`=? AND `id`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {
            
            s.setInt(1, playerObjId);
            s.setInt(2, id);
            
            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("claim_reward");
                }
            }
        } catch (SQLException e) {
            log.debug("No claim reward found for player {}, id {}", playerObjId, id);
        }
        return 0;
    }
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}