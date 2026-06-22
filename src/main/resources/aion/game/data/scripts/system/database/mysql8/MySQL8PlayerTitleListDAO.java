package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerTitleListDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.title.Title;
import com.aionemu.gameserver.model.gameobjects.player.title.TitleList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * MySQL 8 implementation of PlayerTitleListDAO
 * @author xavier, Updated for MySQL 8
 */
public class MySQL8PlayerTitleListDAO extends PlayerTitleListDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerTitleListDAO.class);

    private static final String LOAD_QUERY = "SELECT `title_id`, `remaining` FROM `player_titles` WHERE `player_id` = ? ORDER BY `title_id`";
    private static final String INSERT_QUERY = "INSERT INTO `player_titles` (`player_id`, `title_id`, `remaining`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `remaining` = VALUES(`remaining`)";
    private static final String DELETE_QUERY = "DELETE FROM `player_titles` WHERE `player_id` = ? AND `title_id` = ?";
    private static final String DELETE_ALL_QUERY = "DELETE FROM `player_titles` WHERE `player_id` = ?";
    private static final String DELETE_EXPIRED_QUERY = "DELETE FROM `player_titles` WHERE `remaining` > 0 AND `remaining` < ?";

    @Override
    public TitleList loadTitleList(int playerId) {
        TitleList titleList = new TitleList();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(LOAD_QUERY)) {
            
            stmt.setInt(1, playerId);
            
            try (ResultSet rset = stmt.executeQuery()) {
                long currentTime = System.currentTimeMillis();
                
                while (rset.next()) {
                    int id = rset.getInt("title_id");
                    int remaining = rset.getInt("remaining");
                    
                    // Skip expired titles
                    if (remaining > 0 && remaining <= currentTime) {
                        removeTitle(playerId, id);
                        continue;
                    }
                    
                    titleList.addEntry(id, remaining);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to load titles for player: {}", playerId, e);
        }
        
        return titleList;
    }

    @Override
    public boolean storeTitles(Player player, Title entry) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            stmt.setInt(2, entry.getId());
            stmt.setInt(3, entry.getExpireTime());
            
            int result = stmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            log.error("Failed to store title for player: {}, title: {}", player.getObjectId(), entry.getId(), e);
            return false;
        }
    }

    @Override
    public boolean removeTitle(int playerId, int titleId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setInt(2, titleId);
            
            int result = stmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            log.error("Failed to delete title for player: {}, title: {}", playerId, titleId, e);
            return false;
        }
    }

    public boolean removeAllTitles(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_ALL_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            log.error("Failed to delete all titles for player: {}", playerId, e);
            return false;
        }
    }

    public boolean storeTitles(Player player, TitleList titles) {
        if (titles == null || titles.size() == 0) {
            return true;
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            try (PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
                int batchCount = 0;
                
                for (Title title : titles.getTitles()) {
                    stmt.setInt(1, player.getObjectId());
                    stmt.setInt(2, title.getId());
                    stmt.setInt(3, title.getExpireTime());
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
            return true;
            
        } catch (SQLException e) {
            log.error("Failed to batch store titles for player: {}", player.getObjectId(), e);
            return false;
        }
    }

    public void deleteExpiredTitles() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_EXPIRED_QUERY)) {
            
            stmt.setLong(1, System.currentTimeMillis());
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                log.info("Deleted {} expired player titles", deleted);
            }
            
        } catch (SQLException e) {
            log.error("Failed to delete expired player titles", e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}