package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.EventItemsDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.event.MaxCountOfDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by wanke on 03/03/2017.
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8EventItemsDAO extends EventItemsDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8EventItemsDAO.class);
	
    private static final String INSERT_QUERY = "INSERT INTO `event_items` (`player_id`, `item_id`, `counts`) VALUES (?,?,?) " + "ON DUPLICATE KEY UPDATE `counts` = VALUES(`counts`)";
    private static final String DELETE_QUERY = "DELETE FROM `event_items` WHERE `player_id`=?";
    private static final String DELETE_ITEM_QUERY = "DELETE FROM `event_items` WHERE `item_id`=?";
    private static final String SELECT_QUERY = "SELECT `item_id`, `counts` FROM `event_items` WHERE `player_id`=?";
	
    @Override
    public void loadItems(final Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            
            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int itemId = rset.getInt("item_id");
                    int counts = rset.getInt("counts");
                    player.addItemMaxCountOfDay(itemId, counts);
                }
            }
        } catch (SQLException e) {
            log.error("Error loading event items for player: " + player.getObjectId(), e);
        }
    }
	
    @Override
    public void storeItems(Player player) {
        deleteItems(player);
        
        Map<Integer, MaxCountOfDay> itemsm = player.getItemMaxThisCounts();
        if (itemsm == null || itemsm.isEmpty()) {
            return;
        }
        
        final Iterator<Map.Entry<Integer, MaxCountOfDay>> iterator = itemsm.entrySet().iterator();
        if (!iterator.hasNext()) {
            return;
        }
        
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            try (PreparedStatement st = con.prepareStatement(INSERT_QUERY)) {
                while (iterator.hasNext()) {
                    Map.Entry<Integer, MaxCountOfDay> entry = iterator.next();
                    st.setInt(1, player.getObjectId());
                    st.setInt(2, entry.getKey());
                    st.setInt(3, entry.getValue().getThisCount());
                    st.addBatch();
                }
                st.executeBatch();
            }
            
            con.commit();
            player.clearItemMaxThisCount();
        } catch (SQLException e) {
            log.error("Error while storing event_items for player " + player.getObjectId(), e);
        }
    }
	
    @Override
    public void deleteItems(final int itemId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_ITEM_QUERY)) {
            
            stmt.setInt(1, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error deleting event items for itemId: " + itemId, e);
        }
    }
	
    private void deleteItems(final Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, player.getObjectId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error deleting event items for player: " + player.getObjectId(), e);
        }
    }
	
    @Override
    public boolean supports(String arg0, int arg1, int arg2) {
        return MySQL8DAOUtils.supports(arg0, arg1, arg2);
    }
}