package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.InGameShopDAO;
import com.aionemu.gameserver.dao.MySQL8DAOUtils;
import com.aionemu.gameserver.model.ingameshop.IGItem;
import javolution.util.FastMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xTz
 */
public class MySQL8InGameShopDAO extends InGameShopDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8InGameShopDAO.class);
    
    private static final String SELECT_QUERY = "SELECT `object_id`, `item_id`, `item_count`, `item_price`, `category`, `sub_category`, `list`, `sales_ranking`, `item_type`, `gift`, `title_description`, `description` FROM `ingameshop`";
    
    private static final String DELETE_QUERY = "DELETE FROM `ingameshop` WHERE `item_id`=? AND `category`=? AND `sub_category`=? AND `list`=?";
    
    private static final String UPDATE_SALES_QUERY = "UPDATE `ingameshop` SET `sales_ranking`=? WHERE `object_id`=?";

    @Override
    public FastMap<Byte, List<IGItem>> loadInGameShopItems() {
        FastMap<Byte, List<IGItem>> items = FastMap.newInstance();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
             ResultSet rset = stmt.executeQuery()) {
            
            while (rset.next()) {
                byte category = rset.getByte("category");
                byte subCategory = rset.getByte("sub_category");
                
                if (subCategory < 3) {
                    continue;
                }
                
                int objectId = rset.getInt("object_id");
                int itemId = rset.getInt("item_id");
                long itemCount = rset.getLong("item_count");
                long itemPrice = rset.getLong("item_price");
                int list = rset.getInt("list");
                int salesRanking = rset.getInt("sales_ranking");
                byte itemType = rset.getByte("item_type");
                byte gift = rset.getByte("gift");
                String titleDescription = rset.getString("title_description");
                String description = rset.getString("description");
                
                if (!items.containsKey(category)) {
                    items.put(category, new ArrayList<>());
                }
                
                items.get(category).add(new IGItem(objectId, itemId, itemCount, itemPrice, category, subCategory, list, salesRanking, itemType, gift, titleDescription, description
                ));
            }
        } catch (SQLException e) {
            log.error("Could not restore inGameShop data from DB", e);
        }
        return items;
    }

    @Override
    public boolean deleteIngameShopItem(int itemId, byte category, byte subCategory, int list) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            
            stmt.setInt(1, itemId);
            stmt.setInt(2, category);
            stmt.setInt(3, subCategory);
            stmt.setInt(4, list);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error delete ingameshopItem: {}", itemId, e);
            return false;
        }
    }

    @Override
    public void saveIngameShopItem(int objectId, int itemId, long itemCount, long itemPrice, byte category, byte subCategory, int list, int salesRanking, byte itemType, byte gift, String titleDescription, String description) {
        String insertQuery = "INSERT INTO ingameshop(object_id, item_id, item_count, item_price, category, sub_category, list, sales_ranking, item_type, gift, title_description, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(insertQuery)) {
            
            stmt.setInt(1, objectId);
            stmt.setInt(2, itemId);
            stmt.setLong(3, itemCount);
            stmt.setLong(4, itemPrice);
            stmt.setByte(5, category);
            stmt.setByte(6, subCategory);
            stmt.setInt(7, list);
            stmt.setInt(8, salesRanking);
            stmt.setByte(9, itemType);
            stmt.setByte(10, gift);
            stmt.setString(11, titleDescription);
            stmt.setString(12, description);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Error saving Item: {}", objectId, e);
        }
    }

    @Override
    public boolean increaseSales(int object, int current) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_SALES_QUERY)) {
            
            stmt.setInt(1, current);
            stmt.setInt(2, object);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("Error increaseSales Item: {}", object, e);
            return false;
        }
    }
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}