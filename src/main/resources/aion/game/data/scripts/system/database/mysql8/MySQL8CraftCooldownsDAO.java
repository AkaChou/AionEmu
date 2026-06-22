package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.CraftCooldownsDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;
import javolution.util.FastMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MySQL 8 implementation of CraftCooldownsDAO
 * @author synchro2, Updated for MySQL 8
 */
public class MySQL8CraftCooldownsDAO extends CraftCooldownsDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8CraftCooldownsDAO.class);

    private static final String INSERT_QUERY = "INSERT INTO `craft_cooldowns` (`player_id`, `delay_id`, `reuse_time`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `reuse_time` = VALUES(`reuse_time`)";
    private static final String DELETE_QUERY = "DELETE FROM `craft_cooldowns` WHERE `player_id` = ?";
    private static final String SELECT_QUERY = "SELECT `delay_id`, `reuse_time` FROM `craft_cooldowns` WHERE `player_id` = ?";
    private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `craft_cooldowns` WHERE `reuse_time` < ?";

    @Override
    public void loadCraftCooldowns(Player player) {
        FastMap<Integer, Long> cooldowns = FastMap.newInstance();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                long currentTime = System.currentTimeMillis();
                
                while (rset.next()) {
                    int delayId = rset.getInt("delay_id");
                    long reuseTime = rset.getLong("reuse_time");
                    
                    if (reuseTime > currentTime) {
                        cooldowns.put(delayId, reuseTime);
                    }
                }
            }
            
            player.getCraftCooldownList().setCraftCoolDowns(cooldowns);
            
        } catch (SQLException e) {
            log.error("Failed to load craft cooldowns for player: {}", player.getObjectId(), e);
        }
    }

    @Override
    public void storeCraftCooldowns(Player player) {
        Map<Integer, Long> cooldowns = player.getCraftCooldownList().getCraftCoolDowns();
        
        if (cooldowns == null || cooldowns.isEmpty()) {
            return;
        }

        Map<Integer, Long> validCooldowns = new ConcurrentHashMap<>();
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
            if (entry.getValue() > currentTime) {
                validCooldowns.put(entry.getKey(), entry.getValue());
            }
        }

        if (validCooldowns.isEmpty()) {
            deleteCraftCoolDowns(player);
            return;
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            try (PreparedStatement deleteStmt = con.prepareStatement(DELETE_QUERY)) {
                deleteStmt.setInt(1, player.getObjectId());
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = con.prepareStatement(INSERT_QUERY)) {
                int batchCount = 0;
                
                for (Map.Entry<Integer, Long> entry : validCooldowns.entrySet()) {
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
            log.error("Failed to store craft cooldowns for player: {}", player.getObjectId(), e);
        }
    }

    private void deleteCraftCoolDowns(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Failed to delete craft cooldowns for player: {}", player.getObjectId(), e);
        }
    }

    public void deleteExpiredCraftCooldowns() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXPIRED_QUERY)) {
            
            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                log.info("Deleted {} expired craft cooldowns", deleted);
            }
            
        } catch (SQLException e) {
            log.error("Failed to delete expired craft cooldowns", e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}