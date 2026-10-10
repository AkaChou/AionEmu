package com.aionemu.gameserver.dao.impl;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.outpost.OutpostLocation;

import lombok.extern.slf4j.Slf4j;

/**
 * 前哨据点 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of OutpostDAO using JdbcClient.
 * Created by Wnkrz on 27/08/2017. Updated for MySQL 8.
 */
@Slf4j
public class OutpostDAO extends com.aionemu.gameserver.dao.OutpostDAO {

    /** 查询前哨地点 SQL / Select outpost locations SQL */
    private static final String SELECT_QUERY = "SELECT `id`, `race` FROM `outpost_location` ORDER BY `id`";
    /** 更新前哨地点 SQL / Update outpost location SQL */
    private static final String UPDATE_QUERY = "UPDATE `outpost_location` SET `race` = ? WHERE `id` = ?";
    /** 插入或更新前哨地点 SQL / Insert or update outpost location SQL */
    private static final String INSERT_QUERY = "INSERT INTO `outpost_location` (`id`, `race`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `race` = VALUES(`race`)";

    private final JdbcClient jdbcClient;

    public OutpostDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    public OutpostDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 加载前哨据点；缺失记录会自动插入。
     * Loads outpost locations; missing rows are inserted automatically.
     * @param locations 前哨据点映射 / outpost location map
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean loadOutposLocations(Map<Integer, OutpostLocation> locations) {
        boolean success = true;

        try {
            Map<Integer, Boolean> loadedIds = new ConcurrentHashMap<>();

            jdbcClient.sql(SELECT_QUERY)
                .query((rs, rowNum) -> {
                    int id = rs.getInt("id");
                    OutpostLocation loc = locations.get(id);

                    if (loc != null) {
                        loc.setRace(Race.valueOf(rs.getString("race")));
                        loadedIds.put(id, true);
                    }
                    return null;
                })
                .list();

            for (Map.Entry<Integer, OutpostLocation> entry : locations.entrySet()) {
                if (!loadedIds.containsKey(entry.getKey())) {
                    insertOutpostLocation(entry.getValue());
                }
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.19142084445f"), e);
            success = false;
        }

        return success;
    }

    /**
     * 更新前哨据点。
     * Updates an outpost location.
     * @param location 据点位置 / outpost location
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean updateOutpostLocation(OutpostLocation location) {
        try {
            int updated = jdbcClient.sql(UPDATE_QUERY)
                .param(location.getRace().toString())
                .param(location.getId())
                .update();
            return updated > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.3e416c0dd49f", location.getId()), e);
            return false;
        }
    }

    /**
     * 插入前哨据点（默认种族 NPC）。
     * Inserts an outpost location (default race NPC).
     * @param location 据点位置 / outpost location
     * @return 是否成功 / whether successful
     */
    private boolean insertOutpostLocation(OutpostLocation location) {
        try {
            int inserted = jdbcClient.sql(INSERT_QUERY)
                .param(location.getId())
                .param(Race.NPC.toString())
                .update();
            return inserted > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.96d78ac95569", location.getId()), e);
            return false;
        }
    }

    /**
     * 批量更新前哨据点。
     * Batch-updates outpost locations.
     * @param locations 前哨据点映射 / outpost location map
     * @return 是否成功 / whether successful
     */
    public boolean updateOutpostLocations(Map<Integer, OutpostLocation> locations) {
        try {
            for (OutpostLocation loc : locations.values()) {
                jdbcClient.sql(UPDATE_QUERY)
                    .param(loc.getRace().toString())
                    .param(loc.getId())
                    .update();
            }
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.882bb7efcb76"), e);
            return false;
        }
    }

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
