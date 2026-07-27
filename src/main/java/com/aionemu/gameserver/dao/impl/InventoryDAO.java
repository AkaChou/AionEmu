package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.services.item.ItemService;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 物品栏/仓库数据访问对象的 MySQL 8 实现，已修复连接泄漏。
 * MySQL 8 implementation of InventoryDAO with connection leak fixes.
 *
 * @author ATracer
 */
@Slf4j
public class InventoryDAO extends com.aionemu.gameserver.dao.InventoryDAO {

    /** 查询指定位置未装备物品 / Select unequipped items for owner and location */
    public static final String SELECT_QUERY = "SELECT `item_unique_id`, `item_id`, `item_count`, `item_color`, `color_expires`, " + "`item_creator`, `expire_time`, `activation_count`, `is_equiped`, `is_soul_bound`, " + "`slot`, `enchant`, `enchant_bonus`, `item_skin`, `fusioned_item`, `optional_socket`, " + "`optional_fusion_socket`, `charge`, `rnd_bonus`, `rnd_count`, `wrappable_count`, " + "`is_packed`, `tempering_level`, `is_topped`, `strengthen_skill`, `skin_skill`, " + "`luna_reskin`, `reduction_level`, `is_seal`, `isEnhance`, `enhanceSkillId`, " + "`enhanceSkillEnchant` FROM `inventory` WHERE `item_owner` = ? AND " + "`item_location` = ? AND `is_equiped` = ?";

    /** 插入物品记录 / Insert inventory item row */
    public static final String INSERT_QUERY = "INSERT INTO `inventory` (`item_unique_id`, `item_id`, `item_count`, `item_color`, " + "`color_expires`, `item_creator`, `expire_time`, `activation_count`, `item_owner`, " + "`is_equiped`, is_soul_bound, `slot`, `item_location`, `enchant`, `enchant_bonus`, " + "`item_skin`, `fusioned_item`, `optional_socket`, `optional_fusion_socket`, `charge`, " + "`rnd_bonus`, `rnd_count`, `wrappable_count`, `is_packed`, `tempering_level`, " + "`is_topped`, `strengthen_skill`, `skin_skill`, `luna_reskin`, `reduction_level`, " + "`is_seal`, `isEnhance`, `enhanceSkillId`, `enhanceSkillEnchant`) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /** 更新物品记录 / Update inventory item row */
    public static final String UPDATE_QUERY = "UPDATE inventory SET item_count = ?, item_color = ?, color_expires = ?, " + "item_creator = ?, expire_time = ?, activation_count = ?, item_owner = ?, " + "is_equiped = ?, is_soul_bound = ?, slot = ?, item_location = ?, enchant = ?, " + "enchant_bonus = ?, item_skin = ?, fusioned_item = ?, optional_socket = ?, " + "optional_fusion_socket = ?, charge = ?, rnd_bonus = ?, rnd_count = ?, " + "wrappable_count = ?, is_packed = ?, tempering_level = ?, is_topped = ?, " + "strengthen_skill = ?, skin_skill = ?, luna_reskin = ?, reduction_level = ?, " + "is_seal = ?, isEnhance = ?, enhanceSkillId = ?, enhanceSkillEnchant = ? " + "WHERE item_unique_id = ?";

    /** 按唯一 ID 删除物品 / Delete inventory item by unique id */
    public static final String DELETE_QUERY = "DELETE FROM inventory WHERE item_unique_id = ?";

    /** 删除玩家非账号仓库物品 / Delete player items except account warehouse */
    public static final String DELETE_CLEAN_QUERY = "DELETE FROM inventory WHERE item_owner = ? AND item_location != 2";

    /** 按玩家 ID 查询账号 ID / Select account id by player id */
    public static final String SELECT_ACCOUNT_QUERY = "SELECT `account_id` FROM `players` WHERE `id` = ?";

    /** 按玩家 ID 查询军团 ID / Select legion id by player id */
    public static final String SELECT_LEGION_QUERY = "SELECT `legion_id` FROM `legion_members` WHERE `player_id` = ?";

    /** 删除账号仓库物品 / Delete account warehouse items */
    public static final String DELETE_ACCOUNT_WH = "DELETE FROM inventory WHERE item_owner = ? AND item_location = 2";

    /** 按拥有者与位置查询物品 / Select items by owner and location */
    public static final String SELECT_QUERY2 = "SELECT * FROM `inventory` WHERE `item_owner` = ? AND `item_location` = ?";

    /** 查询已占用的物品唯一 ID / Select used item unique ids */
    public static final String SELECT_USED_IDS_QUERY = "SELECT item_unique_id FROM inventory";

    /**
     * 加载指定存储类型的物品栏；账号仓库会先解析为账号 ID。
     * Loads a storage of the given type for a player; account warehouse resolves to account id first.
     *
     * player id
     * storage type
     * storage instance
     */
    @Override
    public Storage loadStorage(int playerId, StorageType storageType) {
        final Storage inventory = new PlayerStorage(storageType);
        final int storage = storageType.getId();
        final int equipped = 0;

        if (storageType == StorageType.ACCOUNT_WAREHOUSE) {
            playerId = loadPlayerAccountId(playerId);
        }

        final int owner = playerId;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, owner);
            stmt.setInt(2, storage);
            stmt.setInt(3, equipped);

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    Item item = constructItem(storage, rset);
                    if (item.getItemTemplate() == null) {
                        log.error(I18n.get("log.9a2f5be30d7d", playerId, item.getObjectId()));
                    } else {
                        item.setPersistentState(PersistentState.UPDATED);
                        inventory.onLoadHandler(item);
                    }
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.52f3a07ead8f", playerId, e));
        }

        return inventory;
    }

    /**
     * 直接加载指定存储位置的物品列表（不封装为 Storage）。
     * Loads items of a storage type as a plain list (no Storage wrapper).
     *
     * player id
     * storage type
     * item list
     */
    @Override
    public List<Item> loadStorageDirect(int playerId, StorageType storageType) {
        List<Item> list = new ArrayList<>();
        final int storage = storageType.getId();

        if (storageType == StorageType.ACCOUNT_WAREHOUSE) {
            playerId = loadPlayerAccountId(playerId);
        }

        final int owner = playerId;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY2)) {

            stmt.setInt(1, owner);
            stmt.setInt(2, storageType.getId());

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    list.add(constructItem(storage, rset));
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.96d353298d09", playerId, e));
        }

        return list;
    }

    /**
     * 加载玩家已装备物品到装备栏对象。
     * Loads equipped items into an Equipment instance for the player.
     *
     * 玩家 / player
     * equipment
     */
    @Override
    public Equipment loadEquipment(Player player) {
        final Equipment equipment = new Equipment(player);
        int playerId = player.getObjectId();
        final int storage = 0;
        final int equipped = 1;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, storage);
            stmt.setInt(3, equipped);

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    Item item = constructItem(storage, rset);
                    item.setPersistentState(PersistentState.UPDATED);
                    equipment.onLoadHandler(item);
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.ce1e87b43e9b", playerId, e));
        }

        return equipment;
    }

    /**
     * 按玩家 ID 加载已装备物品列表。
     * Loads equipped items as a list by player id.
     *
     * player id
     *
     * @param playerId
     * @return 已装备物品列表 / equipped item list
     */
    @Override
    public List<Item> loadEquipment(int playerId) {
        final List<Item> items = new ArrayList<>();
        final int storage = 0;
        final int equipped = 1;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, storage);
            stmt.setInt(3, equipped);

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    Item item = constructItem(storage, rset);
                    items.add(item);
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.ce1e87b43e9b", playerId, e));
        }

        return items;
    }

    private Item constructItem(final int storage, ResultSet rset) throws SQLException {
        int itemUniqueId = rset.getInt("item_unique_id");
        int itemId = rset.getInt("item_id");
        long itemCount = rset.getLong("item_count");
        int itemColor = rset.getInt("item_color");
        int colorExpireTime = rset.getInt("color_expires");
        String itemCreator = rset.getString("item_creator");
        int expireTime = rset.getInt("expire_time");
        int activationCount = rset.getInt("activation_count");
        int isEquiped = rset.getInt("is_equiped");
        int isSoulBound = rset.getInt("is_soul_bound");
        long slot = rset.getLong("slot");
        int enchant = rset.getInt("enchant");
        int enchantBonus = rset.getInt("enchant_bonus");
        int itemSkin = rset.getInt("item_skin");
        int fusionedItem = rset.getInt("fusioned_item");
        int optionalSocket = rset.getInt("optional_socket");
        int optionalFusionSocket = rset.getInt("optional_fusion_socket");
        int charge = rset.getInt("charge");
        int randomBonus = rset.getInt("rnd_bonus");
        int rndCount = rset.getInt("rnd_count");
        int wrappingCount = rset.getInt("wrappable_count");
        int isPacked = rset.getInt("is_packed");
        int temperingLevel = rset.getInt("tempering_level");
        int isTopped = rset.getInt("is_topped");
        int strengthenSkill = rset.getInt("strengthen_skill");
        int skinSkill = rset.getInt("skin_skill");
        int isLunaReskin = rset.getInt("luna_reskin");
        int reductionLevel = rset.getInt("reduction_level");
        int unSeal = rset.getInt("is_seal");
        boolean isEnhance = rset.getBoolean("isEnhance");
        int enhanceSkillId = rset.getInt("enhanceSkillId");
        int enhanceSkillEnchant = rset.getInt("enhanceSkillEnchant");

        return new Item(itemUniqueId, itemId, itemCount, itemColor, colorExpireTime, itemCreator, expireTime, activationCount, isEquiped == 1, isSoulBound == 1, slot, storage, enchant, enchantBonus, itemSkin, fusionedItem, optionalSocket, optionalFusionSocket, charge, randomBonus, rndCount, wrappingCount, isPacked == 1, temperingLevel, isTopped == 1, strengthenSkill, skinSkill, isLunaReskin == 1, reductionLevel, unSeal, isEnhance, enhanceSkillId, enhanceSkillEnchant);
    }

    private int loadPlayerAccountId(final int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_ACCOUNT_QUERY)) {

            stmt.setInt(1, playerId);
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt("account_id");
                }
            }
            return 0;
        } catch (Exception e) {
            log.error(I18n.get("log.8cf69332a5ea", playerId, e));
            return 0;
        }
    }

    /**
     * 按玩家 ID 加载所属军团 ID。
     * Loads the legion id for the given player.
     *
     * player id
     *
     * @param playerId
     * @return 军团 ID；未加入或出错时返回 0 / legion id, or 0 if none/error
     */
    public int loadLegionId(final int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_LEGION_QUERY)) {

            stmt.setInt(1, playerId);
            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt("legion_id");
                }
            }
            return 0;
        } catch (Exception e) {
            log.error(I18n.get("log.13d51b51a32f", playerId, e));
            return 0;
        }
    }

    /**
     * 持久化玩家全部脏物品（含账号仓/军团仓归属）。
     * Persists all dirty items of the player (resolves account/legion ownership).
     *
     * 玩家 / player
     * @return 是否全部成功 / whether all operations succeeded
     */
    @Override
    public boolean store(Player player) {
        int playerId = player.getObjectId();
        Integer accountId = player.getPlayerAccount() != null ? player.getPlayerAccount().getId() : null;
        Integer legionId = player.getLegion() != null ? player.getLegion().getLegionId() : null;
        List<Item> allPlayerItems = player.getDirtyItemsToUpdate();
        if (!store(allPlayerItems, playerId, accountId, legionId)) {
            return false;
        }
        player.markDirtyItemContainersStored();
        return true;
    }

    /**
     * 持久化单个物品（绑定玩家账号与军团）。
     * Persists a single item bound to the player's account and legion.
     *
     * item
     * 玩家 / player
     * whether succeeded
     */
    @Override
    public boolean store(Item item, Player player) {
        int playerId = player.getObjectId();
        int accountId = player.getPlayerAccount().getId();
        Integer legionId = player.getLegion() != null ? player.getLegion().getLegionId() : null;
        return store(item, playerId, accountId, legionId);
    }

    /**
     * 按玩家 ID 持久化物品列表；按需解析账号仓/军团仓归属。
     * Persists an item list by player id; resolves account/legion ownership when needed.
     *
     * @param items 物品列表 / item list
     * player id
     * whether succeeded
     */
    @Override
    public boolean store(List<Item> items, int playerId) {
        Integer accountId = null;
        Integer legionId = null;

        for (Item item : items) {
            if (accountId == null && item.getItemLocation() == StorageType.ACCOUNT_WAREHOUSE.getId()) {
                accountId = loadPlayerAccountId(playerId);
            }

            if (legionId == null && item.getItemLocation() == StorageType.LEGION_WAREHOUSE.getId()) {
                int localLegionId = loadLegionId(playerId);
                if (localLegionId > 0) {
                    legionId = localLegionId;
                }
            }
        }
        return store(items, playerId, accountId, legionId);
    }

    /**
     * 按持久化状态批量插入/更新/删除物品（事务提交）。
     * Batch inserts/updates/deletes items by persistent state within a transaction.
     *
     * @param items 物品列表 / item list
     * player id
     * @param accountId 账号 ID（账号仓用） / account id (for account warehouse)
     * @param legionId 军团 ID（军团仓用） / legion id (for legion warehouse)
     * @return 是否全部成功 / whether all operations succeeded
     */
    @Override
    public boolean store(List<Item> items, Integer playerId, Integer accountId, Integer legionId) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            try {
                storeInTransaction(con, items, playerId, accountId, legionId);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.c240e77dc213", playerId), e);
            return false;
        }

        markStored(items);
        return true;
    }

    @Override
    public void storeInTransaction(Connection con, List<Item> items, Integer playerId, Integer accountId,
                                   Integer legionId) throws SQLException {
        Collection<Item> itemsToUpdate = new ArrayList<>();
        Collection<Item> itemsToInsert = new ArrayList<>();
        Collection<Item> itemsToDelete = new ArrayList<>();

        for (Item item : items) {
            if (item != null) {
                PersistentState state = item.getPersistentState();
                if (state == PersistentState.NEW) {
                    itemsToInsert.add(item);
                } else if (state == PersistentState.UPDATE_REQUIRED) {
                    itemsToUpdate.add(item);
                } else if (state == PersistentState.DELETED) {
                    itemsToDelete.add(item);
                }
            }
        }

        deleteItems(con, itemsToDelete);
        insertItems(con, itemsToInsert, playerId, accountId, legionId);
        updateItems(con, itemsToUpdate, playerId, accountId, legionId);
    }

    @Override
    public void markStored(Collection<Item> items) {
        for (Item item : items) {
            if (item != null) {
                boolean deleted = item.getPersistentState() == PersistentState.DELETED;
                item.setPersistentState(PersistentState.UPDATED);
                if (deleted) {
                    ItemService.releaseItemId(item);
                }
            }
        }
    }

    private boolean store(Item item, int playerId, int accountId, Integer legionId) {
        List<Item> items = new ArrayList<>();
        items.add(item);
        return store(items, playerId, accountId, legionId);
    }

    private int getItemOwnerId(Item item, Integer playerId, Integer accountId, Integer legionId) {
        if (item.getItemLocation() == StorageType.ACCOUNT_WAREHOUSE.getId()) {
            return accountId != null ? accountId : 0;
        }

        if (item.getItemLocation() == StorageType.LEGION_WAREHOUSE.getId()) {
            return legionId != null ? legionId : playerId;
        }

        return playerId != null ? playerId : 0;
    }

    private boolean insertItems(Connection con, Collection<Item> items, Integer playerId, Integer accountId, Integer legionId)
            throws SQLException {

        if (GenericValidator.isBlankOrNull(items)) {
            return true;
        }

        try (PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {
            for (Item item : items) {
                stmt.setInt(1, item.getObjectId());
                stmt.setInt(2, item.getItemTemplate().getTemplateId());
                stmt.setLong(3, item.getItemCount());
                stmt.setInt(4, item.getItemColor());
                stmt.setInt(5, item.getColorExpireTime());
                stmt.setString(6, item.getItemCreator());
                stmt.setInt(7, item.getExpireTime());
                stmt.setInt(8, item.getActivationCount());
                stmt.setInt(9, getItemOwnerId(item, playerId, accountId, legionId));
                stmt.setBoolean(10, item.isEquipped());
                stmt.setInt(11, item.isSoulBound() ? 1 : 0);
                stmt.setLong(12, item.getEquipmentSlot());
                stmt.setInt(13, item.getItemLocation());
                stmt.setInt(14, item.getEnchantLevel());
                stmt.setInt(15, item.getEnchantBonus());
                stmt.setInt(16, item.getItemSkinTemplate().getTemplateId());
                stmt.setInt(17, item.getFusionedItemId());
                stmt.setInt(18, item.getOptionalSocket());
                stmt.setInt(19, item.getOptionalFusionSocket());
                stmt.setInt(20, item.getChargePoints());
                stmt.setInt(21, item.getBonusNumber());
                stmt.setInt(22, item.getRandomCount());
                stmt.setInt(23, item.getWrappableCount());
                stmt.setBoolean(24, item.isPacked());
                stmt.setInt(25, item.getAuthorize());
                stmt.setBoolean(26, item.isAmplified());
                stmt.setInt(27, item.getAmplificationSkill());
                stmt.setInt(28, item.getItemSkinSkill());
                stmt.setBoolean(29, item.isLunaReskin());
                stmt.setInt(30, item.getReductionLevel());
                stmt.setInt(31, item.getUnSeal());
                stmt.setBoolean(32, item.isEnhance());
                stmt.setInt(33, item.getEnhanceSkillId());
                stmt.setInt(34, item.getEnhanceEnchantLevel());
                stmt.addBatch();
            }

            ensureBatchChanged(stmt.executeBatch(), items.size(), "insert");
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.f078e8399e22"), e);
            throw e;
        }
    }

    private boolean updateItems(Connection con, Collection<Item> items, Integer playerId, Integer accountId, Integer legionId) throws SQLException {

        if (GenericValidator.isBlankOrNull(items)) {
            return true;
        }

        try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
            for (Item item : items) {
                stmt.setLong(1, item.getItemCount());
                stmt.setInt(2, item.getItemColor());
                stmt.setInt(3, item.getColorExpireTime());
                stmt.setString(4, item.getItemCreator());
                stmt.setInt(5, item.getExpireTime());
                stmt.setInt(6, item.getActivationCount());
                stmt.setInt(7, getItemOwnerId(item, playerId, accountId, legionId));
                stmt.setBoolean(8, item.isEquipped());
                stmt.setInt(9, item.isSoulBound() ? 1 : 0);
                stmt.setLong(10, item.getEquipmentSlot());
                stmt.setInt(11, item.getItemLocation());
                stmt.setInt(12, item.getEnchantLevel());
                stmt.setInt(13, item.getEnchantBonus());
                stmt.setInt(14, item.getItemSkinTemplate().getTemplateId());
                stmt.setInt(15, item.getFusionedItemId());
                stmt.setInt(16, item.getOptionalSocket());
                stmt.setInt(17, item.getOptionalFusionSocket());
                stmt.setInt(18, item.getChargePoints());
                stmt.setInt(19, item.getBonusNumber());
                stmt.setInt(20, item.getRandomCount());
                stmt.setInt(21, item.getWrappableCount());
                stmt.setBoolean(22, item.isPacked());
                stmt.setInt(23, item.getAuthorize());
                stmt.setBoolean(24, item.isAmplified());
                stmt.setInt(25, item.getAmplificationSkill());
                stmt.setInt(26, item.getItemSkinSkill());
                stmt.setBoolean(27, item.isLunaReskin());
                stmt.setInt(28, item.getReductionLevel());
                stmt.setInt(29, item.getUnSeal());
                stmt.setBoolean(30, item.isEnhance());
                stmt.setInt(31, item.getEnhanceSkillId());
                stmt.setInt(32, item.getEnhanceEnchantLevel());
                stmt.setInt(33, item.getObjectId());
                stmt.addBatch();
            }

            ensureBatchChanged(stmt.executeBatch(), items.size(), "update");
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.ac4bea7b080f"), e);
            throw e;
        }
    }

    private boolean deleteItems(Connection con, Collection<Item> items) throws SQLException {
        if (GenericValidator.isBlankOrNull(items)) {
            return true;
        }

        try (PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {
            for (Item item : items) {
                stmt.setInt(1, item.getObjectId());
                stmt.addBatch();
            }

            stmt.executeBatch();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.8c6bf238353d"), e);
            throw e;
        }
    }

    private void ensureBatchChanged(int[] results, int expected, String operation) throws SQLException {
        if (results.length != expected) {
            throw new SQLException("Inventory " + operation + " batch returned " + results.length
                    + " results for " + expected + " items");
        }
        for (int result : results) {
            if (result == 0 || result == Statement.EXECUTE_FAILED) {
                throw new SQLException("Inventory " + operation + " batch did not change every row");
            }
        }
    }

    /**
     * 删除玩家非账号仓库的全部物品。
     * Deletes all non-account-warehouse items owned by the player.
     *
     * player id
     * whether succeeded
     */
    @Override
    public boolean deletePlayerItems(final int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_CLEAN_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            log.error(I18n.get("log.6a58c54be58e", playerId, e));
            return false;
        }
    }

    /**
     * 删除指定账号的账号仓库物品。
     * Deletes account warehouse items for the given account id.
     *
     * @param accountId 账号 ID / account id
     */
    @Override
    public void deleteAccountWH(final int accountId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_ACCOUNT_WH)) {

            stmt.setInt(1, accountId);
            stmt.executeUpdate();
        } catch (Exception e) {
            log.error(I18n.get("log.aa5fb04d48d6", accountId, e));
        }
    }

    /**
     * 获取已占用的物品唯一 ID 列表，供 ID 工厂使用。
     * Returns used item unique ids for ID factory allocation.
     *
     * 已占用 ID 数组；出错时返回空数组。
     * used id array, or empty on error.
     */
    @Override
    public int[] getUsedIDs() {
        String query = "SELECT item_unique_id FROM inventory";
        List<Integer> ids = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ids.add(rs.getInt("item_unique_id"));
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.fc3775ae3813", e));
            return new int[0];
        }

        int[] result = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            result[i] = ids.get(i);
        }
        return result;
    }

    /**
     * 判断当前数据库是否受 MySQL 8 DAO 支持。
     * Returns whether the given database is supported by MySQL 8 DAOs.
     *
     * database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
