package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.PlayerScripts;
import java.sql.*;

/**
 * 房屋脚本 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of HouseScriptsDAO.
 *
 * Updated for MySQL 8.
 *
 * @author Rolandas
 */
@Slf4j
public class HouseScriptsDAO extends com.aionemu.gameserver.dao.HouseScriptsDAO {


    /** Mergeinsert 更新 scriptSQL / Merge insert or update script SQL */
    private static final String MERGE_QUERY = "INSERT INTO `house_scripts` (`house_id`, `index`, `script`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `script` = VALUES(`script`)";
    /** 删除脚本 SQL / Delete script SQL */
    private static final String DELETE_QUERY = "DELETE FROM `house_scripts` WHERE `house_id` = ? AND `index` = ?";
    /** 删除房屋全部脚本 SQL / Delete all scripts of a house SQL */
    private static final String DELETE_ALL_QUERY = "DELETE FROM `house_scripts` WHERE `house_id` = ?";
    /** 查询房屋脚本 SQL / Select house scripts SQL */
    private static final String SELECT_QUERY = "SELECT `index`, `script` FROM `house_scripts` WHERE `house_id` = ? ORDER BY `index`";
    /** 次数房屋 scriptsSQL / Count house scripts SQL */
    private static final String SELECT_COUNT_QUERY = "SELECT COUNT(*) FROM `house_scripts` WHERE `house_id` = ?";

    /**
     * 添加房屋脚本（合并写入）。
     * Adds a house script (merge write).
     *
     * house id
     * script slot
     * script XML
     */
    @Override
    public void addScript(int houseId, int position, String scriptXML) {
        updateScript(houseId, position, scriptXML); // Use merge approach
    }

    /**
     * 获取房屋全部玩家脚本。
     * Gets all player scripts for a house.
     *
     * house id
     *
     * @param houseId
     * @return 玩家脚本集合 / player scripts
     */
    @Override
    public PlayerScripts getPlayerScripts(int houseId) {
        PlayerScripts scripts = new PlayerScripts(houseId);

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setInt(1, houseId);

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int position = rset.getInt("index");
                    String scriptXML = rset.getString("script");
                    scripts.addScript(position, scriptXML);
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.0121b4201018", houseId, e), e);
        }

        return scripts;
    }

    /**
     * 更新房屋脚本。
     * Updates a house script.
     *
     * house id
     * script slot
     * script XML
     */
    @Override
    public void updateScript(int houseId, int position, String scriptXML) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(MERGE_QUERY)) {

            stmt.setInt(1, houseId);
            stmt.setInt(2, position);

            if (scriptXML == null || scriptXML.isEmpty()) {
                stmt.setNull(3, Types.LONGVARCHAR);
            } else {
                stmt.setString(3, scriptXML);
            }

            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error(I18n.get("log.22094aa47afe", houseId, position, e), e);
        }
    }

    /**
     * 删除指定槽位脚本。
     * Deletes the script at the given slot.
     *
     * house id
     * script slot
     */
    @Override
    public void deleteScript(int houseId, int position) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_QUERY)) {

            stmt.setInt(1, houseId);
            stmt.setInt(2, position);
            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error(I18n.get("log.888a7b0cab84", houseId, position, e), e);
        }
    }

    /**
     * 删除房屋全部脚本。
     * Deletes all scripts of a house.
     *
     * house id
     */
    public void deleteAllScripts(int houseId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(DELETE_ALL_QUERY)) {

            stmt.setInt(1, houseId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            log.error(I18n.get("log.e7dedd0f512c", houseId, e), e);
        }
    }

    /**
     * 获取房屋脚本数量。
     * Gets the script count of a house.
     *
     * house id
     * script count
     */
    public int getScriptCount(int houseId) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_COUNT_QUERY)) {

            stmt.setInt(1, houseId);

            try (ResultSet rset = stmt.executeQuery()) {
                if (rset.next()) {
                    return rset.getInt(1);
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.921bc268cc85", houseId, e), e);
        }

        return 0;
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
        return DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
