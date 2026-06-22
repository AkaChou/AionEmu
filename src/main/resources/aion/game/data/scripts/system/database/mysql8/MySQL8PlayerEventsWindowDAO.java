package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.PlayerEventsWindowDAO;
import com.aionemu.gameserver.model.event_window.PlayerEventWindowEntry;
import com.aionemu.gameserver.model.event_window.PlayerEventWindowList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ghostfur (Aion-Unique)
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8PlayerEventsWindowDAO extends PlayerEventsWindowDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerEventsWindowDAO.class);
    
    private static final String SELECT_QUERY = "SELECT * FROM `player_events_window` WHERE `account_id`=?";
    private static final String INSERT_QUERY = "INSERT INTO `player_events_window` (`account_id`, `event_id`, `last_stamp`, `elapsed`) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE `event_id` = VALUES(`event_id`), `last_stamp` = VALUES(`last_stamp`)";
    private static final String INSERT_SIMPLE_QUERY = "INSERT INTO `player_events_window` (`account_id`, `event_id`, `last_stamp`) VALUES (?,?,?)";
    private static final String DELETE_QUERY = "DELETE FROM player_events_window WHERE account_id = ? AND event_id = ?";
    private static final String SELECT_IDS_QUERY = "SELECT event_id FROM player_events_window WHERE account_id = ?";
    private static final String SELECT_LAST_STAMP_QUERY = "SELECT last_stamp FROM player_events_window WHERE account_id = ? AND event_id = ?";
    private static final String SELECT_ELAPSED_QUERY = "SELECT elapsed FROM player_events_window WHERE account_id = ? AND event_id = ?";
    private static final String UPDATE_ELAPSED_QUERY = "UPDATE player_events_window SET elapsed = ? WHERE account_id = ? AND event_id = ?";
    private static final String SELECT_REWARD_COUNT_QUERY = "SELECT reward_recived_count FROM player_events_window WHERE account_id = ? AND event_id = ?";
    private static final String UPDATE_REWARD_QUERY = "UPDATE player_events_window SET reward_recived_count = ?, elapsed = 0, last_stamp = NOW() WHERE account_id = ? AND event_id = ?";
    
    @Override
    public PlayerEventWindowList load(Player player) {
        List<PlayerEventWindowEntry> eventWindow = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getPlayerAccount().getId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("event_id");
                    Timestamp lastStamp = rset.getTimestamp("last_stamp");
                    int elapsed = rset.getInt("elapsed");
                    eventWindow.add(new PlayerEventWindowEntry(id, lastStamp, elapsed, PersistentState.UPDATED));
                }
            }
        } catch (SQLException e) {
            log.error("Could not restore Event Window account: {} from DB", player.getObjectId(), e);
        }
        return new PlayerEventWindowList(eventWindow);
    }
    
    @Override
    public boolean store(int accountId, int eventId, Timestamp last_stamp, int elapsed) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, accountId);
            stmt.setInt(2, eventId);
            stmt.setTimestamp(3, last_stamp);
            stmt.setInt(4, elapsed);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Could not store event window for account {} from DB", accountId, e);
            return false;
        }
    }

    @Override
    public void insert(int accountId, int eventId, Timestamp last_stamp) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_SIMPLE_QUERY)) {
            
            stmt.setInt(1, accountId);
            stmt.setInt(2, eventId);
            stmt.setTimestamp(3, last_stamp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Can't insert into events window", e);
        }
    }

    @Override
    public void delete(final int accountId, final int eventId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, accountId);
            stmt.setInt(2, eventId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error deleting event window entry for account {} event {}", accountId, eventId, e);
        }
    }

    @Override
    public List<Integer> getEventsWindow(final int accountId) {
        final List<Integer> ids = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_IDS_QUERY)) {
            
            stmt.setInt(1, accountId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("event_id"));
                }
            }
        } catch (SQLException e) {
            log.error("Error getting events window for account {}", accountId, e);
        }
        return ids;
    }

    @Override
    public Timestamp getLastStamp(int accountId, int eventId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LAST_STAMP_QUERY)) {
            
            stmt.setInt(1, accountId);
            stmt.setInt(2, eventId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getTimestamp("last_stamp");
                }
            }
        } catch (SQLException e) {
            log.error("Can't get last received Stamp for account {} event {}", accountId, eventId, e);
        }
        return new Timestamp(System.currentTimeMillis());
    }

    @Override
    public int getElapsed(int accountId, int eventId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_ELAPSED_QUERY)) {
            
            stmt.setInt(1, accountId);
            stmt.setInt(2, eventId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("elapsed");
                }
            }
        } catch (SQLException e) {
            log.debug("No elapsed time found for account {}, event {}", accountId, eventId);
        }
        return 0;
    }

    @Override
    public void updateElapsed(int accountId, int eventId, int elapsed) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_ELAPSED_QUERY)) {
            
            stmt.setInt(1, elapsed);
            stmt.setInt(2, accountId);
            stmt.setInt(3, eventId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error updating elapsed for account {} event {}", accountId, eventId, e);
        }
    }
    
    @Override
    public int getRewardRecivedCount(int accountId, int eventId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_REWARD_COUNT_QUERY)) {
            
            stmt.setInt(1, accountId);
            stmt.setInt(2, eventId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("reward_recived_count ");
                }
            }
        } catch (SQLException e) {
            log.debug("No reward count found for account {}, event {}", accountId, eventId);
        }
        return 0;
    }
    
    @Override
    public void setRewardRecivedCount(int accountId, int eventId, int rewardReceivedCount) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_REWARD_QUERY)) {
            
            stmt.setInt(1, rewardReceivedCount);
            stmt.setInt(2, accountId);
            stmt.setInt(3, eventId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Can't update reward received count for account {} event {}", accountId, eventId, e);
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}