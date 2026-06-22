package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.dao.RewardServiceDAO;
import com.aionemu.gameserver.model.templates.rewards.RewardEntryItem;
import javolution.util.FastList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8RewardServiceDAO extends RewardServiceDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8RewardServiceDAO.class);
    
    private static final String UPDATE_QUERY = "UPDATE `web_reward` SET `rewarded`=?, received=NOW() WHERE `unique`=?";
    private static final String UPDATE_QUERY_DOWN = "UPDATE `web_reward` SET `rewarded`=? WHERE `unique`=?";
    private static final String SELECT_QUERY = "SELECT * FROM `web_reward` WHERE `item_owner`=? AND `rewarded`=?";
	
    @Override
    public boolean supports(String arg0, int arg1, int arg2) {
        return MySQL8DAOUtils.supports(arg0, arg1, arg2);
    }
	
    public void setUpdateDown(int unique) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY_DOWN)) {
            
            stmt.setInt(1, 0);
            stmt.setInt(2, unique);
            stmt.executeUpdate();
        } catch (Exception e) {
            log.error("Error setting update down for unique: {}", unique, e);
        }
    }
	
    public boolean setUpdate(int unique) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            stmt.setInt(1, 1);
            stmt.setInt(2, unique);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            log.error("Error setting update for unique: {}", unique, e);
            return false;
        }
    }
	
    @Override
    public FastList<RewardEntryItem> getAvailable(int playerId) {
        FastList<RewardEntryItem> list = FastList.newInstance();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.setInt(2, 0);
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int unique = rset.getInt("unique");
                    int item_id = rset.getInt("item_id");
                    long count = rset.getLong("item_count");
                    list.add(new RewardEntryItem(unique, item_id, count));
                }
            }
        } catch (Exception e) {
            log.error("Error getting available rewards for player: {}", playerId, e);
        }
        return list;
    }
	
    @Override
    public void uncheckAvailable(FastList<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            
            for (int uniqid : ids) {
                stmt.setInt(1, 1);
                stmt.setInt(2, uniqid);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (Exception e) {
            log.error("Error unchecking available rewards", e);
        }
    }
}