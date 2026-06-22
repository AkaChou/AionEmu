package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MotionDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.motion.Motion;
import com.aionemu.gameserver.model.gameobjects.player.motion.MotionList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author MrPoke
 * @rework MATTY
 */
public class MySQL8MotionDAO extends MotionDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8MotionDAO.class);
    
    private static final String INSERT_QUERY = "INSERT INTO `player_motions` (`player_id`, `motion_id`, `active`, `time`) VALUES (?,?,?,?)";
    private static final String SELECT_QUERY = "SELECT `motion_id`, `active`, `time` FROM `player_motions` WHERE `player_id`=?";
    private static final String DELETE_QUERY = "DELETE FROM `player_motions` WHERE `player_id`=? AND `motion_id`=?";
    private static final String UPDATE_QUERY = "UPDATE `player_motions` SET `active`=? WHERE `player_id`=? AND `motion_id`=?";

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }

    @Override
    public void loadMotionList(Player player) {
        MotionList motions = new MotionList(player);
        List<Motion> loadedMotions = loadMotions(player.getObjectId());
        
        if (loadedMotions != null) {
            for (Motion motion : loadedMotions) {
                motions.add(motion, false);
            }
        }
        player.setMotions(motions);
    }
    
    @Override
    public List<Motion> loadMotions(Integer playerId) {
        List<Motion> motions = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, playerId);
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int motionId = rset.getInt("motion_id");
                    int time = rset.getInt("time");
                    boolean isActive = rset.getBoolean("active");
                    motions.add(new Motion(motionId, time, isActive));
                }
            }
        } catch (SQLException e) {
            log.error("Could not load motions for playerObjId: {} from DB", playerId, e);
        }
        return motions;
    }

    @Override
    public boolean storeMotion(int objectId, Motion motion) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, objectId);
            stmt.setInt(2, motion.getId());
            stmt.setBoolean(3, motion.isActive());
            stmt.setInt(4, motion.getExpireTime());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Could not store motion for player {} from DB", objectId, e);
            return false;
        }
    }

    @Override
    public boolean deleteMotion(int objectId, int motionId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, objectId);
            stmt.setInt(2, motionId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Could not delete motion for player {} from DB", objectId, e);
            return false;
        }
    }

    @Override
    public boolean updateMotion(int objectId, Motion motion) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setBoolean(1, motion.isActive());
            stmt.setInt(2, objectId);
            stmt.setInt(3, motion.getId());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Could not update motion for player {} from DB", objectId, e);
            return false;
        }
    }
}