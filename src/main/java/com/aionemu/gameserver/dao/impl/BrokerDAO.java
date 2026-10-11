package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.model.broker.BrokerRace;
import com.aionemu.gameserver.model.gameobjects.BrokerItem;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;

import lombok.extern.slf4j.Slf4j;

/**
 * 交易所（Broker）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of BrokerDAO using JdbcClient. Fixed connection leaks.
 */
@Slf4j
@Repository
public class BrokerDAO extends com.aionemu.gameserver.dao.BrokerDAO {

    /** 加载全部交易所条目 / Select all broker entries */
    private static final String SELECT_BROKER_QUERY = "SELECT * FROM broker";
    /** 查询托管行背包物品地点126 / Select broker inventory items (location=126) */
    private static final String SELECT_INVENTORY_QUERY = "SELECT * FROM inventory WHERE `item_location` = 126";
    /** 插入交易所条目 / Insert a broker entry */
    private static final String INSERT_BROKER_QUERY = "INSERT INTO `broker` " + "(`item_pointer`, `item_id`, `item_count`, `item_creator`, " + "`seller`, `price`, `broker_race`, `expire_time`, `settle_time`, " + "`seller_id`, `is_sold`, `is_settled`, `is_splitsell`) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    /** 删除交易所条目 / Delete a broker entry */
    private static final String DELETE_BROKER_QUERY = "DELETE FROM `broker` " + "WHERE `item_pointer` = ? AND `seller_id` = ? AND `expire_time` = ?";
    /** 更新出售/结算状态 / Update sold/settled state */
    private static final String UPDATE_BROKER_QUERY = "UPDATE broker SET " + "`is_sold` = ?, `is_settled` = 1, `settle_time` = ? " + "WHERE `item_pointer` = ? AND `expire_time` = ? AND `seller_id` = ? AND `is_settled` = 0";
    /** 更新交易所物品数量与价格等 / Update broker item count, price, etc. */
    private static final String UPDATE_ITEM_QUERY = "UPDATE broker SET " + "`item_count` = ?, `price` = ?, `is_sold` = ?, " + "`is_settled` = ?, `settle_time` = ?, `is_splitsell` = ? " + "WHERE `item_pointer` = ? AND `expire_time` = ? AND `seller_id` = ? AND `is_settled` = 0";
    /** 购买前检查物品是否仍可售 / Pre-buy availability check */
    private static final String PREBUY_CHECK_QUERY = "SELECT 1 FROM broker WHERE `item_pointer` = ? AND `is_sold` = 0 LIMIT 1";

    private final JdbcClient jdbcClient;
    private final ItemStoneListDAO itemStoneListDAO;

    public BrokerDAO() {
        this(DatabaseFactory.getJdbcClient(), safeItemStoneListDAO());
    }

    @Autowired

    public BrokerDAO(JdbcClient jdbcClient, ItemStoneListDAO itemStoneListDAO) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
        this.itemStoneListDAO = Objects.requireNonNull(itemStoneListDAO, "itemStoneListDAO must not be null");
    }

    /**
     * 依赖 DAO 解析兜底：容器未就绪（离线/单测）时退回实现类无参构造。
     * Dependency-DAO resolution fallback: falls back to the no-arg impl constructor when the container is not ready (offline/unit tests).
     */
    private static ItemStoneListDAO safeItemStoneListDAO() {
        if (DAOManager.isInitialized()) {
            try {
                return DAOManager.getDAO(ItemStoneListDAO.class);
            } catch (Exception e) {
                // 离线兜底路径，退回实现类无参构造。 / offline fallback, use the no-arg impl constructor
            }
        }
        return new com.aionemu.gameserver.dao.impl.ItemStoneListDAO();
    }

    /**
     * 加载全部交易所物品（含未售出物品详情与镶嵌石）。
     * Loads all broker items (including unsold item details and stones).
     * @return 交易所物品列表 / list of broker items
     */
    @Override
    public List<BrokerItem> loadBroker() {
        final List<BrokerItem> brokerItems = new ArrayList<>();
        final List<Item> items = getBrokerItems();

        if (!items.isEmpty()) {
            itemStoneListDAO.load(items);
        }

        try {
            jdbcClient.sql(SELECT_BROKER_QUERY)
                .query((rset, _) -> {
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

                    return new BrokerItem(item, itemId, itemPointer, itemCount, itemCreator, price, seller, sellerId, itemBrokerRace, isSold, isSettled, expireTime, settleTime, isSplitSell);
                })
                .list()
                .forEach(brokerItems::add);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.39996ee23f7f"), e);
        }

        return brokerItems;
    }

    /**
     * 从库存表加载交易所位置（location=126）的物品。
     * Loads items from inventory at broker location (location=126).
     * @return 物品列表 / list of items
     */
    private List<Item> getBrokerItems() {
        final List<Item> brokerItems = new ArrayList<>();

        try {
            jdbcClient.sql(SELECT_INVENTORY_QUERY)
                .query((rset, _) -> {
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

                    return new Item(itemUniqueId, itemId, itemCount, itemColor, colorExpireTime, itemCreator, expireTime, activationCount, false, false, slot, location, enchant, enchantBonus, itemSkin, fusionedItem, optionalSocket, optionalFusionSocket, charge, randomNumber, rndCount, wrappingCount, false, temperingLevel, false, 0, 0, false, reductionLevel, unSeal, isEnhance, enhanceSkillId, enhanceSkillEnchant);
                })
                .list()
                .forEach(brokerItems::add);
        } catch (DataAccessException e) {
            log.error(I18n.get("log.21c8f55ddfc2"), e);
        }

        return brokerItems;
    }

    /**
     * 按持久化状态存储交易所物品。
     * Stores a broker item according to its persistent state.
     * @param item 交易所物品 / broker item
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean store(BrokerItem item) {
        if (item == null) {
            log.warn(I18n.get("log.89da0d5f3c1f"));
            return false;
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            storeInTransaction(con, item);
            item.setPersistentState(PersistentState.UPDATED);
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.96bb344ecaba", item.getItemUniqueId()), e);
            return false;
        }
    }

    @Override
    public void storeInTransaction(Connection con, BrokerItem item) throws SQLException {
        boolean result = switch (item.getPersistentState()) {
            case NEW -> insertBrokerItem(con, item);
            case DELETED -> deleteBrokerItem(con, item);
            case UPDATE_ITEM_BROKER -> updateItem(con, item);
            case UPDATE_REQUIRED -> updateBrokerItem(con, item);
            default -> true;
        };
        if (!result) {
            throw new SQLException("No broker row changed for item " + item.getItemUniqueId());
        }
    }

    /**
     * 插入交易所条目。
     * Inserts a broker entry.
     * @param item 交易所物品 / broker item
     * @return 是否成功 / whether successful
     */
    private boolean insertBrokerItem(Connection con, final BrokerItem item) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(INSERT_BROKER_QUERY)) {
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
        }
    }

    /**
     * 删除交易所条目。
     * Deletes a broker entry.
     * @param item 交易所物品 / broker item
     * @return 是否成功 / whether successful
     */
    private boolean deleteBrokerItem(Connection con, final BrokerItem item) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(DELETE_BROKER_QUERY)) {
            stmt.setInt(1, item.getItemUniqueId());
            stmt.setInt(2, item.getSellerId());
            stmt.setTimestamp(3, item.getExpireTime());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * 购买前检查物品是否仍可售。
     * Checks whether the item is still available for purchase.
     * @param itemForCheck 物品指针 ID / item pointer id
     * @return 是否可用 / whether available
     */
    @Override
    public boolean preBuyCheck(int itemForCheck) {
        try {
            Integer found = jdbcClient.sql(PREBUY_CHECK_QUERY)
                .param(itemForCheck)
                .query(Integer.class)
                .optional()
                .orElse(null);
            return found != null;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.565c72023756", itemForCheck), e);
            return false;
        }
    }

    /**
     * 更新交易所条目的出售/结算状态。
     * Updates the sold/settled state of a broker entry.
     * @param item 交易所物品 / broker item
     * @return 是否成功 / whether successful
     */
    private boolean updateBrokerItem(Connection con, final BrokerItem item) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(UPDATE_BROKER_QUERY)) {
            stmt.setBoolean(1, item.isSold());
            stmt.setTimestamp(2, item.getSettleTime());
            stmt.setInt(3, item.getItemUniqueId());
            stmt.setTimestamp(4, item.getExpireTime());
            stmt.setInt(5, item.getSellerId());
            return stmt.executeUpdate() > 0;
        }
    }

    /**
     * 更新交易所物品数量、价格与拆分出售等字段。
     * Updates broker item count, price, split-sell and related fields.
     * @param item 交易所物品 / broker item
     * @return 是否成功 / whether successful
     */
    private boolean updateItem(Connection con, final BrokerItem item) throws SQLException {
        try (PreparedStatement stmt = con.prepareStatement(UPDATE_ITEM_QUERY)) {
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
        }
    }

    /**
     * 获取已使用的玩家 ID 列表（用于 ID 分配）。
     * Returns used player ids (for id allocation).
     * @return 已用 ID 数组 / array of used ids
     */
    @Override
    public int[] getUsedIDs() {
        String query = "SELECT id FROM players";
        try {
            List<Integer> ids = jdbcClient.sql(query)
                .query((rs, _) -> rs.getInt("id"))
                .list();

            int[] result = new int[ids.size()];
            for (int i = 0; i < ids.size(); i++) {
                result[i] = ids.get(i);
            }
            return result;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.ce9d363cd21c"), e);
            return new int[0];
        }
    }

    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
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
