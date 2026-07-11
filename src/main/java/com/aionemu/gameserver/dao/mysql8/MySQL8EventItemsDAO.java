package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.EventItemsDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.event.MaxCountOfDay;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

/**
 * 活动物品计数 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of EventItemsDAO.
 *
 * Created by wanke on 03/03/2017.
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
public class MySQL8EventItemsDAO extends EventItemsDAO {

    /** 插入或更新活动物品数量 SQL / Insert or update event item counts SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `event_items` (`player_id`, `item_id`, `counts`) VALUES (?,?,?) " + "ON DUPLICATE KEY UPDATE `counts` = VALUES(`counts`)";
    /** 删除活动物品按玩家 SQL / Delete event items by player SQL*/
    private static final String DELETE_QUERY = "DELETE FROM `event_items` WHERE `player_id`=?";
    /** 删除按物品 ID 的活动物品 SQL / Delete event items by item id SQL*/
    private static final String DELETE_ITEM_QUERY = "DELETE FROM `event_items` WHERE `item_id`=?";
    /** 查询活动物品数量 SQL / Select event item counts SQL*/
    private static final String SELECT_QUERY = "SELECT `item_id`, `counts` FROM `event_items` WHERE `player_id`=?";

    /**
     * 加载玩家活动物品当日上限计数。
     * Loads daily event item max-count data for a player.
     *
     * @param player 玩家 / player
     */
    @Override
    public void loadItems(final Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int itemId = rset.getInt("item_id");
                    int counts = rset.getInt("counts");
                    player.addItemMaxCountOfDay(itemId, counts);
                }
            }
        } catch (SQLException e) {
            log.error(I18n.get("log.5aff73432829", player.getObjectId(), e));
        }
    }

    /**
     * 保存玩家活动物品当日上限计数。
     * Stores daily event item max-count data for a player.
     *
     * @param player 玩家 / player
     */
    @Override
    public void storeItems(Player player) {
        deleteItems(player);

        Map<Integer, MaxCountOfDay> itemsm = player.getItemMaxThisCounts();
        if (itemsm == null || itemsm.isEmpty()) {
            return;
        }

        final Iterator<Map.Entry<Integer, MaxCountOfDay>> iterator = itemsm.entrySet().iterator();
        if (!iterator.hasNext()) {
            return;
        }

        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement st = con.prepareStatement(INSERT_QUERY)) {
                while (iterator.hasNext()) {
                    Map.Entry<Integer, MaxCountOfDay> entry = iterator.next();
                    st.setInt(1, player.getObjectId());
                    st.setInt(2, entry.getKey());
                    st.setInt(3, entry.getValue().getThisCount());
                    st.addBatch();
                }
                st.executeBatch();
            }

            con.commit();
            player.clearItemMaxThisCount();
        } catch (SQLException e) {
            log.error(I18n.get("log.cdff3db2bd19", player.getObjectId(), e));
        }
    }

    /**
     * 按物品 ID 删除全部玩家的活动物品记录。
     * Deletes event item records for all players by item id.
     *
     * item id
     */
    @Override
    public void deleteItems(final int itemId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_ITEM_QUERY)) {

            stmt.setInt(1, itemId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.903226c3be37", itemId, e));
        }
    }

    /**
     * 删除指定玩家的全部活动物品记录。
     * Deletes all event item records for a player.
     *
     * @param player 玩家 / player
     */
    private void deleteItems(final Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, player.getObjectId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error(I18n.get("log.bf40ce06a63c", player.getObjectId(), e));
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     *
     * @param arg0 数据库名 / database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String arg0, int arg1, int arg2) {
        return MySQL8DAOUtils.supports(arg0, arg1, arg2);
    }
}
