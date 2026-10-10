package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeRace;

import lombok.extern.slf4j.Slf4j;

/**
 * 攻城 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of SiegeDAO using JdbcClient.
 * Updated for MySQL 8.
 */
@Slf4j
@Repository
public class SiegeDAO extends com.aionemu.gameserver.dao.SiegeDAO {

    /** 查询攻城地点 SQL / Select siege locations SQL*/
    private static final String SELECT_QUERY = "SELECT `id`, `race`, `legion_id` FROM `siege_locations` ORDER BY `id`";
    /** 插入或更新攻城地点 SQL / Insert or update siege location SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `siege_locations` (`id`, `race`, `legion_id`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `race` = VALUES(`race`), `legion_id` = VALUES(`legion_id`)";
    /** 更新攻城地点 SQL / Update siege location SQL*/
    private static final String UPDATE_QUERY = "UPDATE `siege_locations` SET `race` = ?, `legion_id` = ? WHERE `id` = ?";
    /** 按种族查询攻城地点 SQL / Select siege locations by race SQL*/
    private static final String SELECT_BY_RACE_QUERY = "SELECT `id`, `race`, `legion_id` FROM `siege_locations` WHERE `race` = ?";

    private final JdbcClient jdbcClient;

    public SiegeDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    @Autowired

    public SiegeDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 加载攻城据点数据。
     * Loads siege locations.
     * @param locations 地图位置 / location map
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean loadSiegeLocations(Map<Integer, SiegeLocation> locations) {
        boolean success = true;

        try {
            Map<Integer, Boolean> loadedIds = new ConcurrentHashMap<>();

            jdbcClient.sql(SELECT_QUERY)
                .query((rset, rowNum) -> {
                    int id = rset.getInt("id");
                    SiegeLocation loc = locations.get(id);

                    if (loc != null) {
                        loc.setRace(SiegeRace.valueOf(rset.getString("race")));
                        loc.setLegionId(rset.getInt("legion_id"));
                        loadedIds.put(id, true);
                    }
                    return null;
                })
                .list();

            for (Map.Entry<Integer, SiegeLocation> entry : locations.entrySet()) {
                if (!loadedIds.containsKey(entry.getKey())) {
                    insertSiegeLocation(entry.getValue());
                }
            }
        } catch (DataAccessException e) {
            log.error(I18n.get("log.f0edc322efd4"), e);
            success = false;
        }

        return success;
    }

    /**
     * 更新单个攻城据点。
     * Updates a single siege location.
     * @param siegeLocation 攻城战据点 / siege location
     * @return 是否成功 / whether succeeded
     */
    @Override
    public boolean updateSiegeLocation(SiegeLocation siegeLocation) {
        try {
            int updated = jdbcClient.sql(UPDATE_QUERY)
                .param(siegeLocation.getRace().toString())
                .param(siegeLocation.getLegionId())
                .param(siegeLocation.getLocationId())
                .update();
            return updated > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.98dc2b49ab4b", siegeLocation.getLocationId()), e);
            return false;
        }
    }

    /**
     * 插入攻城据点。
     * Inserts a siege location.
     * @param siegeLocation 攻城战据点 / siege location
     * @return 是否成功 / whether succeeded
     */
    private boolean insertSiegeLocation(SiegeLocation siegeLocation) {
        try {
            int inserted = jdbcClient.sql(INSERT_QUERY)
                .param(siegeLocation.getLocationId())
                .param(siegeLocation.getRace().toString())
                .param(siegeLocation.getLegionId())
                .update();
            return inserted > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.2d6381d9bee1", siegeLocation.getLocationId()), e);
            return false;
        }
    }

    /**
     * 批量更新攻城据点。
     * Batch updates siege locations.
     * @param locations 地图位置 / location map
     * @return 是否成功 / whether succeeded
     */
    public boolean updateSiegeLocations(Map<Integer, SiegeLocation> locations) {
        try {
            for (SiegeLocation loc : locations.values()) {
                jdbcClient.sql(UPDATE_QUERY)
                    .param(loc.getRace().toString())
                    .param(loc.getLegionId())
                    .param(loc.getLocationId())
                    .update();
            }
            return true;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.16223a055fa8"), e);
            return false;
        }
    }

    /**
     * 按种族加载攻城据点。
     * Loads siege locations by race.
     * @param race 阵营 / race
     * @return 地图位置 / location map
     */
    public Map<Integer, SiegeLocation> loadSiegeLocationsByRace(SiegeRace race) {
        Map<Integer, SiegeLocation> locations = new ConcurrentHashMap<>();

        try {
            jdbcClient.sql(SELECT_BY_RACE_QUERY)
                .param(race.toString())
                .query((rset, rowNum) -> {
                    int id = rset.getInt("id");
                    SiegeLocation loc = locations.get(id);
                    if (loc != null) {
                        loc.setRace(SiegeRace.valueOf(rset.getString("race")));
                        loc.setLegionId(rset.getInt("legion_id"));
                        locations.put(id, loc);
                    }
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.error(I18n.get("log.afdfcca54f61", race), e);
        }

        return locations;
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
