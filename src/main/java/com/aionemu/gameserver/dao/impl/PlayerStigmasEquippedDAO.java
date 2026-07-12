package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.commons.utils.GenericValidator;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.skill.linked_skill.EquippedStigmasEntry;
import com.aionemu.gameserver.model.skill.linked_skill.PlayerEquippedStigmaList;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 玩家已装备灵魂石（Stigma）列表 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerStigmasEquippedDAO.
 */
@Slf4j
public class PlayerStigmasEquippedDAO extends com.aionemu.gameserver.dao.PlayerStigmasEquippedDAO {


    /** 插入已装备灵魂石 / Insert an equipped stigma */
    private static final String INSERT_QUERY = "INSERT INTO `player_stigmas_equipped` (`player_id`, `item_id`, `item_name`) VALUES (?,?,?)";

    /** 更新已装备灵魂石 / Update an equipped stigma */
    private static final String UPDATE_QUERY = "UPDATE `player_stigmas_equipped` SET item_id=?, item_name=? WHERE player_id=? AND item_id=?";

    /** 删除已装备灵魂石 / Delete an equipped stigma */
    private static final String DELETE_QUERY = "DELETE FROM `player_stigmas_equipped` WHERE `player_id`=? AND item_id=? AND item_name=?";

    /** 查询玩家已装备灵魂石 / Select equipped stigmas for a player */
    private static final String SELECT_QUERY = "SELECT `item_id`, `item_name` FROM `player_stigmas_equipped` WHERE `player_id`=?";

    /** 需要插入的条目过滤条件 / Predicate for items to insert */
    private static final Predicate<EquippedStigmasEntry> itemsToInsertPredicate = new Predicate<EquippedStigmasEntry>() {
        @Override
        public boolean apply(EquippedStigmasEntry input) {
            return input != null && PersistentState.NEW == input.getPersistentState();
        }
    };

    /** 需要更新的条目过滤条件 / Predicate for items to update */
    private static final Predicate<EquippedStigmasEntry> itemsToUpdatePredicate = new Predicate<EquippedStigmasEntry>() {
        @Override
        public boolean apply(EquippedStigmasEntry input) {
            return input != null && PersistentState.UPDATE_REQUIRED == input.getPersistentState();
        }
    };

    /** 需要删除的条目过滤条件 / Predicate for items to delete */
    private static final Predicate<EquippedStigmasEntry> itemsToDeletePredicate = new Predicate<EquippedStigmasEntry>() {
        @Override
        public boolean apply(EquippedStigmasEntry input) {
            return input != null && PersistentState.DELETED == input.getPersistentState();
        }
    };

    /**
     * 加载玩家已装备灵魂石列表。
     * Loads the player's equipped stigma list.
     *
     * player id
     *
     * @param playerId @return 已装备灵魂石列表 / equipped stigma list
     */
    @Override
    public PlayerEquippedStigmaList loadItemsList(int playerId) {
        List<EquippedStigmasEntry> items = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, playerId);

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("item_id");
                    String name = rset.getString("item_name");
                    items.add(new EquippedStigmasEntry(id, name, PersistentState.UPDATED));
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.484620fa0a55", playerId, e));
        }
        return new PlayerEquippedStigmaList(items);
    }

    /**
     * 持久化玩家已装备灵魂石（含新增、更新与删除）。
     * Persists the player's equipped stigmas (insert, update, and delete).
     *
     * 玩家 / player
     * whether successful
     */
    @Override
    public boolean storeItems(Player player) {
        List<EquippedStigmasEntry> skillsActive = Lists.newArrayList(
            player.getEquipedStigmaList().getAllItems()
        );
        List<EquippedStigmasEntry> skillsDeleted = Lists.newArrayList(
            player.getEquipedStigmaList().getDeletedItems()
        );

        store(player, skillsActive);
        store(player, skillsDeleted);
        return true;
    }

    /**
     * 在同一事务中按持久化状态分发增删改操作。
     * Dispatches insert/update/delete operations by persistent state in one transaction.
     *
     * 玩家 / player
     * @param skills 灵魂石条目列表 / stigma entry list
     */
    private void store(Player player, List<EquippedStigmasEntry> skills) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            deleteItems(con, player, skills);
            addItems(con, player, skills);
            updateItems(con, player, skills);

            con.commit();
        } catch (SQLException e) {
            log.error(I18n.get("log.6790b402b724", player.getObjectId(), e));
            return;
        }

        for (EquippedStigmasEntry skill : skills) {
            skill.setPersistentState(PersistentState.UPDATED);
        }
    }

    /**
     * 批量插入新增的灵魂石条目。
     * Batch-inserts newly equipped stigma entries.
     *
     * @param con 数据库连接 / database connection
     * @param player 玩家 / player
     * @param items 条目列表 / entry list
     */
    private void addItems(Connection con, Player player, List<EquippedStigmasEntry> items) {
        Collection<EquippedStigmasEntry> skillsToInsert = Collections2.filter(items, itemsToInsertPredicate);
        if (GenericValidator.isBlankOrNull(skillsToInsert)) {
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(INSERT_QUERY)) {
            for (EquippedStigmasEntry skill : skillsToInsert) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, skill.getItemId());
                ps.setString(3, skill.getItemName());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            log.error(I18n.get("log.b4df25263a5f", player.getObjectId(), e));
        }
    }

    /**
     * 批量更新待更新的灵魂石条目。
     * Batch-updates stigma entries that require update.
     *
     * @param con 数据库连接 / database connection
     * 玩家 / player
     * entry list
     */
    private void updateItems(Connection con, Player player, List<EquippedStigmasEntry> skills) {
        Collection<EquippedStigmasEntry> skillsToUpdate = Collections2.filter(skills, itemsToUpdatePredicate);
        if (GenericValidator.isBlankOrNull(skillsToUpdate)) {
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(UPDATE_QUERY)) {
            for (EquippedStigmasEntry skill : skillsToUpdate) {
                ps.setInt(1, skill.getItemId());
                ps.setString(2, skill.getItemName());
                ps.setInt(3, player.getObjectId());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            log.error(I18n.get("log.06bcbf6b0649", player.getObjectId(), e));
        }
    }

    /**
     * 批量删除已标记删除的灵魂石条目。
     * Batch-deletes stigma entries marked as deleted.
     *
     * @param con 数据库连接 / database connection
     * 玩家 / player
     * entry list
     */
    private void deleteItems(Connection con, Player player, List<EquippedStigmasEntry> skills) {
        Collection<EquippedStigmasEntry> skillsToDelete = Collections2.filter(skills, itemsToDeletePredicate);
        if (GenericValidator.isBlankOrNull(skillsToDelete)) {
            return;
        }

        try (PreparedStatement ps = con.prepareStatement(DELETE_QUERY)) {
            for (EquippedStigmasEntry skill : skillsToDelete) {
                ps.setInt(1, player.getObjectId());
                ps.setInt(2, skill.getItemId());
                ps.setString(3, skill.getItemName());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            log.error(I18n.get("log.a0aecdf644ac", player.getObjectId(), e));
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
