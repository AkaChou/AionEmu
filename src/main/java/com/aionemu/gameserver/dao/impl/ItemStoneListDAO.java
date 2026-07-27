package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.configs.main.EnchantsConfig;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.items.GodStone;
import com.aionemu.gameserver.model.items.IdianStone;
import com.aionemu.gameserver.model.items.ItemStone;
import com.aionemu.gameserver.model.items.ItemStone.ItemStoneType;
import com.aionemu.gameserver.model.items.ManaStone;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import java.sql.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 物品镶嵌石（Mana/God/Fusion/Idian 等）列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of ItemStoneListDAO.
 */
@Slf4j
public class ItemStoneListDAO extends com.aionemu.gameserver.dao.ItemStoneListDAO {


    /** 插入镶嵌石 / Insert item stone */
    public static final String INSERT_QUERY = "INSERT INTO `item_stones` " + "(`item_unique_id`, `item_id`, `slot`, `category`, `polishNumber`, `polishCharge`, `proc_count`) " + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    /** 更新镶嵌石 / Update item stone */
    public static final String UPDATE_QUERY = "UPDATE `item_stones` SET " + "`item_id` = ?, `slot` = ?, `polishNumber` = ?, `polishCharge` = ?, `proc_count` = ? " + "WHERE `item_unique_id` = ? AND `category` = ?";

    /** 删除镶嵌石 / Delete item stone */
    public static final String DELETE_QUERY = "DELETE FROM `item_stones` " + "WHERE `item_unique_id` = ? AND `slot` = ? AND `category` = ?";

    /** 查询物品镶嵌石 / Select item stones */
    public static final String SELECT_QUERY = "SELECT `item_id`, `slot`, `category`, " + "`polishNumber`, `polishCharge`, `proc_count` FROM `item_stones` WHERE `item_unique_id` = ?";

    /** 筛选需新增的镶嵌石 / Filter stones to insert */
    private static final Predicate<ItemStone> itemStoneAddPredicate =
        new Predicate<ItemStone>() {
            @Override
            public boolean apply(ItemStone itemStone) {
                return itemStone != null && PersistentState.NEW == itemStone.getPersistentState();
            }
        };

    /** 筛选需删除的镶嵌石 / Filter stones to delete */
    private static final Predicate<ItemStone> itemStoneDeletedPredicate =
        new Predicate<ItemStone>() {
            @Override
            public boolean apply(ItemStone itemStone) {
                return itemStone != null && PersistentState.DELETED == itemStone.getPersistentState();
            }
        };

    /** 筛选需更新的镶嵌石 / Filter stones to update */
    private static final Predicate<ItemStone> itemStoneUpdatePredicate =
        new Predicate<ItemStone>() {
            @Override
            public boolean apply(ItemStone itemStone) {
                return itemStone != null && PersistentState.UPDATE_REQUIRED == itemStone.getPersistentState();
            }
        };

    /**
     * 为武器/防具加载镶嵌石（Mana/God/Fusion/Idian）。
     * Loads item stones (Mana/God/Fusion/Idian) for weapons and armor.
     *
     * @param items 物品集合 / item collection
     */
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
                                    item.setGodStone(new GodStone(item.getObjectId(), itemId, rset.getInt("proc_count"), PersistentState.UPDATED));
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
                                    log.warn(I18n.get("log.92b064ec3143", stoneType, item.getObjectId()));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(I18n.get("log.112e060c3cda", e));
        }
    }

    /**
     * 保存物品上的全部镶嵌石变更。
     * Saves all item-stone changes for the given items.
     *
     * @param items 物品列表 / item list
     */
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

    /**
     * 持久化 Mana 石集合。
     * Persists a set of mana stones.
     *
     * mana stone set
     */
    @Override
    public void storeManaStones(Set<ManaStone> manaStones) {
        store(manaStones, ItemStoneType.MANASTONE);
    }

    /**
     * 持久化融合石集合。
     * Persists a set of fusion stones.
     *
     * @param fusionStones 融合石集合 / fusion stone set
     */
    @Override
    public void storeFusionStones(Set<ManaStone> fusionStones) {
        store(fusionStones, ItemStoneType.FUSIONSTONE);
    }

    /**
     * 持久化单颗 Idian 石。
     * Persists a single Idian stone.
     *
     * Idian stone
     */
    @Override
    public void storeIdianStones(IdianStone idianStone) {
        store(Collections.singleton(idianStone), ItemStoneType.IDIANSTONE);
    }

    /**
     * 按持久化状态批量增删改镶嵌石。
     * Batch inserts/updates/deletes stones according to persistent state.
     *
     * @param stones 镶嵌石集合 / stone set
     * @param ist 镶嵌石类型 / item stone type
     */
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
            log.error(I18n.get("log.441d0f828363", e));
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException rollbackEx) {
                log.error(I18n.get("log.469fdfa81ee5", rollbackEx));
            }
            return;
        } finally {
            try {
                if (con != null) {
                    con.setAutoCommit(true);
                }
            } catch (SQLException e) {
                log.error(I18n.get("log.42b10c59c466", e));
            }
            DatabaseFactory.close(con);
        }

        for (ItemStone is : stones) {
            is.setPersistentState(PersistentState.UPDATED);
        }
    }

    /**
     * 批量插入镶嵌石。
     * Batch-inserts item stones.
     *
     * @param con 数据库连接 / database connection
     * @param itemStones 镶嵌石集合 / stone collection
     * @param ist 镶嵌石类型 / item stone type
     * SQL exception
     */
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
				st.setInt(7, is instanceof GodStone godStone ? godStone.getActivatedCount() : 0);

                st.addBatch();
            }

            st.executeBatch();
        } catch (SQLException e) {
            log.error(I18n.get("log.ff24b8461da0", e));
            throw e;
        }
    }

    /**
     * 批量更新镶嵌石。
     * Batch-updates item stones.
     *
     * @param con 数据库连接 / database connection
     * @param itemStones 镶嵌石集合 / stone collection
     * @param ist 镶嵌石类型 / item stone type
     * SQL exception
     */
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

				st.setInt(5, is instanceof GodStone godStone ? godStone.getActivatedCount() : 0);
				st.setInt(6, is.getItemObjId());
				st.setInt(7, ist.ordinal());
                st.addBatch();
            }

            st.executeBatch();
        } catch (SQLException e) {
            log.error(I18n.get("log.9292d96bede8", e));
            throw e;
        }
    }

    /**
     * 批量删除镶嵌石。
     * Batch-deletes item stones.
     *
     * @param con 数据库连接 / database connection
     * @param itemStones 镶嵌石集合 / stone collection
     * @param ist 镶嵌石类型 / item stone type
     * SQL exception
     */
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
            log.error(I18n.get("log.e2ee88575c70", e));
            throw e;
        }
    }

    /**
     * 删除单颗镶嵌石（用于超限清理）。
     * Deletes a single item stone (used when sockets overflow).
     *
     * @param con 数据库连接 / database connection
     * @param uid 物品唯一 ID / item unique id
     * slot
     * category
     * SQL exception
     */
    private void deleteItemStone(Connection con, int uid, int slot, int category)
            throws SQLException {
        try (PreparedStatement st = con.prepareStatement(DELETE_QUERY)) {
            st.setInt(1, uid);
            st.setInt(2, slot);
            st.setInt(3, category);
            st.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.48e07380c52b", e));
            throw e;
        }
    }

    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
     *
     * @param databaseName 数据库名称 / database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
