package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.siege.SiegeLocation;
import com.aionemu.gameserver.model.siege.SiegeRace;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 攻城 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of SiegeDAO.
 *
 * Updated for MySQL 8.
 */
@Slf4j
public class SiegeDAO extends com.aionemu.gameserver.dao.SiegeDAO {


    /** 查询攻城地点 SQL / Select siege locations SQL*/
    private static final String SELECT_QUERY = "SELECT `id`, `race`, `legion_id` FROM `siege_locations` ORDER BY `id`";
    /** 插入或更新攻城地点 SQL / Insert or update siege location SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `siege_locations` (`id`, `race`, `legion_id`) VALUES (?, ?, ?) " + "ON DUPLICATE KEY UPDATE `race` = VALUES(`race`), `legion_id` = VALUES(`legion_id`)";
    /** 更新攻城地点 SQL / Update siege location SQL*/
    private static final String UPDATE_QUERY = "UPDATE `siege_locations` SET `race` = ?, `legion_id` = ? WHERE `id` = ?";
    /** 按种族查询攻城地点 SQL / Select siege locations by race SQL*/
    private static final String SELECT_BY_RACE_QUERY = "SELECT `id`, `race`, `legion_id` FROM `siege_locations` WHERE `race` = ?";

    /**
     * 加载攻城据点数据。
     * Loads siege locations.
     *
     * location map
     * whether succeeded
     */
    @Override
    public boolean loadSiegeLocations(Map<Integer, SiegeLocation> locations) {
        boolean success = true;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
             ResultSet rset = stmt.executeQuery()) {

            Map<Integer, Boolean> loadedIds = new ConcurrentHashMap<>();

            while (rset.next()) {
                int id = rset.getInt("id");
                SiegeLocation loc = locations.get(id);

                if (loc != null) {
                    loc.setRace(SiegeRace.valueOf(rset.getString("race")));
                    loc.setLegionId(rset.getInt("legion_id"));
                    loadedIds.put(id, true);
                }
            }

            for (Map.Entry<Integer, SiegeLocation> entry : locations.entrySet()) {
                if (!loadedIds.containsKey(entry.getKey())) {
                    insertSiegeLocation(entry.getValue());
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.f0edc322efd4", e), e);
            success = false;
        }

        return success;
    }

    /**
     * 更新单个攻城据点。
     * Updates a single siege location.
     *
     * siege location
     * whether succeeded
     */
    @Override
    public boolean updateSiegeLocation(SiegeLocation siegeLocation) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setString(1, siegeLocation.getRace().toString());
            stmt.setInt(2, siegeLocation.getLegionId());
            stmt.setInt(3, siegeLocation.getLocationId());

            int updated = stmt.executeUpdate();
            return updated > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.98dc2b49ab4b", siegeLocation.getLocationId(), e), e);
            return false;
        }
    }

    /**
     * 插入攻城据点。
     * Inserts a siege location.
     *
     * siege location
     * whether succeeded
     */
    private boolean insertSiegeLocation(SiegeLocation siegeLocation) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, siegeLocation.getLocationId());
            stmt.setString(2, siegeLocation.getRace().toString());
            stmt.setInt(3, siegeLocation.getLegionId());

            int inserted = stmt.executeUpdate();
            return inserted > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.2d6381d9bee1", siegeLocation.getLocationId(), e), e);
            return false;
        }
    }

    /**
     * 批量更新攻城据点。
     * Batch updates siege locations.
     *
     * location map
     * whether succeeded
     */
    public boolean updateSiegeLocations(Map<Integer, SiegeLocation> locations) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
                for (SiegeLocation loc : locations.values()) {
                    stmt.setString(1, loc.getRace().toString());
                    stmt.setInt(2, loc.getLegionId());
                    stmt.setInt(3, loc.getLocationId());
                    stmt.addBatch();
                }

                int[] results = stmt.executeBatch();
                con.commit();

                for (int result : results) {
                    if (result == PreparedStatement.EXECUTE_FAILED) {
                        return false;
                    }
                }
                return true;
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.16223a055fa8", e), e);
            return false;
        }
    }

    /**
     * 按种族加载攻城据点。
     * Loads siege locations by race.
     *
     * 阵营 / race
     * location map
     */
    public Map<Integer, SiegeLocation> loadSiegeLocationsByRace(SiegeRace race) {
        Map<Integer, SiegeLocation> locations = new ConcurrentHashMap<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_BY_RACE_QUERY)) {

            stmt.setString(1, race.toString());

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("id");
                    SiegeLocation loc = locations.get(id);
                    loc.setRace(SiegeRace.valueOf(rset.getString("race")));
                    loc.setLegionId(rset.getInt("legion_id"));
                    locations.put(id, loc);
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.afdfcca54f61", race, e), e);
        }

        return locations;
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
