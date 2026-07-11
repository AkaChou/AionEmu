package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.PlayerTransformDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import java.sql.*;

/**
 * 玩家变身 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of PlayerTransformDAO.
 *
 * Updated for MySQL 8.
 */
@Slf4j
public class MySQL8PlayerTransfoDAO extends PlayerTransformDAO {


    /** 插入或更新变形 SQL / Insert or update transform SQL */
    private static final String INSERT_QUERY = "INSERT INTO `player_transform` (`player_id`, `panel_id`, `item_id`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `panel_id` = VALUES(`panel_id`), `item_id` = VALUES(`item_id`)";
    /** 查询变形 SQL / Select transform SQL */
    private static final String SELECT_QUERY = "SELECT `panel_id`, `item_id` FROM `player_transform` WHERE `player_id` = ?";
    /**
	 * Delete transform SQL / Delete transform SQL
	 */
    private static final String DELETE_QUERY = "DELETE FROM `player_transform` WHERE `player_id` = ?";
    /** 更新变形 SQL / Update transform SQL */
    private static final String UPDATE_QUERY = "UPDATE `player_transform` SET `panel_id` = ?, `item_id` = ? WHERE `player_id` = ?";

    /**
     * 加载玩家变身数据。
     * Loads player transform data.
     *
     * @param player 玩家 / player
     */
    @Override
    public void loadPlTransfo(Player player) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, player.getObjectId());

            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    int panelId = rset.getInt("panel_id");
                    int itemId = rset.getInt("item_id");

                    player.getTransformModel().setPanelId(panelId);
                    player.getTransformModel().setItemId(itemId);
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.81bdb310b4b8", player.getObjectId(), e));
        }
    }

    /**
     * 保存玩家变身数据。
     * Stores player transform data.
     *
     * player id
     * panel id
     * item id
     * whether successful
     */
    @Override
    public boolean storePlTransfo(int playerId, int panelId, int itemId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, playerId);
            stmt.setInt(2, panelId);
            stmt.setInt(3, itemId);

            int result = stmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.0ec4dcbf87ba", playerId, e));
            return false;
        }
    }

    /**
     * 删除玩家变身数据。
     * Deletes player transform data.
     *
     * player id
     * whether successful
     */
    @Override
    public boolean deletePlTransfo(int playerId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, playerId);

            int result = stmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.f096dcaaec6b", playerId, e));
            return false;
        }
    }

    /**
     * 更新玩家变身数据；若无记录则插入。
     * Updates player transform data; inserts if no row exists.
     *
     * player id
     * panel id
     * item id
     * whether successful
     */
    public boolean updatePlTransfo(int playerId, int panelId, int itemId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setInt(1, panelId);
            stmt.setInt(2, itemId);
            stmt.setInt(3, playerId);

            int updated = stmt.executeUpdate();

            if (updated == 0) {
                return storePlTransfo(playerId, panelId, itemId);
            }

            return true;

        } catch (SQLException e) {
            log.error(I18n.get("log.7aefe66a2cfb", playerId, e));
            return false;
        }
    }

    /**
     * 保存玩家当前变身模型状态。
     * Saves the player's current transform model state.
     *
     * 玩家 / player
     * whether successful
     */
    public boolean savePlTransfo(Player player) {
        int panelId = player.getTransformModel().getPanelId();
        int itemId = player.getTransformModel().getItemId();

        if (panelId == 0 && itemId == 0) {
            return deletePlTransfo(player.getObjectId());
        }

        return updatePlTransfo(player.getObjectId(), panelId, itemId);
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     *
     * database name
     * major version
     * minor version
     * whether supported
     */
    @Override
    public boolean supports(String databaseName, int majorVersion, int minorVersion) {
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
