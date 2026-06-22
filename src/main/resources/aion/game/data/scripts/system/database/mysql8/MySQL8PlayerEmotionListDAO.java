package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerEmotionListDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.emotion.Emotion;
import com.aionemu.gameserver.model.gameobjects.player.emotion.EmotionList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * MySQL 8 implementation of PlayerEmotionListDAO
 * @author Mr. Poke, Updated for MySQL 8
 */
public class MySQL8PlayerEmotionListDAO extends PlayerEmotionListDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerEmotionListDAO.class);

    private static final String INSERT_QUERY = "INSERT INTO `player_emotions` (`player_id`, `emotion`, `remaining`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `remaining` = VALUES(`remaining`)";
    private static final String SELECT_QUERY = "SELECT `emotion`, `remaining` FROM `player_emotions` WHERE `player_id` = ?";
    private static final String DELETE_QUERY = "DELETE FROM `player_emotions` WHERE `player_id` = ? AND `emotion` = ?";
    private static final String DELETE_ALL_QUERY = "DELETE FROM `player_emotions` WHERE `player_id` = ?";
    private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `player_emotions` WHERE `remaining` > 0 AND `remaining` < ?";

    @Override
    public void loadEmotions(Player player) {
        EmotionList emotions = new EmotionList(player);
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                long currentTime = System.currentTimeMillis();
                
                while (rset.next()) {
                    int emotionId = rset.getInt("emotion");
                    int remaining = rset.getInt("remaining");
                    
                    // Skip expired emotions
                    if (remaining > 0 && remaining <= currentTime) {
                        deleteEmotion(player.getObjectId(), emotionId);
                        continue;
                    }
                    
                    emotions.add(emotionId, remaining, false);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to load emotions for player: {}", player.getObjectId(), e);
        }
        
        player.setEmotions(emotions);
    }

    @Override
    public void insertEmotion(Player player, Emotion emotion) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            stmt.setInt(2, emotion.getId());
            stmt.setInt(3, emotion.getExpireTime());
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Failed to store emotion for player: {}", player.getObjectId(), e);
        }
    }

    @Override
    public void deleteEmotion(int playerId, int emotionId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setInt(2, emotionId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Failed to delete emotion for player: {}, emotion: {}", playerId, emotionId, e);
        }
    }

    public void deleteAllEmotions(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_ALL_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Failed to delete all emotions for player: {}", playerId, e);
        }
    }

    public void deleteExpiredEmotions() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXPIRED_QUERY)) {
            
            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                log.info("Deleted {} expired player emotions", deleted);
            }
            
        } catch (SQLException e) {
            log.error("Failed to delete expired player emotions", e);
        }
    }

    public void insertEmotions(Player player, EmotionList emotions) {
        if (emotions == null || emotions.getEmotions().isEmpty()) {
            return;
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            try (PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
                int batchCount = 0;
                
                for (Emotion emotion : emotions.getEmotions()) {
                    stmt.setInt(1, player.getObjectId());
                    stmt.setInt(2, emotion.getId());
                    stmt.setInt(3, emotion.getExpireTime());
                    stmt.addBatch();
                    batchCount++;
                    
                    if (batchCount % 50 == 0) {
                        stmt.executeBatch();
                    }
                }
                
                if (batchCount % 50 != 0) {
                    stmt.executeBatch();
                }
            }
            
            con.commit();
            
        } catch (SQLException e) {
            log.error("Failed to batch store emotions for player: {}", player.getObjectId(), e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}