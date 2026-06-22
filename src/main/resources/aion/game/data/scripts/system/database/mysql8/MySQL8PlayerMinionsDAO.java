package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerMinionsDAO;
import com.aionemu.gameserver.model.gameobjects.player.MinionCommonData;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.minion.MinionDopingBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Falke_34
 * Updated for MySQL 8 support
 */
public class MySQL8PlayerMinionsDAO extends PlayerMinionsDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8PlayerMinionsDAO.class);

    private static final String INSERT_QUERY = "INSERT INTO player_minions (player_id, object_id, minion_id, name, grade, level, growthpoints, birthday, is_locked, buff_bag) VALUES (?, ?, ?, ?, ?, ?, 0, NOW(), 0, '')";
    private static final String DELETE_QUERY = "DELETE FROM player_minions WHERE player_id = ? AND object_id = ?";
    private static final String SELECT_QUERY = "SELECT * FROM player_minions WHERE player_id = ?";
    private static final String UPDATE_NAME_QUERY = "UPDATE player_minions SET name = ? WHERE player_id = ? AND object_id = ?";
    private static final String UPDATE_GROWTH_QUERY = "UPDATE player_minions SET growthpoints = ? WHERE player_id = ? AND object_id = ?";
    private static final String CHECK_EXIST_QUERY = "SELECT COUNT(*) FROM player_minions WHERE player_id = ? AND object_id = ?";
    private static final String CHECK_EXIST_LIMIT_QUERY = "SELECT 1 FROM player_minions WHERE player_id = ? AND object_id = ? LIMIT 1";
    private static final String EVOLUTION_QUERY = "UPDATE player_minions SET minion_id = ?, growthpoints = 0, level = ? WHERE player_id = ? AND object_id = ?";
    private static final String LOCK_QUERY = "UPDATE player_minions SET is_locked = ? WHERE player_id = ? AND object_id = ?";
    private static final String UPDATE_DOPING_QUERY = "UPDATE player_minions SET buff_bag = ? WHERE player_id = ? AND object_id = ?";
    private static final String SELECT_BIRTHDAY_QUERY = "SELECT birthday FROM player_minions WHERE player_id = ? AND object_id = ?";
    private static final String CLEAN_ORPHANED_QUERY = "DELETE FROM player_minions WHERE player_id NOT IN (SELECT id FROM players)";

    @Override
    public void insertPlayerMinion(MinionCommonData minionCommonData) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            
            stmt.setInt(1, minionCommonData.getMasterObjectId());
            stmt.setInt(2, minionCommonData.getObjectId());
            stmt.setInt(3, minionCommonData.getMinionId());
            stmt.setString(4, minionCommonData.getName());
            stmt.setString(5, minionCommonData.getMinionGrade());
            stmt.setInt(6, minionCommonData.getMinionLevel());
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error inserting new minion #{} [{}]", minionCommonData.getMinionId(), minionCommonData.getName(), e);
        }
    }

    @Override
    public void removePlayerMinion(Player player, int minionObjectId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            stmt.setInt(2, minionObjectId);
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error removing minion #{}", minionObjectId, e);
        }
    }

    @Override
    public List<MinionCommonData> getPlayerMinions(Player player) {
        List<MinionCommonData> minions = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    MinionCommonData minionCommonData = new MinionCommonData(
                        rs.getInt("minion_id"), 
                        player.getObjectId(), 
                        rs.getString("name"), 
                        rs.getString("grade"), 
                        rs.getInt("level"), 
                        rs.getInt("growthpoints")
                    );
                    
                    minionCommonData.setObjectId(rs.getInt("object_id"));
                    minionCommonData.setBirthday(rs.getTimestamp("birthday"));
                    minionCommonData.setLock(rs.getInt("is_locked") == 1);
                    
                    int minionId = rs.getInt("minion_id");
                    if (minionId > 980013) {
                        MinionDopingBag dopingBag = minionCommonData.getDopingBag();
                        if (dopingBag != null) {
                            String bag = rs.getString("buff_bag");
                            if (bag != null && !bag.isEmpty()) {
                                String[] ids = bag.split(",");
                                for (int i = 0; i < ids.length; i++) {
                                    if (i < 5 && !ids[i].isEmpty()) {
                                        try {
                                            int itemId = Integer.parseInt(ids[i]);
                                            dopingBag.setItem(itemId, i);
                                        } catch (NumberFormatException e) {
                                            log.warn("Invalid item ID format for minion {}: {}", minionCommonData.getObjectId(), ids[i]);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    minions.add(minionCommonData);
                }
            }
        } catch (Exception e) {
            log.error("Error getting minions for player {}", player.getObjectId(), e);
        }
        
        return minions;
    }

    @Override
    public void updateMinionName(MinionCommonData minionCommonData) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_NAME_QUERY)) {
            
            stmt.setString(1, minionCommonData.getName());
            stmt.setInt(2, minionCommonData.getMasterObjectId());
            stmt.setInt(3, minionCommonData.getObjectId());
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error updating minion name #{}", minionCommonData.getMinionId(), e);
        }
    }
    
    @Override
    public void updatePlayerMinionGrowthPoint(Player player, MinionCommonData minionCommonData) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_GROWTH_QUERY)) {
            
            stmt.setInt(1, minionCommonData.getMinionGrowthPoint());
            stmt.setInt(2, minionCommonData.getMasterObjectId());
            stmt.setInt(3, minionCommonData.getObjectId());
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error updating minion growth point #{}", minionCommonData.getMinionId(), e);
        }
    }

    @Override
    public boolean PlayerMinions(int playerId, int minionObjId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(CHECK_EXIST_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setInt(2, minionObjId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            log.error("Error checking minion for player {} object {}", playerId, minionObjId, e);
        }
        return false;
    }
    
    public boolean playerHasMinion(int playerid, int minionObjId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(CHECK_EXIST_LIMIT_QUERY)) {
            
            stmt.setInt(1, playerid);
            stmt.setInt(2, minionObjId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            log.error("Error checking minion for player {}", playerid, e);
            return false;
        }
    }

    @Override
    public void evolutionMinion(Player player, MinionCommonData minionCommonData) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(EVOLUTION_QUERY)) {
            
            stmt.setInt(1, minionCommonData.getMinionId());
            stmt.setInt(2, minionCommonData.getMinionLevel());
            stmt.setInt(3, minionCommonData.getMasterObjectId());
            stmt.setInt(4, minionCommonData.getObjectId());
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error evolving minion #{}", minionCommonData.getMinionId(), e);
        }
    }

    @Override
    public void lockMinions(Player player, int minionObjId, int isLocked) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(LOCK_QUERY)) {
            
            stmt.setInt(1, isLocked);
            stmt.setInt(2, player.getObjectId());
            stmt.setInt(3, minionObjId);
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error locking minion #{}", minionObjId, e);
        }
    }
    
    @Override
    public void saveDopingBag(Player player, MinionCommonData minionCommonData, MinionDopingBag bag) {
        if (bag == null) {
            log.warn("Attempted to save null doping bag for minion #{}", minionCommonData.getObjectId());
            return;
        }
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_DOPING_QUERY)) {
            
            StringBuilder itemIds = new StringBuilder();
            
            itemIds.append(bag.getFoodItem() > 0 ? bag.getFoodItem() : "0");
            itemIds.append(",");
            itemIds.append(bag.getDrinkItem() > 0 ? bag.getDrinkItem() : "0");
            
            for (int itemId : bag.getScrollsUsed()) {
                itemIds.append(",").append(itemId > 0 ? itemId : "0");
            }
            
            stmt.setString(1, itemIds.toString());
            stmt.setInt(2, player.getObjectId());
            stmt.setInt(3, minionCommonData.getObjectId());
            stmt.executeUpdate();
            
        } catch (Exception e) {
            log.error("Error updating doping for minion #{}", minionCommonData.getObjectId(), e);
        }
    }
    
    public void saveBirthday(MinionCommonData minionCommonData) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_BIRTHDAY_QUERY)) {
            
            stmt.setInt(1, minionCommonData.getMasterObjectId());
            stmt.setInt(2, minionCommonData.getObjectId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    minionCommonData.setBirthday(rs.getTimestamp("birthday"));
                }
            }
        } catch (Exception e) {
            log.error("Error getting birthday for minion {}", minionCommonData.getMasterObjectId(), e);
        }
    }
    
    public int cleanOrphanedMinions() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(CLEAN_ORPHANED_QUERY)) {
            
            int deleted = stmt.executeUpdate();
            if (deleted > 0) {
                log.info("Cleaned {} orphaned minions from database", deleted);
            }
            return deleted;
        } catch (Exception e) {
            log.error("Error cleaning orphaned minions", e);
            return 0;
        }
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}