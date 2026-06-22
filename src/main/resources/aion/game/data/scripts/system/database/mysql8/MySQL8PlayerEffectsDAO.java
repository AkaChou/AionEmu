package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerEffectsDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.model.Effect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * MySQL 8 implementation of PlayerEffectsDAO
 * @author ATracer, Updated for MySQL 8
 */
public class MySQL8PlayerEffectsDAO extends PlayerEffectsDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerEffectsDAO.class);

    private static final String INSERT_QUERY = "INSERT INTO `player_effects` (`player_id`, `skill_id`, `skill_lvl`, `current_time`, `end_time`) " + "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " + "`skill_lvl` = VALUES(`skill_lvl`), `current_time` = VALUES(`current_time`), `end_time` = VALUES(`end_time`)";
    private static final String DELETE_QUERY = "DELETE FROM `player_effects` WHERE `player_id` = ?";
    private static final String SELECT_QUERY = "SELECT `skill_id`, `skill_lvl`, `current_time`, `end_time` FROM `player_effects` WHERE `player_id` = ?";
    private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `player_effects` WHERE `end_time` < ?";

    private static final Predicate<Effect> INSERTABLE_EFFECTS_PREDICATE = effect -> effect != null && effect.getRemainingTime() > 28000;

    @Override
    public void loadPlayerEffects(Player player) {
        List<SavedEffect> savedEffects = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int skillId = rset.getInt("skill_id");
                    int skillLvl = rset.getInt("skill_lvl");
                    int remainingTime = rset.getInt("current_time");
                    long endTime = rset.getLong("end_time");
                    
                    if (remainingTime > 0 && endTime > System.currentTimeMillis()) {
                        savedEffects.add(new SavedEffect(skillId, skillLvl, remainingTime, endTime));
                    }
                }
            }
            
            for (SavedEffect effect : savedEffects) {
                player.getEffectController().addSavedEffect(
                    effect.skillId, 
                    effect.skillLvl, 
                    effect.remainingTime, 
                    effect.endTime
                );
            }
            
        } catch (SQLException e) {
            log.error("Failed to load effects for player: {}", player.getObjectId(), e);
        }
        
        player.getEffectController().broadCastEffects();
    }

    @Override
    public void storePlayerEffects(Player player) {
        List<Effect> validEffects = new ArrayList<>();
        
        for (Effect effect : player.getEffectController().getAbnormalEffectsToShow()) {
            if (INSERTABLE_EFFECTS_PREDICATE.test(effect)) {
                validEffects.add(effect);
            }
        }

        if (validEffects.isEmpty()) {
            deletePlayerEffects(player);
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
                
                for (Effect effect : validEffects) {
                    insertStmt.setInt(1, player.getObjectId());
                    insertStmt.setInt(2, effect.getSkillId());
                    insertStmt.setInt(3, effect.getSkillLevel());
                    insertStmt.setInt(4, effect.getRemainingTime());
                    insertStmt.setLong(5, effect.getEndTime());
                    insertStmt.addBatch();
                    batchCount++;
                    
                    if (batchCount % 50 == 0) {
                        insertStmt.executeBatch();
                    }
                }
                
                if (batchCount % 50 != 0) {
                    insertStmt.executeBatch();
                }
            }
            
            con.commit();
            
        } catch (SQLException e) {
            log.error("Failed to store effects for player: {}", player.getObjectId(), e);
        }
    }

    private void deletePlayerEffects(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Failed to delete effects for player: {}", player.getObjectId(), e);
        }
    }

    public void deleteExpiredEffects() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXPIRED_QUERY)) {
            
            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                log.info("Deleted {} expired player effects", deleted);
            }
            
        } catch (SQLException e) {
            log.error("Failed to delete expired player effects", e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }

    private static class SavedEffect {
        final int skillId;
        final int skillLvl;
        final int remainingTime;
        final long endTime;

        SavedEffect(int skillId, int skillLvl, int remainingTime, long endTime) {
            this.skillId = skillId;
            this.skillLvl = skillLvl;
            this.remainingTime = remainingTime;
            this.endTime = endTime;
        }
    }
}