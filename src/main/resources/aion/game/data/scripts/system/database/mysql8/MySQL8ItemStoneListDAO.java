package mysql8;

import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.configs.main.EnchantsConfig;
import com.aionemu.gameserver.dao.ItemStoneListDAO;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.items.GodStone;
import com.aionemu.gameserver.model.items.IdianStone;
import com.aionemu.gameserver.model.items.ItemStone;
import com.aionemu.gameserver.model.items.ItemStone.ItemStoneType;
import com.aionemu.gameserver.model.items.ManaStone;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.sql.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * MySQL 8 implementation of ItemStoneListDAO
 */
public class MySQL8ItemStoneListDAO extends ItemStoneListDAO {
    
    private static final Logger log = LoggerFactory.getLogger(MySQL8ItemStoneListDAO.class);
    
    public static final String INSERT_QUERY = "INSERT INTO `item_stones` " + "(`item_unique_id`, `item_id`, `slot`, `category`, `polishNumber`, `polishCharge`) " + "VALUES (?, ?, ?, ?, ?, ?)";
    
    public static final String UPDATE_QUERY = "UPDATE `item_stones` SET " + "`item_id` = ?, `slot` = ?, `polishNumber` = ?, `polishCharge` = ? " + "WHERE `item_unique_id` = ? AND `category` = ?";
    
    public static final String DELETE_QUERY = "DELETE FROM `item_stones` " + "WHERE `item_unique_id` = ? AND `slot` = ? AND `category` = ?";
    
    public static final String SELECT_QUERY = "SELECT `item_id`, `slot`, `category`, " + "`polishNumber`, `polishCharge` FROM `item_stones` WHERE `item_unique_id` = ?";
    
    private static final Predicate<ItemStone> itemStoneAddPredicate = 
        new Predicate<ItemStone>() {
            @Override
            public boolean apply(@Nullable ItemStone itemStone) {
                return itemStone != null && PersistentState.NEW == itemStone.getPersistentState();
            }
        };
    
    private static final Predicate<ItemStone> itemStoneDeletedPredicate = 
        new Predicate<ItemStone>() {
            @Override
            public boolean apply(@Nullable ItemStone itemStone) {
                return itemStone != null && PersistentState.DELETED == itemStone.getPersistentState();
            }
        };
    
    private static final Predicate<ItemStone> itemStoneUpdatePredicate = 
        new Predicate<ItemStone>() {
            @Override
            public boolean apply(@Nullable ItemStone itemStone) {
                return itemStone != null && PersistentState.UPDATE_REQUIRED == itemStone.getPersistentState();
            }
        };
    
    @Override
    public void load(final Collection<Item> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {
            
            for (Item item : items) {
                if (item.getItemTemplate() == null) {
                    continue;
                }
                
                if (item.getItemTemplate().isArmor() || item.getItemTemplate().isWeapon()) {
                    stmt.setInt(1, item.getObjectId());
                    
                    try (ResultSet rset = stmt.executeQuery()) {
                        while (rset.next()) {
                            int itemId = rset.getInt("item_id");
                            int slot = rset.getInt("slot");
                            int stoneType = rset.getInt("category");
                            
                            switch (stoneType) {
                                case 0: // ManaStone
                                    if (item.getSockets(false) <= item.getItemStonesSize()) {
                                        if (EnchantsConfig.CLEAN_STONE) {
                                            deleteItemStone(con, item.getObjectId(), slot, stoneType);
                                        }
                                        continue;
                                    }
                                    item.getItemStones().add(new ManaStone(item.getObjectId(), itemId, slot, PersistentState.UPDATED));
                                    break;
                                    
                                case 1: // GodStone
                                    item.setGodStone(new GodStone(item.getObjectId(), itemId, PersistentState.UPDATED));
                                    break;
                                    
                                case 2: // FusionStone
                                    if (item.getSockets(true) <= item.getFusionStonesSize()) {
                                        if (EnchantsConfig.CLEAN_STONE) {
                                            deleteItemStone(con, item.getObjectId(), slot, stoneType);
                                        }
                                        continue;
                                    }
                                    item.getFusionStones().add(new ManaStone(item.getObjectId(), itemId, slot, PersistentState.UPDATED));
                                    break;
                                    
                                case 3: // IdianStone
                                    item.setIdianStone(new IdianStone(itemId, PersistentState.UPDATE_REQUIRED, item, rset.getInt("polishNumber"), rset.getInt("polishCharge")));
                                    break;
                                    
                                default:
                                    log.warn("Unknown stone type: {} for item: {}", stoneType, item.getObjectId());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not restore ItemStoneList data from DB", e);
        }
    }
    
    @Override
    public void save(List<Item> items) {
        if (GenericValidator.isBlankOrNull(items)) {
            return;
        }
        
        Set<ManaStone> manaStones = Sets.newHashSet();
        Set<ManaStone> fusionStones = Sets.newHashSet();
        Set<GodStone> godStones = Sets.newHashSet();
        Set<IdianStone> idianStones = Sets.newHashSet();
        
        for (Item item : items) {
            if (item.hasManaStones()) {
                manaStones.addAll(item.getItemStones());
            }
            if (item.hasFusionStones()) {
                fusionStones.addAll(item.getFusionStones());
            }
            
            GodStone godStone = item.getGodStone();
            if (godStone != null) {
                godStones.add(godStone);
            }
            
            IdianStone idianStone = item.getIdianStone();
            if (idianStone != null) {
                idianStones.add(idianStone);
            }
        }
        
        store(manaStones, ItemStoneType.MANASTONE);
        store(fusionStones, ItemStoneType.FUSIONSTONE);
        store(godStones, ItemStoneType.GODSTONE);
        store(idianStones, ItemStoneType.IDIANSTONE);
    }
    
    @Override
    public void storeManaStones(Set<ManaStone> manaStones) {
        store(manaStones, ItemStoneType.MANASTONE);
    }
    
    @Override
    public void storeFusionStones(Set<ManaStone> fusionStones) {
        store(fusionStones, ItemStoneType.FUSIONSTONE);
    }
    
    @Override
    public void storeIdianStones(IdianStone idianStone) {
        store(Collections.singleton(idianStone), ItemStoneType.IDIANSTONE);
    }
    
    private void store(Set<? extends ItemStone> stones, ItemStoneType ist) {
        if (GenericValidator.isBlankOrNull(stones)) {
            return;
        }
        
        Set<? extends ItemStone> stonesToAdd = Sets.filter(stones, itemStoneAddPredicate);
        Set<? extends ItemStone> stonesToDelete = Sets.filter(stones, itemStoneDeletedPredicate);
        Set<? extends ItemStone> stonesToUpdate = Sets.filter(stones, itemStoneUpdatePredicate);
        
        Connection con = null;
        try {
            con = DatabaseFactory.getConnection();
            con.setAutoCommit(false);
            
            deleteItemStones(con, stonesToDelete, ist);
            addItemStones(con, stonesToAdd, ist);
            updateItemStones(con, stonesToUpdate, ist);
            
            con.commit();
        } catch (SQLException e) {
            log.error("Can't save stones", e);
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException rollbackEx) {
                log.error("Failed to rollback transaction", rollbackEx);
            }
        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                }
            } catch (SQLException e) {
                log.error("Failed to reset auto-commit", e);
            }
            DatabaseFactory.close(con);
        }
        
        for (ItemStone is : stones) {
            is.setPersistentState(PersistentState.UPDATED);
        }
    }
    
    private void addItemStones(Connection con, Collection<? extends ItemStone> itemStones, ItemStoneType ist) throws SQLException {
        if (GenericValidator.isBlankOrNull(itemStones)) {
            return;
        }
        
        try (PreparedStatement st = con.prepareStatement(INSERT_QUERY)) {
            for (ItemStone is : itemStones) {
                st.setInt(1, is.getItemObjId());
                st.setInt(2, is.getItemId());
                st.setInt(3, is.getSlot());
                st.setInt(4, ist.ordinal());
                
                if (is instanceof IdianStone) {
                    IdianStone stone = (IdianStone) is;
                    st.setInt(5, stone.getPolishNumber());
                    st.setInt(6, stone.getPolishCharge());
                } else {
                    st.setInt(5, 0);
                    st.setInt(6, 0);
                }
                
                st.addBatch();
            }
            
            st.executeBatch();
        } catch (SQLException e) {
            log.error("Error occurred while saving item stones", e);
            throw e;
        }
    }
    
    private void updateItemStones(Connection con, Collection<? extends ItemStone> itemStones, ItemStoneType ist) throws SQLException {
        if (GenericValidator.isBlankOrNull(itemStones)) {
            return;
        }
        
        try (PreparedStatement st = con.prepareStatement(UPDATE_QUERY)) {
            for (ItemStone is : itemStones) {
                st.setInt(1, is.getItemId());
                st.setInt(2, is.getSlot());
                
                if (is instanceof IdianStone) {
                    IdianStone stone = (IdianStone) is;
                    st.setInt(3, stone.getPolishNumber());
                    st.setInt(4, stone.getPolishCharge());
                } else {
                    st.setInt(3, 0);
                    st.setInt(4, 0);
                }
                
                st.setInt(5, is.getItemObjId());
                st.setInt(6, ist.ordinal());
                st.addBatch();
            }
            
            st.executeBatch();
        } catch (SQLException e) {
            log.error("Error occurred while updating item stones", e);
            throw e;
        }
    }
    
    private void deleteItemStones(Connection con, Collection<? extends ItemStone> itemStones, ItemStoneType ist) throws SQLException {
        if (GenericValidator.isBlankOrNull(itemStones)) {
            return;
        }
        
        try (PreparedStatement st = con.prepareStatement(DELETE_QUERY)) {
            for (ItemStone is : itemStones) {
                st.setInt(1, is.getItemObjId());
                st.setInt(2, is.getSlot());
                st.setInt(3, ist.ordinal());
                st.addBatch();
            }
            
            st.executeBatch();
        } catch (SQLException e) {
            log.error("Error occurred while deleting item stones", e);
            throw e;
        }
    }
    
    private void deleteItemStone(Connection con, int uid, int slot, int category) 
            throws SQLException {
        try (PreparedStatement st = con.prepareStatement(DELETE_QUERY)) {
            st.setInt(1, uid);
            st.setInt(2, slot);
            st.setInt(3, category);
            st.executeUpdate();
        } catch (SQLException e) {
            log.error("Error occurred while deleting item stone", e);
            throw e;
        }
    }
    
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}