package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.landing.LandingLocation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 欧比斯登陆点 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of AbyssLandingDAO.
 */
@Slf4j
public class AbyssLandingDAO extends com.aionemu.gameserver.dao.AbyssLandingDAO {

    /** 查询全部登陆地点 SQL / Select all landing locations SQL*/
    private static final String SELECT_QUERY = "SELECT * FROM `abyss_landing`";
    /** 更新登陆地点 SQL / Update landing location SQL*/
    private static final String UPDATE_QUERY = "UPDATE `abyss_landing` SET `level` = ?, `siege` = ?, `commander` = ?, `artefact` = ?, `base` = ?, `monuments` = ?, `quest` = ?, `facility` = ?, `points` = ? WHERE `id` = ?";
    /** 插入登陆地点 SQL / Insert landing location SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `abyss_landing` (`id`, `level`, `siege`, `commander`, `artefact`, `base`, `monuments`, `quest`, `facility`, `level_up_date`, `race`, `points`) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /**
     * 持久化登陆点（委托更新）。
     * Persists a landing location (delegates to update).
     *
     * landing location
     */
    @Override
    public void store(LandingLocation location) {
        updateLandingLocation(location);
    }

    /**
     * 加载全部登陆点；缺失记录会自动插入。
     * Loads all landing locations; missing rows are inserted automatically.
     *
     * @param locations 登陆点映射 / landing location map
     * whether successful
     */
    @Override
    public boolean loadLandingLocations(final Map<Integer, LandingLocation> locations) {
        boolean success = true;
        List<Integer> loaded = new ArrayList<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
             ResultSet resultSet = stmt.executeQuery()) {

            while (resultSet.next()) {
                int locationId = resultSet.getInt("id");
                LandingLocation loc = locations.get(locationId);
                if (loc != null) {
                    loc.setLevel(resultSet.getInt("level"));
                    loc.setPoints(resultSet.getInt("points"));
                    loc.setArtifactPoints(resultSet.getInt("artefact"));
                    loc.setBasePoints(resultSet.getInt("base"));
                    loc.setCommanderPoints(resultSet.getInt("commander"));
                    loc.setQuestPoints(resultSet.getInt("quest"));
                    loc.setFacilityPoints(resultSet.getInt("facility"));
                    loc.setSiegePoints(resultSet.getInt("siege"));
                    loc.setMonumentsPoints(resultSet.getInt("monuments"));
                    loaded.add(locationId);
                }
            }
        } catch (Exception e) {
            log.warn(I18n.get("log.c4af5ae55182", e), e);
            success = false;
        }

        for (Map.Entry<Integer, LandingLocation> entry : locations.entrySet()) {
            LandingLocation sLoc = entry.getValue();
            if (!loaded.contains(sLoc.getId())) {
                insertLandingLocation(sLoc);
            }
        }
        return success;
    }

    /**
     * 更新登陆点。
     * Updates a landing location.
     *
     * landing location
     * whether successful
     */
    @Override
    public boolean updateLandingLocation(final LandingLocation locations) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setInt(1, locations.getLevel());
            stmt.setInt(2, locations.getSiegePoints());
            stmt.setInt(3, locations.getCommanderPoints());
            stmt.setInt(4, locations.getArtifactPoints());
            stmt.setInt(5, locations.getBasePoints());
            stmt.setInt(6, locations.getMonumentsPoints());
            stmt.setInt(7, locations.getQuestPoints());
            stmt.setInt(8, locations.getFacilityPoints());
            stmt.setInt(9, locations.getPoints());
            stmt.setInt(10, locations.getId());

            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            log.error(I18n.get("log.c2e97c849263", locations.getId(), e), e);
            return false;
        }
    }

    /**
     * 插入登陆点。
     * Inserts a landing location.
     *
     * landing location
     * whether successful
     */
    private boolean insertLandingLocation(final LandingLocation locations) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, locations.getId());
            stmt.setInt(2, locations.getLevel());
            stmt.setInt(3, locations.getSiegePoints());
            stmt.setInt(4, locations.getCommanderPoints());
            stmt.setInt(5, locations.getArtifactPoints());
            stmt.setInt(6, locations.getBasePoints());
            stmt.setInt(7, locations.getMonumentsPoints());
            stmt.setInt(8, locations.getQuestPoints());
            stmt.setInt(9, locations.getFacilityPoints());
            stmt.setTimestamp(10, new Timestamp(System.currentTimeMillis()));
            stmt.setString(11, locations.getTemplate().getRace().toString());
            stmt.setInt(12, locations.getPoints());

            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            log.error(I18n.get("log.34ceaeea74ab", locations.getId(), e), e);
            return false;
        }
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
