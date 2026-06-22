package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerLifeStatsDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.PlayerLifeStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * MySQL 8 implementation of PlayerLifeStatsDAO
 * @author Mr. Poke, Updated for MySQL 8
 */
public class MySQL8PlayerLifeStatsDAO extends PlayerLifeStatsDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerLifeStatsDAO.class);

    private static final String INSERT_QUERY = "INSERT INTO `player_life_stats` (`player_id`, `hp`, `mp`, `fp`) VALUES (?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE `hp` = VALUES(`hp`), `mp` = VALUES(`mp`), `fp` = VALUES(`fp`)";
    private static final String SELECT_QUERY = "SELECT `hp`, `mp`, `fp` FROM `player_life_stats` WHERE `player_id` = ?";
    private static final String UPDATE_QUERY = "UPDATE `player_life_stats` SET `hp` = ?, `mp` = ?, `fp` = ? WHERE `player_id` = ?";

    @Override
    public void loadPlayerLifeStat(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    PlayerLifeStats lifeStats = player.getLifeStats();
                    lifeStats.setCurrentHp(rset.getInt("hp"));
                    lifeStats.setCurrentMp(rset.getInt("mp"));
                    lifeStats.setCurrentFp(rset.getInt("fp"));
                } else {
                    insertPlayerLifeStat(player);
                }
            }
            
        } catch (SQLException e) {
            log.error("Failed to load life stats for player: {}", player.getObjectId(), e);
            insertPlayerLifeStat(player);
        }
    }

    @Override
    public void insertPlayerLifeStat(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            PlayerLifeStats lifeStats = player.getLifeStats();
            
            stmt.setInt(1, player.getObjectId());
            stmt.setInt(2, lifeStats.getCurrentHp());
            stmt.setInt(3, lifeStats.getCurrentMp());
            stmt.setInt(4, lifeStats.getCurrentFp());
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            log.error("Failed to insert life stats for player: {}", player.getObjectId(), e);
        }
    }

    @Override
    public void updatePlayerLifeStat(Player player) {
        PlayerLifeStats lifeStats = player.getLifeStats();
        int hp = lifeStats.getCurrentHp();
        int mp = lifeStats.getCurrentMp();
        int fp = lifeStats.getCurrentFp();
        
        if (hp < 0) hp = 0;
        if (mp < 0) mp = 0;
        if (fp < 0) fp = 0;
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setInt(1, hp);
            stmt.setInt(2, mp);
            stmt.setInt(3, fp);
            stmt.setInt(4, player.getObjectId());
            
            int updated = stmt.executeUpdate();
            
            if (updated == 0) {
                insertPlayerLifeStat(player);
            }
            
        } catch (SQLException e) {
            log.error("Failed to update life stats for player: {}", player.getObjectId(), e);
        }
    }

    public void updatePlayerLifeStats(Iterable<Player> players) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
                int batchCount = 0;
                
                for (Player player : players) {
                    if (player == null || !player.isOnline()) {
                        continue;
                    }
                    
                    PlayerLifeStats lifeStats = player.getLifeStats();
                    int hp = Math.max(lifeStats.getCurrentHp(), 0);
                    int mp = Math.max(lifeStats.getCurrentMp(), 0);
                    int fp = Math.max(lifeStats.getCurrentFp(), 0);
                    
                    stmt.setInt(1, hp);
                    stmt.setInt(2, mp);
                    stmt.setInt(3, fp);
                    stmt.setInt(4, player.getObjectId());
                    stmt.addBatch();
                    batchCount++;
                    
                    if (batchCount % 100 == 0) {
                        stmt.executeBatch();
                    }
                }
                
                if (batchCount % 100 != 0) {
                    stmt.executeBatch();
                }
            }
            
            con.commit();
            
        } catch (SQLException e) {
            log.error("Failed to batch update player life stats", e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}