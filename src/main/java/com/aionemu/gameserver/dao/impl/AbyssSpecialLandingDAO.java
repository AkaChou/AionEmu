package com.aionemu.gameserver.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.model.landing_special.LandingSpecialStateType;

import lombok.extern.slf4j.Slf4j;

/**
 * 欧比斯特殊登陆点 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of AbyssSpecialLandingDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
public class AbyssSpecialLandingDAO extends com.aionemu.gameserver.dao.AbyssSpecialLandingDAO {

    /** 查询全部特殊登陆点 SQL / Select all special landing locations SQL*/
    private static final String SELECT_QUERY = "SELECT * FROM `special_landing`";
    /** 更新特殊登陆类型 SQL / Update special landing type SQL*/
    private static final String UPDATE_QUERY = "UPDATE `special_landing` SET `type` = ? WHERE `id` = ?";
    /** 插入特殊登陆点 SQL / Insert special landing location SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `special_landing` (`id`, `type`) VALUES(?, ?)";

    private final JdbcClient jdbcClient;

    public AbyssSpecialLandingDAO() {
        this(DatabaseFactory.getJdbcClient());
    }

    public AbyssSpecialLandingDAO(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
    }

    /**
     * 加载全部特殊登陆点状态，缺失的记录会自动插入。
     * Loads all special landing location states; missing rows are inserted automatically.
     * @param locations 模板位置映射 / template location map
     * @return 是否加载成功 / whether load succeeded
     */
    @Override
    public boolean loadLandingSpecialLocations(final Map<Integer, LandingSpecialLocation> locations) {
        boolean success = true;
        List<Integer> loaded = new ArrayList<>();

        try {
            jdbcClient.sql(SELECT_QUERY)
                .query((rs, rowNum) -> {
                    int id = rs.getInt("id");
                    LandingSpecialLocation loc = locations.get(id);
                    if (loc != null) {
                        loc.setType(LandingSpecialStateType.valueOf(rs.getString("type")));
                        loaded.add(loc.getId());
                    }
                    return null;
                })
                .list();
        } catch (DataAccessException e) {
            log.warn(I18n.get("log.5b9d0f9920d3"), e);
            success = false;
        }

        for (Map.Entry<Integer, LandingSpecialLocation> entry : locations.entrySet()) {
            LandingSpecialLocation sLoc = entry.getValue();
            if (!loaded.contains(sLoc.getId())) {
                insertLandingSpecialLocation(sLoc);
            }
        }
        return success;
    }

    /**
     * 持久化特殊登陆点。
     * Persists a special landing location.
     * @param location 特殊登陆点 / special landing location
     */
    @Override
    public void store(LandingSpecialLocation location) {
        updateLandingSpecialLocation(location);
    }

    /**
     * 更新特殊登陆点类型。
     * Updates the type of a special landing location.
     * @param locations 特殊登陆点 / special landing location
     * @return 是否更新成功 / whether update succeeded
     */
    @Override
    public boolean updateLandingSpecialLocation(final LandingSpecialLocation locations) {
        try {
            int affected = jdbcClient.sql(UPDATE_QUERY)
                .param(locations.getType().toString())
                .param(locations.getId())
                .update();
            return affected > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.2e2028503d2e", locations.getId()), e);
            return false;
        }
    }

    /**
     * 插入新的特殊登陆点（默认 NO_ACTIVE）。
     * Inserts a new special landing location (default NO_ACTIVE).
     * @param locations 特殊登陆点 / special landing location
     * @return 是否插入成功 / whether insert succeeded
     */
    private boolean insertLandingSpecialLocation(final LandingSpecialLocation locations) {
        locations.setType(LandingSpecialStateType.NO_ACTIVE);
        try {
            int inserted = jdbcClient.sql(INSERT_QUERY)
                .param(locations.getId())
                .param(LandingSpecialStateType.NO_ACTIVE.toString())
                .update();
            return inserted > 0;
        } catch (DataAccessException e) {
            log.error(I18n.get("log.7029001b4c33", locations.getId()), e);
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
