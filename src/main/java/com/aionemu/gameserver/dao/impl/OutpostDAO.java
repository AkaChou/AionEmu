package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.outpost.OutpostLocation;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 前哨据点 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of OutpostDAO.
 *
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

    /**
     * 加载前哨据点；缺失记录会自动插入。
     * Loads outpost locations; missing rows are inserted automatically.
     *
     * @param locations 前哨据点映射 / outpost location map
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean loadOutposLocations(Map<Integer, OutpostLocation> locations) {
        boolean success = true;

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
             ResultSet rset = stmt.executeQuery()) {

            Map<Integer, Boolean> loadedIds = new ConcurrentHashMap<>();

            while (rset.next()) {
                int id = rset.getInt("id");
                OutpostLocation loc = locations.get(id);

                if (loc != null) {
                    loc.setRace(Race.valueOf(rset.getString("race")));
                    loadedIds.put(id, true);
                }
            }

            for (Map.Entry<Integer, OutpostLocation> entry : locations.entrySet()) {
                if (!loadedIds.containsKey(entry.getKey())) {
                    insertOutpostLocation(entry.getValue());
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.19142084445f", e));
            success = false;
        }

        return success;
    }

    /**
     * 更新前哨据点。
     * Updates an outpost location.
     *
     * @param location 据点位置 / outpost location
     * @return 是否成功 / whether successful
     */
    @Override
    public boolean updateOutpostLocation(OutpostLocation location) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setString(1, location.getRace().toString());
            stmt.setInt(2, location.getId());

            int updated = stmt.executeUpdate();
            return updated > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.3e416c0dd49f", location.getId(), e));
            return false;
        }
    }

    /**
     * 插入前哨据点（默认种族 NPC）。
     * Inserts an outpost location (default race NPC).
     *
     * @param location 据点位置 / outpost location
     * @return 是否成功 / whether successful
     */
    private boolean insertOutpostLocation(OutpostLocation location) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, location.getId());
            stmt.setString(2, Race.NPC.toString());

            int inserted = stmt.executeUpdate();
            return inserted > 0;

        } catch (SQLException e) {
            log.error(I18n.get("log.96d78ac95569", location.getId(), e));
            return false;
        }
    }

    /**
     * 批量更新前哨据点。
     * Batch-updates outpost locations.
     *
     * @param locations 前哨据点映射 / outpost location map
     * @return 是否成功 / whether successful
     */
    public boolean updateOutpostLocations(Map<Integer, OutpostLocation> locations) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {
                for (OutpostLocation loc : locations.values()) {
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
            log.error(I18n.get("log.882bb7efcb76", e));
            return false;
        }
    }

    /**
     * 是否支持当前数据库。
     * Whether the current database is supported.
     *
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
