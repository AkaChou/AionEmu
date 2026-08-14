package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.ingameshop.IGItem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 游戏内商城 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of InGameShopDAO.
 *
 * @author xTz
 */
@Slf4j
public class InGameShopDAO extends com.aionemu.gameserver.dao.InGameShopDAO {


 /** 查询商店物品 SQL / Select shop items SQL */
    private static final String SELECT_QUERY = "SELECT `object_id`, `item_id`, `item_count`, `item_price`, `category`, `sub_category`, `list`, `sales_ranking`, `item_type`, `gift`, `title_description`, `description` FROM `ingameshop`";

 /** 删除商店物品 SQL / Delete shop item SQL */
    private static final String DELETE_QUERY = "DELETE FROM `ingameshop` WHERE `item_id`=? AND `category`=? AND `sub_category`=? AND `list`=?";

    /** 更新销量排名 SQL / Update sales ranking SQL*/
    private static final String UPDATE_SALES_QUERY = "UPDATE `ingameshop` SET `sales_ranking`=? WHERE `object_id`=?";

    /**
     * 加载游戏内商城物品。
     * Loads in-game shop items.
     *
     * @return 按分类分组的商城物品 / shop items grouped by category
     */
    @Override
    public Map<Byte, List<IGItem>> loadInGameShopItems() {
        Map<Byte, List<IGItem>> items = new LinkedHashMap<>();

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
            log.error(I18n.get("log.e1ffb8726a87", e));
        }
        return items;
    }

    /**
     * 删除游戏内商城物品。
     * Deletes an in-game shop item.
     *
     * @param itemId 物品 ID / item id
     * @param category 类别 / category
     * @param subCategory 子类别 / sub category
     * @param list 列表编号 / list number
     * @return 是否删除成功 / whether deletion succeeded
     */
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
            log.error(I18n.get("log.ab89a36682b8", itemId, e));
            return false;
        }
    }

    /**
     * 保存游戏内商城物品。
     * Saves an in-game shop item.
     *
     * @param objectId 对象 ID / object id
     * @param itemId 物品 ID / item id
     * @param itemCount 物品数量 / item count
     * @param itemPrice 物品价格 / item price
     * @param category 类别 / category
     * @param subCategory 子类别 / sub category
     * @param list 列表编号 / list number
     * @param salesRanking 销售额排名 / sales ranking
     * @param itemType 物品类型 / item type
     * @param gift 是否礼品 / gift flag
     * @param titleDescription 称号描述 / title description
     * @param description 描述 / description
     */
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
            log.error(I18n.get("log.1bd07eff1e18", objectId, e));
        }
    }

    /**
     * 增加销量排名。
     * Increases sales ranking.
     *
     * @param object 对象 ID / object id
     * @param current 当前销售额 / current sales
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean increaseSales(int object, int current) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_SALES_QUERY)) {

            stmt.setInt(1, current);
            stmt.setInt(2, object);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.2b5a5b8ef0fb", object, e));
            return false;
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     *
     * @param databaseName 数据库名 / database name
     * @param majorVersion 主版本 / major version
     * @param minorVersion 次版本 / minor version
     * @return 是否支持 / whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
