package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.dao.InventoryDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.PlayerStorage;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.items.storage.StorageType;
import com.aionemu.gameserver.services.item.ItemService;
import javolution.util.FastList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author ATracer
 * Updated for MySQL 8 - Fixed connection leaks
 */
public class MySQL8InventoryDAO extends InventoryDAO {

    private static final Logger log = LoggerFactory.getLogger(MySQL8InventoryDAO.class);
    
    public static final String SELECT_QUERY = "SELECT `item_unique_id`, `item_id`, `item_count`, `item_color`, `color_expires`, " + "`item_creator`, `expire_time`, `activation_count`, `is_equiped`, `is_soul_bound`, " + "`slot`, `enchant`, `enchant_bonus`, `item_skin`, `fusioned_item`, `optional_socket`, " + "`optional_fusion_socket`, `charge`, `rnd_bonus`, `rnd_count`, `wrappable_count`, " + "`is_packed`, `tempering_level`, `is_topped`, `strengthen_skill`, `skin_skill`, " + "`luna_reskin`, `reduction_level`, `is_seal`, `isEnhance`, `enhanceSkillId`, " + "`enhanceSkillEnchant` FROM `inventory` WHERE `item_owner` = ? AND " + "`item_location` = ? AND `is_equiped` = ?";
    
    public static final String INSERT_QUERY = "INSERT INTO `inventory` (`item_unique_id`, `item_id`, `item_count`, `item_color`, " + "`color_expires`, `item_creator`, `expire_time`, `activation_count`, `item_owner`, " + "`is_equiped`, is_soul_bound, `slot`, `item_location`, `enchant`, `enchant_bonus`, " + "`item_skin`, `fusioned_item`, `optional_socket`, `optional_fusion_socket`, `charge`, " + "`rnd_bonus`, `rnd_count`, `wrappable_count`, `is_packed`, `tempering_level`, " + "`is_topped`, `strengthen_skill`, `skin_skill`, `luna_reskin`, `reduction_level`, " + "`is_seal`, `isEnhance`, `enhanceSkillId`, `enhanceSkillEnchant`) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
    public static final String UPDATE_QUERY = "UPDATE inventory SET item_count = ?, item_color = ?, color_expires = ?, " + "item_creator = ?, expire_time = ?, activation_count = ?, item_owner = ?, " + "is_equiped = ?, is_soul_bound = ?, slot = ?, item_location = ?, enchant = ?, " + "enchant_bonus = ?, item_skin = ?, fusioned_item = ?, optional_socket = ?, " + "optional_fusion_socket = ?, charge = ?, rnd_bonus = ?, rnd_count = ?, " + "wrappable_count = ?, is_packed = ?, tempering_level = ?, is_topped = ?, " + "strengthen_skill = ?, skin_skill = ?, luna_reskin = ?, reduction_level = ?, " + "is_seal = ?, isEnhance = ?, enhanceSkillId = ?, enhanceSkillEnchant = ? " + "WHERE item_unique_id = ?";
    
    public static final String DELETE_QUERY = "DELETE FROM inventory WHERE item_unique_id = ?";
    
    public static final String DELETE_CLEAN_QUERY = "DELETE FROM inventory WHERE item_owner = ? AND item_location != 2";
    
    public static final String SELECT_ACCOUNT_QUERY = "SELECT `account_id` FROM `players` WHERE `id` = ?";
    
    public static final String SELECT_LEGION_QUERY = "SELECT `legion_id` FROM `legion_members` WHERE `player_id` = ?";
    
    public static final String DELETE_ACCOUNT_WH = "DELETE FROM inventory WHERE item_owner = ? AND item_location = 2";
    
    public static final String SELECT_QUERY2 = "SELECT * FROM `inventory` WHERE `item_owner` = ? AND `item_location` = ?";
    
    public static final String SELECT_USED_IDS_QUERY = "SELECT item_unique_id FROM inventory";

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
                        log.error("Player {} loaded error item, itemUniqueId is: {}", 
                                playerId, item.getObjectId());
                    } else {
                        item.setPersistentState(PersistentState.UPDATED);
                        inventory.onLoadHandler(item);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not restore storage data for player: {}", playerId, e);
        }
        
        return inventory;
    }
    
    @Override
    public List<Item> loadStorageDirect(int playerId, StorageType storageType) {
        List<Item> list = FastList.newInstance();
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
            log.error("Could not restore loadStorageDirect data for player: {}", playerId, e);
        }
        
        return list;
    }

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
            log.error("Could not restore Equipment data for player: {}", playerId, e);
        }
        
        return equipment;
    }

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
            log.error("Could not restore Equipment data for player: {}", playerId, e);
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
            log.error("Could not restore accountId data for player: {}", playerId, e);
            return 0;
        }
    }

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
            log.error("Failed to load legion id for player id: {}", playerId, e);
            return 0;
        }
    }

    @Override
    public boolean store(Player player) {
        int playerId = player.getObjectId();
        Integer accountId = player.getPlayerAccount() != null ? player.getPlayerAccount().getId() : null;
        Integer legionId = player.getLegion() != null ? player.getLegion().getLegionId() : null;
        List<Item> allPlayerItems = player.getDirtyItemsToUpdate();
        return store(allPlayerItems, playerId, accountId, legionId);
    }

    @Override
    public boolean store(Item item, Player player) {
        int playerId = player.getObjectId();
        int accountId = player.getPlayerAccount().getId();
        Integer legionId = player.getLegion() != null ? player.getLegion().getLegionId() : null;
        return store(item, playerId, accountId, legionId);
    }

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

    @Override
    public boolean store(List<Item> items, Integer playerId, Integer accountId, Integer legionId) {
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

        boolean deleteResult = false;
        boolean insertResult = false;
        boolean updateResult = false;

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);
            
            deleteResult = deleteItems(con, itemsToDelete);
            insertResult = insertItems(con, itemsToInsert, playerId, accountId, legionId);
            updateResult = updateItems(con, itemsToUpdate, playerId, accountId, legionId);
            
            con.commit();
        } catch (SQLException e) {
            log.error("Can't open connection to save player inventory: {}", playerId, e);
            return false;
        }

        for (Item item : items) {
            if (item != null) {
                item.setPersistentState(PersistentState.UPDATED);
            }
        }

        if (!itemsToDelete.isEmpty() && deleteResult) {
            for (Item item : itemsToDelete) {
                if (item != null) {
                    ItemService.releaseItemId(item);
                }
            }
        }
        
        return deleteResult && insertResult && updateResult;
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

            stmt.executeBatch();
            return true;
        } catch (SQLException e) {
            log.error("Failed to execute insert batch", e);
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

            stmt.executeBatch();
            return true;
        } catch (SQLException e) {
            log.error("Failed to execute update batch", e);
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
            log.error("Failed to execute delete batch", e);
            throw e;
        }
    }

    @Override
    public boolean deletePlayerItems(final int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_CLEAN_QUERY)) {
            
            stmt.setInt(1, playerId);
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            log.error("Error deleting all player items. PlayerObjId: {}", playerId, e);
            return false;
        }
    }

    @Override
    public void deleteAccountWH(final int accountId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_ACCOUNT_WH)) {
            
            stmt.setInt(1, accountId);
            stmt.executeUpdate();
        } catch (Exception e) {
            log.error("Error deleting all items from account WH. AccountId: {}", accountId, e);
        }
    }

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
            log.error("Can't get list of id's from inventory table", e);
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