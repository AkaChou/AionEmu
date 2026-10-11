package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.gameobjects.player.PlayerScripts;

import lombok.extern.slf4j.Slf4j;

/**
 * 房屋脚本 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of HouseScriptsDAO using JdbcClient.
 * Updated for MySQL 8.
 * @author Rolandas
 */
@Slf4j
@Repository
public class HouseScriptsDAO extends com.aionemu.gameserver.dao.HouseScriptsDAO {

    /** 合并插入或更新脚本 SQL / Merge insert or update script SQL */
    private static final String MERGE_QUERY = "INSERT INTO `house_scripts` (`house_id`, `index`, `script`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `script` = VALUES(`script`)";
    /** 删除脚本 SQL / Delete script SQL */
    private static final String DELETE_QUERY = "DELETE FROM `house_scripts` WHERE `house_id` = ? AND `index` = ?";
    /** 删除房屋全部脚本 SQL / Delete all scripts of a house SQL */
    private static final String DELETE_ALL_QUERY = "DELETE FROM `house_scripts` WHERE `house_id` = ?";
    /** 查询房屋脚本 SQL / Select house scripts SQL */
    private static final String SELECT_QUERY = "SELECT `index`, `script` FROM `house_scripts` WHERE `house_id` = ? ORDER BY `index`";
    /** 次数房屋 scriptsSQL / Count house scripts SQL */
    private static final String SELECT_COUNT_QUERY = "SELECT COUNT(*) FROM `house_scripts` WHERE `house_id` = ?";

    private final JdbcClient jdbcClient;

    public HouseScriptsDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public HouseScriptsDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 添加房屋脚本（合并写入）。
     * Adds a house script (merge write).
     * @param houseId 房屋 ID / house id
     * @param position script slot
     * @param scriptXML script XML
     */
    @Override
    public void addScript(int houseId, int position, String scriptXML) {
        updateScript(houseId, position, scriptXML);
    }

    /**
     * 获取房屋全部玩家脚本。
     * Gets all player scripts for a house.
     * @param houseId 房屋 ID / house id
     * @return 玩家脚本集合 / player scripts
     */
    @Override
    public PlayerScripts getPlayerScripts(int houseId) {
        PlayerScripts scripts = new PlayerScripts(houseId);

        try {
            jdbcClient.sql(SELECT_QUERY)
                .param(houseId)
                .query((rs, _) -> {
                    int position = rs.getInt("index");
                    String scriptXML = rs.getString("script");
                    scripts.addScript(position, scriptXML);
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.0121b4201018", houseId), e);
        }

        return scripts;
    }

    /**
     * 更新房屋脚本。
     * Updates a house script.
     * @param houseId 房屋 ID / house id
     * @param position script slot
     * @param scriptXML script XML
     */
    @Override
    public void updateScript(int houseId, int position, String scriptXML) {
        try {
            String scriptParam = (scriptXML == null || scriptXML.isEmpty()) ? null : scriptXML;
            jdbcClient.sql(MERGE_QUERY)
                .param(houseId)
                .param(position)
                .param(scriptParam)
                .update();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.22094aa47afe", houseId, position), e);
        }
    }

    

    /**
     * 删除房屋全部脚本。
     * Deletes all scripts of a house.
     * @param houseId 房屋 ID / house id
     */
    
    /**
     * 获取房屋脚本数量。
     * Gets the script count of a house.
     * @param houseId 房屋 ID / house id
     * @return script count
     */
    
    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
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
