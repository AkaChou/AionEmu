package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeEntry;
import com.aionemu.gameserver.model.dorinerk_wardrobe.PlayerWardrobeList;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家衣柜（时装）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerWardrobeDAO.
 *
 * @author Ranastic
 */
@Slf4j
public class PlayerWardrobeDAO extends com.aionemu.gameserver.dao.PlayerWardrobeDAO {


    /** 插入或更新衣柜条目 / Insert or update a wardrobe entry */
    private static final String INSERT_OR_UPDATE = "INSERT INTO `player_wardrobe` (`player_id`, `item_id`, `slot`, `reskin_count`) VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE `item_id` = VALUES(`item_id`), `slot` = VALUES(`slot`)";

    /** 查询玩家衣柜全部条目 / Select all wardrobe entries for a player */
    private static final String SELECT_QUERY = "SELECT `item_id`,`slot`,`reskin_count` FROM `player_wardrobe` WHERE `player_id`=?";

    /** 删除指定衣柜条目 / Delete a specific wardrobe entry */
    private static final String DELETE_QUERY = "DELETE FROM `player_wardrobe` WHERE `player_id`=? AND `item_id`=?";

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

    /**
     * 加载玩家衣柜列表。
     * Loads the player's wardrobe list.
     *
     * 玩家 / player
     * wardrobe list
     */
    @Override
    public PlayerWardrobeList load(Player player) {
        List<PlayerWardrobeEntry> w = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int itemId = rset.getInt("item_id");
                    int slot = rset.getInt("slot");
                    int reskin = rset.getInt("reskin_count");
                    w.add(new PlayerWardrobeEntry(itemId, slot, reskin, PersistentState.UPDATED));
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.0c81ac00324b", player.getObjectId(), e), e);
        }
        return new PlayerWardrobeList(w);
    }

    /**
     * 保存衣柜条目（插入或更新）。
     * Stores a wardrobe entry (insert or update).
     *
     * player object id
     * item id
     * slot
     * reskin count
     * whether successful
     */
    @Override
    public boolean store(int objectId, int itemId, int slot, int reskin) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_OR_UPDATE)) {

            stmt.setInt(1, objectId);
            stmt.setInt(2, itemId);
            stmt.setInt(3, slot);
            stmt.setInt(4, reskin);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.7fed95e25235", objectId, e), e);
            return false;
        }
    }

    /**
     * 删除指定衣柜条目。
     * Deletes a wardrobe entry.
     *
     * player object id
     * item id
     * whether successful
     */
    @Override
    public boolean delete(int objectId, int itemId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, objectId);
            stmt.setInt(2, itemId);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.06e3e78e5e3b", objectId, e), e);
            return false;
        }
    }

    /**
     * 获取玩家衣柜物品数量。
     * Returns the number of wardrobe items for the player.
     *
     * player object id
     * item count
     */
    @Override
    public int getItemSize(int playerObjId) {
        String query = "SELECT COUNT(*) AS `size` FROM `player_wardrobe` WHERE `player_id`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setInt(1, playerObjId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("size");
                }
            }
        } catch (SQLException e) {
            log.debug("Could not get item size for player {}", playerObjId, e);
        }
        return 0;
    }

    /**
     * 按槽位获取衣柜物品 ID。
     * Returns the wardrobe item id for the given slot.
     *
     * @param obj 玩家对象 ID / player object id
     * slot
     *
     * @return 物品 ID，未找到时返回 0 / item id, or 0 if not found
     */
    @Override
    public int getWardrobeItemBySlot(final int obj, int slot) {
        String query = "SELECT `item_id` FROM `player_wardrobe` WHERE `player_id`=? AND `slot`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {

            s.setInt(1, obj);
            s.setInt(2, slot);

            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("item_id");
                }
            }
        } catch (SQLException e) {
            log.debug("No wardrobe item found for player {}, slot {}", obj, slot);
        }
        return 0;
    }

    /**
     * 按槽位获取重染次数。
     * Returns the reskin count for the given slot.
     *
     * @param obj 玩家对象 ID / player object id
     * slot
     *
     * @return 重染次数，未找到时返回 0 / reskin count, or 0 if not found
     */
    @Override
    public int getReskinCountBySlot(final int obj, int slot) {
        String query = "SELECT `reskin_count` FROM `player_wardrobe` WHERE `player_id`=? AND `slot`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement s = con.prepareStatement(query)) {

            s.setInt(1, obj);
            s.setInt(2, slot);

            try (ResultSet rs = s.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("reskin_count");
                }
            }
        } catch (SQLException e) {
            log.debug("No reskin count found for player {}, slot {}", obj, slot);
        }
        return 0;
    }

    /**
     * 按槽位设置重染次数。
     * Sets the reskin count for the given slot.
     *
     * @param obj 玩家对象 ID / player object id
     * slot
     * reskin count
     * whether successful
     */
    @Override
    public boolean setReskinCountBySlot(int obj, int slot, int reskin_count) {
        String query = "UPDATE player_wardrobe SET `reskin_count`=? WHERE `player_id`=? AND `slot`=?";
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setInt(1, reskin_count);
            stmt.setInt(2, obj);
            stmt.setInt(3, slot);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error(I18n.get("log.5acbf781683c", obj, slot, e), e);
            return false;
        }
    }
}
