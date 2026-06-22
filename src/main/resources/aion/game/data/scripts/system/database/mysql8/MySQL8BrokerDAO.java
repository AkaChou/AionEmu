package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.BrokerDAO;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.broker.BrokerRace;
import com.aionemu.gameserver.model.gameobjects.BrokerItem;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL 8 implementation of BrokerDAO
 * Fixed connection leaks
 */
public class MySQL8BrokerDAO extends BrokerDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8BrokerDAO.class);

    private static final String SELECT_BROKER_QUERY = "SELECT * FROM broker";
    private static final String SELECT_INVENTORY_QUERY = "SELECT * FROM inventory WHERE `item_location` = 126";
    private static final String INSERT_BROKER_QUERY = "INSERT INTO `broker` " + "(`item_pointer`, `item_id`, `item_count`, `item_creator`, " + "`seller`, `price`, `broker_race`, `expire_time`, `settle_time`, " + "`seller_id`, `is_sold`, `is_settled`, `is_splitsell`) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DELETE_BROKER_QUERY = "DELETE FROM `broker` " + "WHERE `item_pointer` = ? AND `seller_id` = ? AND `expire_time` = ?";
    private static final String UPDATE_BROKER_QUERY = "UPDATE broker SET " + "`is_sold` = ?, `is_settled` = 1, `settle_time` = ? " + "WHERE `item_pointer` = ? AND `expire_time` = ? AND `seller_id` = ? AND `is_settled` = 0";
    private static final String UPDATE_ITEM_QUERY = "UPDATE broker SET " + "`item_count` = ?, `price` = ?, `is_sold` = ?, " + "`is_settled` = ?, `settle_time` = ?, `is_splitsell` = ? " + "WHERE `item_pointer` = ? AND `expire_time` = ? AND `seller_id` = ? AND `is_settled` = 0";
    private static final String PREBUY_CHECK_QUERY = "SELECT 1 FROM broker WHERE `item_pointer` = ? AND `is_sold` = 0 LIMIT 1";

    @Override
    public List<BrokerItem> loadBroker() {
        final List<BrokerItem> brokerItems = new ArrayList<>();
        final List<Item> items = getBrokerItems();
        
        if (items != null && !items.isEmpty()) {
            DAOManager.getDAO(ItemStoneListDAO.class).load(items);
        }

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_BROKER_QUERY);
             ResultSet rset = stmt.executeQuery()) {
            
            while (rset.next()) {
                int itemPointer = rset.getInt("item_pointer");
                int itemId = rset.getInt("item_id");
                long itemCount = rset.getLong("item_count");
                String itemCreator = rset.getString("item_creator");
                String seller = rset.getString("seller");
                int sellerId = rset.getInt("seller_id");
                long price = rset.getLong("price");
                BrokerRace itemBrokerRace = BrokerRace.valueOf(rset.getString("broker_race"));
                Timestamp expireTime = rset.getTimestamp("expire_time");
                Timestamp settleTime = rset.getTimestamp("settle_time");
                int sold = rset.getInt("is_sold");
                int settled = rset.getInt("is_settled");
                int splitSell = rset.getInt("is_splitsell");

                boolean isSold = sold == 1;
                boolean isSettled = settled == 1;
                boolean isSplitSell = splitSell == 1;

                Item item = null;
                if (!isSold) {
                    for (Item brItem : items) {
                        if (itemPointer == brItem.getObjectId()) {
                            item = brItem;
                            break;
                        }
                    }
                }

                brokerItems.add(new BrokerItem(item, itemId, itemPointer, itemCount, itemCreator, price, seller, sellerId, itemBrokerRace, isSold, isSettled, expireTime, settleTime, isSplitSell));
            }
        } catch (SQLException e) {
            log.error("Error loading broker items", e);
        }
        
        return brokerItems;
    }

    private List<Item> getBrokerItems() {
        final List<Item> brokerItems = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_INVENTORY_QUERY);
             ResultSet rset = stmt.executeQuery()) {
            
            while (rset.next()) {
                int itemUniqueId = rset.getInt("item_unique_id");
                int itemId = rset.getInt("item_id");
                long itemCount = rset.getLong("item_count");
                int itemColor = rset.getInt("item_color");
                int colorExpireTime = rset.getInt("color_expires");
                String itemCreator = rset.getString("item_creator");
                int expireTime = rset.getInt("expire_time");
                int activationCount = rset.getInt("activation_count");
                long slot = rset.getLong("slot");
                int location = rset.getInt("item_location");
                int enchant = rset.getInt("enchant");
                int enchantBonus = rset.getInt("enchant_bonus");
                int itemSkin = rset.getInt("item_skin");
                int fusionedItem = rset.getInt("fusioned_item");
                int optionalSocket = rset.getInt("optional_socket");
                int optionalFusionSocket = rset.getInt("optional_fusion_socket");
                int charge = rset.getInt("charge");
                Integer randomNumber = rset.getInt("rnd_bonus");
                int rndCount = rset.getInt("rnd_count");
                int wrappingCount = rset.getInt("wrappable_count");
                int temperingLevel = rset.getInt("tempering_level");
                int reductionLevel = rset.getInt("reduction_level");
                int unSeal = rset.getInt("is_seal");
                boolean isEnhance = rset.getBoolean("isEnhance");
                int enhanceSkillId = rset.getInt("enhanceSkillId");
                int enhanceSkillEnchant = rset.getInt("enhanceSkillEnchant");
                
                brokerItems.add(new Item(itemUniqueId, itemId, itemCount, itemColor, colorExpireTime, itemCreator, expireTime, activationCount, false, false, slot, location, enchant, enchantBonus, itemSkin, fusionedItem, optionalSocket, optionalFusionSocket, charge, randomNumber, rndCount, wrappingCount, false, temperingLevel, false, 0, 0, false, reductionLevel, unSeal, isEnhance, enhanceSkillId, enhanceSkillEnchant));
            }
        } catch (SQLException e) {
            log.error("Error loading broker inventory items", e);
        }
        
        return brokerItems;
    }

    @Override
    public boolean store(BrokerItem item) {
        if (item == null) {
            log.warn("Null broker item on save");
            return false;
        }

        boolean result = false;

        switch (item.getPersistentState()) {
            case NEW:
                result = insertBrokerItem(item);
                if (result && item.getItem() != null) {
                    DAOManager.getDAO(InventoryDAO.class).store(item.getItem(), item.getSellerId());
                }
                break;
            case DELETED:
                result = deleteBrokerItem(item);
                break;
            case UPDATE_ITEM_BROKER:
                result = updateItem(item);
                break;
            case UPDATE_REQUIRED:
                result = updateBrokerItem(item);
                break;
            default:
                return true;
        }

        if (result) {
            item.setPersistentState(PersistentState.UPDATED);
        }
        
        return result;
    }

    private boolean insertBrokerItem(final BrokerItem item) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_BROKER_QUERY)) {
            
            stmt.setInt(1, item.getItemUniqueId());
            stmt.setInt(2, item.getItemId());
            stmt.setLong(3, item.getItemCount());
            stmt.setString(4, item.getItemCreator());
            stmt.setString(5, item.getSeller());
            stmt.setLong(6, item.getPrice());
            stmt.setString(7, String.valueOf(item.getItemBrokerRace()));
            stmt.setTimestamp(8, item.getExpireTime());
            stmt.setTimestamp(9, item.getSettleTime());
            stmt.setInt(10, item.getSellerId());
            stmt.setBoolean(11, item.isSold());
            stmt.setBoolean(12, item.isSettled());
            stmt.setBoolean(13, item.isSplitSell());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error inserting broker item: " + item.getItemUniqueId(), e);
            return false;
        }
    }

    private boolean deleteBrokerItem(final BrokerItem item) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_BROKER_QUERY)) {
            
            stmt.setInt(1, item.getItemUniqueId());
            stmt.setInt(2, item.getSellerId());
            stmt.setTimestamp(3, item.getExpireTime());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error deleting broker item: " + item.getItemUniqueId(), e);
            return false;
        }
    }

    @Override
    public boolean preBuyCheck(int itemForCheck) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement st = con.prepareStatement(PREBUY_CHECK_QUERY)) {
            
            st.setInt(1, itemForCheck);
            try (ResultSet rs = st.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.error("Can't perform prebuy broker check for item: {}", itemForCheck, e);
            return false;
        }
    }

    private boolean updateBrokerItem(final BrokerItem item) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_BROKER_QUERY)) {
            
            stmt.setBoolean(1, item.isSold());
            stmt.setTimestamp(2, item.getSettleTime());
            stmt.setInt(3, item.getItemUniqueId());
            stmt.setTimestamp(4, item.getExpireTime());
            stmt.setInt(5, item.getSellerId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating broker item: " + item.getItemUniqueId(), e);
            return false;
        }
    }
    
    private boolean updateItem(final BrokerItem item) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_ITEM_QUERY)) {
            
            stmt.setLong(1, item.getItemCount());
            stmt.setLong(2, item.getPrice());
            stmt.setBoolean(3, item.isSold());
            stmt.setBoolean(4, item.isSettled());
            stmt.setTimestamp(5, item.getSettleTime());
            stmt.setBoolean(6, item.isSplitSell());
            stmt.setInt(7, item.getItemUniqueId());
            stmt.setTimestamp(8, item.getExpireTime());
            stmt.setInt(9, item.getSellerId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error updating broker item details: " + item.getItemUniqueId(), e);
            return false;
        }
    }

    @Override
    public int[] getUsedIDs() {
        String query = "SELECT id FROM players";
        List<Integer> ids = new ArrayList<>();
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement statement = con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = statement.executeQuery()) {
            
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            log.error("Can't get list of id's from players table", e);
            return new int[0];
        }
        
        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}