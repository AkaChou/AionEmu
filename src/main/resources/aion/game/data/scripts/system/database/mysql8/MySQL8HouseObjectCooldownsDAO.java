package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.HouseObjectCooldownsDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;
import javolution.util.FastMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MySQL 8 implementation of HouseObjectCooldownsDAO
 * @author Rolandas, Updated for MySQL 8
 */
public class MySQL8HouseObjectCooldownsDAO extends HouseObjectCooldownsDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8HouseObjectCooldownsDAO.class);

    private static final String INSERT_QUERY = "INSERT INTO `house_object_cooldowns` (`player_id`, `object_id`, `reuse_time`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `reuse_time` = VALUES(`reuse_time`)";
    private static final String DELETE_QUERY = "DELETE FROM `house_object_cooldowns` WHERE `player_id` = ?";
    private static final String SELECT_QUERY = "SELECT `object_id`, `reuse_time` FROM `house_object_cooldowns` WHERE `player_id` = ?";
    private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `house_object_cooldowns` WHERE `reuse_time` < ?";

    @Override
    public void loadHouseObjectCooldowns(Player player) {
        String query = SELECT_QUERY;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {
            
            stmt.setInt(1, player.getObjectId());
            
            FastMap<Integer, Long> cooldowns = FastMap.newInstance();
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int objectId = rset.getInt("object_id");
                    long reuseTime = rset.getLong("reuse_time");
                    
                    if (reuseTime > System.currentTimeMillis()) {
                        cooldowns.put(objectId, reuseTime);
                    }
                }
            }
            
            player.getHouseObjectCooldownList().setHouseObjectCooldowns(cooldowns);
            
        } catch (SQLException e) {
            log.error("Failed to load house object cooldowns for player: {}", player.getObjectId(), e);
        }
    }

    @Override
    public void storeHouseObjectCooldowns(Player player) {
        Map<Integer, Long> cooldowns = player.getHouseObjectCooldownList().getHouseObjectCooldowns();
        
        if (cooldowns == null || cooldowns.isEmpty()) {
            return;
        }

        String deleteQuery = DELETE_QUERY;
        String insertQuery = INSERT_QUERY;
        
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            try (PreparedStatement deleteStmt = con.prepareStatement(deleteQuery)) {
                deleteStmt.setInt(1, player.getObjectId());
                deleteStmt.executeUpdate();
            }
            
            try (PreparedStatement insertStmt = con.prepareStatement(insertQuery)) {
                long currentTime = System.currentTimeMillis();
                int batchCount = 0;
                
                for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
                    if (entry.getValue() <= currentTime) {
                        continue;
                    }
                    
                    insertStmt.setInt(1, player.getObjectId());
                    insertStmt.setInt(2, entry.getKey());
                    insertStmt.setLong(3, entry.getValue());
                    insertStmt.addBatch();
                    batchCount++;
                    
                    if (batchCount % 100 == 0) {
                        insertStmt.executeBatch();
                    }
                }
                
                if (batchCount % 100 != 0) {
                    insertStmt.executeBatch();
                }
            }
            
            con.commit();
            
        } catch (SQLException e) {
            log.error("Failed to store house object cooldowns for player: {}", player.getObjectId(), e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }

    public void deleteExpiredCooldowns() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXPIRED_QUERY)) {
            
            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                log.info("Deleted {} expired house object cooldowns", deleted);
            }
            
        } catch (SQLException e) {
            log.error("Failed to delete expired house object cooldowns", e);
        }
    }
}