package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.base.BaseLocation;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基地位置（Base Location）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of BaseDAO.
 *
 * @author Updated for MySQL 8
 */
@Slf4j
public class BaseDAO extends com.aionemu.gameserver.dao.BaseDAO {


    /** 查询全部基地位置 / Select all base locations */
    private static final String SELECT_QUERY = "SELECT `id`, `race` FROM `base_location` ORDER BY `id`";
    /** 更新基地所属阵营 / Update base race ownership */
    private static final String UPDATE_QUERY = "UPDATE `base_location` SET `race` = ? WHERE `id` = ?";
    /** 插入基地位置 / Insert a base location */
    private static final String INSERT_QUERY = "INSERT INTO `base_location` (`id`, `race`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `race` = VALUES(`race`)";
    /** 按阵营查询基地位置 / Select base locations by race */
    private static final String SELECT_BY_RACE_QUERY = "SELECT `id`, `race` FROM `base_location` WHERE `race` = ?";

    /**
     * 加载基地位置数据到给定映射；缺失记录会自动插入默认 NPC 阵营。
     * Loads base location data into the given map; missing records are auto-inserted with default NPC race.
     *
     * @param locations 基地位置映射 / base location map
     * whether successful
     */
    @Override
    public boolean loadBaseLocations(Map<Integer, BaseLocation> locations) {
        boolean success = true;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
             ResultSet rset = stmt.executeQuery()) {

            Map<Integer, Boolean> loadedIds = new ConcurrentHashMap<>();

            while (rset.next()) {
                int id = rset.getInt("id");
                BaseLocation loc = locations.get(id);

                if (loc != null) {
                    loc.setRace(Race.valueOf(rset.getString("race")));
                    loadedIds.put(id, true);
                }
            }

            for (Map.Entry<Integer, BaseLocation> entry : locations.entrySet()) {
                if (!loadedIds.containsKey(entry.getKey())) {
                    insertBaseLocation(entry.getValue());
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.e6377177b72f", e));
            success = false;
        }

        return success;
    }

    /**
     * 更新单个基地位置的所属阵营。
     * Updates the race ownership of a single base location.
     *
     * base location
     * whether successful
     */
    @Override
    public boolean updateBaseLocation(BaseLocation location) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setString(1, location.getRace().toString());
            stmt.setInt(2, location.getId());

            int updated = stmt.executeUpdate();
            return updated > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.00e03bf592f1", location.getId(), e));
            return false;
        }
    }

    /**
     * 插入基地位置（默认 NPC 阵营）。
     * Inserts a base location with default NPC race.
     *
     * base location
     * whether successful
     */
    private boolean insertBaseLocation(BaseLocation location) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, location.getId());
            stmt.setString(2, Race.NPC.toString());

            int inserted = stmt.executeUpdate();
            return inserted > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.31d1da1209bd", location.getId(), e));
            return false;
        }
    }

    /**
     * 批量更新多个基地位置的所属阵营。
     * Batch-updates race ownership for multiple base locations.
     *
     * @param locations 基地位置映射 / base location map
     * whether successful
     */
    public boolean updateBaseLocations(Map<Integer, BaseLocation> locations) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
                for (BaseLocation loc : locations.values()) {
                    stmt.setString(1, loc.getRace().toString());
                    stmt.setInt(2, loc.getId());
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
            log.error(I18n.get("log.2d182cfdc4ca", e));
            return false;
        }
    }

    /**
     * 按阵营加载基地位置。
     * Loads base locations filtered by race.
     *
     * @param race 阵营 / race
     * @return 基地位置映射 / base location map
     */
    public Map<Integer, BaseLocation> loadBaseLocationsByRace(Race race) {
        Map<Integer, BaseLocation> locations = new ConcurrentHashMap<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_BY_RACE_QUERY)) {

            stmt.setString(1, race.toString());

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    int id = rset.getInt("id");
                    BaseLocation loc = locations.get(id);
                    if (loc != null) {
                        loc.setRace(Race.valueOf(rset.getString("race")));
                        locations.put(id, loc);
                    }
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.f5cd772ab16f", race, e));
        }

        return locations;
    }

    /**
     * 将全部基地位置重置为 NPC 阵营。
     * Resets all base locations to NPC race ownership.
     *
     * whether successful
     */
    public boolean resetBaseLocations() {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            con.setAutoCommit(false);

            try (PreparedStatement selectStmt = con.prepareStatement(SELECT_QUERY);
                 ResultSet rset = selectStmt.executeQuery()) {

                while (rset.next()) {
                    int id = rset.getInt("id");
                    stmt.setString(1, Race.NPC.toString());
                    stmt.setInt(2, id);
                    stmt.addBatch();
                }

                int[] results = stmt.executeBatch();
                con.commit();

                return true;
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.7fe78b3d8033", e));
            return false;
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
