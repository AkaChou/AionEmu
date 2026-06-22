package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerCooldownsDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * MySQL 8 implementation of PlayerCooldownsDAO
 * @author nrg, Updated for MySQL 8
 */
public class MySQL8PlayerCooldownsDAO extends PlayerCooldownsDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerCooldownsDAO.class);

    private static final String INSERT_QUERY = "INSERT INTO `player_cooldowns` (`player_id`, `cooldown_id`, `reuse_delay`) " + "VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `reuse_delay` = VALUES(`reuse_delay`)";
    
    private static final String DELETE_QUERY = "DELETE FROM `player_cooldowns` WHERE `player_id` = ?";
    
    private static final String SELECT_QUERY = "SELECT `cooldown_id`, `reuse_delay` FROM `player_cooldowns` WHERE `player_id` = ?";
    
    private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `player_cooldowns` WHERE `reuse_delay` < ?";
    
    private static final String COUNT_QUERY = "SELECT COUNT(*) FROM `player_cooldowns` WHERE `player_id` = ? AND `reuse_delay` > ?";

    private static final Predicate<Long> COOLDOWN_PREDICATE = reuseDelay -> reuseDelay != null && reuseDelay - System.currentTimeMillis() > 28000;

    @Override
    public void loadPlayerCooldowns(Player player) {
        Map<Integer, Long> validCooldowns = new ConcurrentHashMap<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                long currentTime = System.currentTimeMillis();
                
                while (rset.next()) {
                    int cooldownId = rset.getInt("cooldown_id");
                    long reuseDelay = rset.getLong("reuse_delay");
                    
                    if (reuseDelay > currentTime) {
                        validCooldowns.put(cooldownId, reuseDelay);
                    }
                }
            }
            
            for (Map.Entry<Integer, Long> entry : validCooldowns.entrySet()) {
                player.setSkillCoolDown(entry.getKey(), entry.getValue());
            }
            
            log.debug("Loaded {} cooldowns for player {}", validCooldowns.size(), player.getObjectId());
            
        } catch (SQLException e) {
            log.error("Failed to load cooldowns for player: {}", player.getObjectId(), e);
        }
    }

    @Override
    public void storePlayerCooldowns(Player player) {
        Map<Integer, Long> cooldowns = player.getSkillCoolDowns();
        
        if (cooldowns == null || cooldowns.isEmpty()) {
            deletePlayerCooldowns(player);
            return;
        }

        Map<Integer, Long> validCooldowns = new ConcurrentHashMap<>();
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<Integer, Long> entry : cooldowns.entrySet()) {
            Long reuseDelay = entry.getValue();
            if (COOLDOWN_PREDICATE.test(reuseDelay)) {
                validCooldowns.put(entry.getKey(), reuseDelay);
            }
        }

        if (validCooldowns.isEmpty()) {
            deletePlayerCooldowns(player);
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
                final int BATCH_SIZE = 100;
                
                for (Map.Entry<Integer, Long> entry : validCooldowns.entrySet()) {
                    insertStmt.setInt(1, player.getObjectId());
                    insertStmt.setInt(2, entry.getKey());
                    insertStmt.setLong(3, entry.getValue());
                    insertStmt.addBatch();
                    batchCount++;
                    
                    if (batchCount % BATCH_SIZE == 0) {
                        int[] results = insertStmt.executeBatch();
                        log.debug("Executed batch insert of {} cooldowns for player {}", results.length, player.getObjectId());
                    }
                }
                
                if (batchCount % BATCH_SIZE != 0) {
                    int[] results = insertStmt.executeBatch();
                    log.debug("Executed final batch insert of {} cooldowns for player {}", results.length, player.getObjectId());
                }
            }
            
            con.commit();
            log.debug("Stored {} cooldowns for player {}", validCooldowns.size(), player.getObjectId());
            
        } catch (SQLException e) {
            log.error("Failed to store cooldowns for player: {}", player.getObjectId(), e);
        }
    }

    private void deletePlayerCooldowns(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                log.debug("Deleted {} cooldowns for player {}", deleted, player.getObjectId());
            }
            
        } catch (SQLException e) {
            log.error("Failed to delete cooldowns for player: {}", player.getObjectId(), e);
        }
    }

    public int deleteExpiredCooldowns() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXPIRED_QUERY)) {
            
            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                log.info("Deleted {} expired player cooldowns", deleted);
            }
            
            return deleted;
            
        } catch (SQLException e) {
            log.error("Failed to delete expired player cooldowns", e);
            return 0;
        }
    }

    public int getActiveCooldownsCount(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(COUNT_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setLong(2, System.currentTimeMillis());
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to get active cooldowns count for player: {}", playerId, e);
        }
        
        return 0;
    }

    public boolean deletePlayerCooldown(int playerId, int cooldownId) {
        String deleteSpecificQuery = "DELETE FROM `player_cooldowns` WHERE `player_id` = ? AND `cooldown_id` = ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(deleteSpecificQuery)) {
            
            stmt.setInt(1, playerId);
            stmt.setInt(2, cooldownId);
            
            int deleted = stmt.executeUpdate();
            return deleted > 0;
            
        } catch (SQLException e) {
            log.error("Failed to delete cooldown {} for player: {}", cooldownId, playerId, e);
            return false;
        }
    }

    public boolean hasCooldown(int playerId, int cooldownId) {
        String checkQuery = "SELECT COUNT(*) FROM `player_cooldowns` WHERE `player_id` = ? AND `cooldown_id` = ? AND `reuse_delay` > ?";
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(checkQuery)) {
            
            stmt.setInt(1, playerId);
            stmt.setInt(2, cooldownId);
            stmt.setLong(3, System.currentTimeMillis());
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt(1) > 0;
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to check cooldown {} for player: {}", cooldownId, playerId, e);
        }
        
        return false;
    }

    public void storeMultiplePlayersCooldowns(Map<Player, Map<Integer, Long>> playersCooldowns) {
        if (playersCooldowns == null || playersCooldowns.isEmpty()) {
            return;
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            try (PreparedStatement deleteStmt = con.prepareStatement(DELETE_QUERY)) {
                for (Player player : playersCooldowns.keySet()) {
                    deleteStmt.setInt(1, player.getObjectId());
                    deleteStmt.addBatch();
                }
                deleteStmt.executeBatch();
            }
            
            try (PreparedStatement insertStmt = con.prepareStatement(INSERT_QUERY)) {
                int batchCount = 0;
                
                for (Map.Entry<Player, Map<Integer, Long>> playerEntry : playersCooldowns.entrySet()) {
                    Player player = playerEntry.getKey();
                    Map<Integer, Long> cooldowns = playerEntry.getValue();
                    
                    if (cooldowns == null || cooldowns.isEmpty()) {
                        continue;
                    }
                    
                    long currentTime = System.currentTimeMillis();
                    
                    for (Map.Entry<Integer, Long> cooldownEntry : cooldowns.entrySet()) {
                        Long reuseDelay = cooldownEntry.getValue();
                        if (reuseDelay != null && reuseDelay > currentTime) {
                            insertStmt.setInt(1, player.getObjectId());
                            insertStmt.setInt(2, cooldownEntry.getKey());
                            insertStmt.setLong(3, reuseDelay);
                            insertStmt.addBatch();
                            batchCount++;
                            
                            if (batchCount % 1000 == 0) {
                                insertStmt.executeBatch();
                            }
                        }
                    }
                }
                
                if (batchCount % 1000 != 0) {
                    insertStmt.executeBatch();
                }
            }
            
            con.commit();
            log.info("Successfully stored cooldowns for {} players", playersCooldowns.size());
            
        } catch (SQLException e) {
            log.error("Failed to store multiple players cooldowns", e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}