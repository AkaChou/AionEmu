package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.ingameshop.IGItem;

import lombok.extern.slf4j.Slf4j;

/**
 * 游戏内商城 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of InGameShopDAO using JdbcClient.
 * @author xTz
 */
@Slf4j
@Repository
public class InGameShopDAO extends com.aionemu.gameserver.dao.InGameShopDAO {

    /** 查询商店物品 SQL / Select shop items SQL */
    private static final String SELECT_QUERY = "SELECT `object_id`, `item_id`, `item_count`, `item_price`, `category`, `sub_category`, `list`, `sales_ranking`, `item_type`, `gift`, `title_description`, `description` FROM `ingameshop`";
    /** 删除商店物品 SQL / Delete shop item SQL */
    private static final String DELETE_QUERY = "DELETE FROM `ingameshop` WHERE `item_id`=? AND `category`=? AND `sub_category`=? AND `list`=?";
    /** 更新销量排名 SQL / Update sales ranking SQL*/
    private static final String UPDATE_SALES_QUERY = "UPDATE `ingameshop` SET `sales_ranking`=? WHERE `object_id`=?";
    /** 插入商店物品 SQL / Insert shop item SQL */
    private static final String INSERT_QUERY = "INSERT INTO ingameshop(object_id, item_id, item_count, item_price, category, sub_category, list, sales_ranking, item_type, gift, title_description, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private final JdbcClient jdbcClient;

    public InGameShopDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public InGameShopDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 加载游戏内商城物品。
     * Loads in-game shop items.
     * @return 按分类分组的商城物品 / shop items grouped by category
     */
    @Override
    public Map<Byte, List<IGItem>> loadInGameShopItems() {
        Map<Byte, List<IGItem>> items = new LinkedHashMap<>();

        try {
            jdbcClient.sql(SELECT_QUERY)
                .query((rs, _) -> {
                    byte category = rs.getByte("category");
                    byte subCategory = rs.getByte("sub_category");

                    if (subCategory < 3) {
                        return null;
                    }

                    int objectId = rs.getInt("object_id");
                    int itemId = rs.getInt("item_id");
                    long itemCount = rs.getLong("item_count");
                    long itemPrice = rs.getLong("item_price");
                    int list = rs.getInt("list");
                    int salesRanking = rs.getInt("sales_ranking");
                    byte itemType = rs.getByte("item_type");
                    byte gift = rs.getByte("gift");
                    String titleDescription = rs.getString("title_description");
                    String description = rs.getString("description");

                    if (!items.containsKey(category)) {
                        items.put(category, new ArrayList<>());
                    }

                    items.get(category).add(new IGItem(objectId, itemId, itemCount, itemPrice, category, subCategory, list, salesRanking, itemType, gift, titleDescription, description));
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.e1ffb8726a87"), e);
        }
        return items;
    }

    /**
     * 删除游戏内商城物品。
     * Deletes an in-game shop item.
     * @param itemId 物品 ID / item id
     * @param category 类别 / category
     * @param subCategory 子类别 / sub category
     * @param list 列表编号 / list number
     * @return 是否删除成功 / whether deletion succeeded
     */
    @Override
    public void deleteIngameShopItem(int itemId, byte category, byte subCategory, int list) {
        try {
            jdbcClient.sql(DELETE_QUERY)
                .param(itemId)
                .param(category)
                .param(subCategory)
                .param(list)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.ab89a36682b8", itemId), e);
                        ;
        }
    }

    /**
     * 保存游戏内商城物品。
     * Saves an in-game shop item.
     */
    @Override
    public void saveIngameShopItem(int objectId, int itemId, long itemCount, long itemPrice, byte category, byte subCategory, int list, int salesRanking, byte itemType, byte gift, String titleDescription, String description) {
        try {
            jdbcClient.sql(INSERT_QUERY)
                .param(objectId)
                .param(itemId)
                .param(itemCount)
                .param(itemPrice)
                .param(category)
                .param(subCategory)
                .param(list)
                .param(salesRanking)
                .param(itemType)
                .param(gift)
                .param(titleDescription)
                .param(description)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.1bd07eff1e18", objectId), e);
        }
    }

    /**
     * 增加销量排名。
     * Increases sales ranking.
     * @param object 对象 ID / object id
     * @param current 当前销售额 / current sales
     * @return 是否成功 / whether succeeded
     */
    @Override
    public void increaseSales(int object, int current) {
        try {
            jdbcClient.sql(UPDATE_SALES_QUERY)
                .param(current)
                .param(object)
                .update();
                        ;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.2b5a5b8ef0fb", object), e);
                        ;
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
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
