package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.AbyssSpecialLandingDAO;
import com.aionemu.gameserver.model.landing_special.LandingSpecialLocation;
import com.aionemu.gameserver.model.landing_special.LandingSpecialStateType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 欧比斯特殊登陆点 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of AbyssSpecialLandingDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 */
@Slf4j
public class MySQL8AbyssSpecialLandingDAO extends AbyssSpecialLandingDAO {

    /** 查询全部特殊登陆点 SQL / Select all special landing locations SQL*/
    private static final String SELECT_QUERY = "SELECT * FROM `special_landing`";
    /** 更新特殊登陆类型 SQL / Update special landing type SQL*/
    private static final String UPDATE_QUERY = "UPDATE `special_landing` SET `type` = ? WHERE `id` = ?";
    /** 插入特殊登陆点 SQL / Insert special landing location SQL*/
    private static final String INSERT_QUERY = "INSERT INTO `special_landing` (`id`, `type`) VALUES(?, ?)";

    /**
     * 加载全部特殊登陆点状态，缺失的记录会自动插入。
     * Loads all special landing location states; missing rows are inserted automatically.
     *
     * @param locations 模板位置映射 / template location map
     * @return 是否加载成功 / whether load succeeded
     */
    @Override
    public boolean loadLandingSpecialLocations(final Map<Integer, LandingSpecialLocation> locations) {
        boolean success = true;
        List<Integer> loaded = new ArrayList<Integer>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY);
             ResultSet resultSet = stmt.executeQuery()) {

            while (resultSet.next()) {
                LandingSpecialLocation loc = locations.get(resultSet.getInt("id"));
                if (loc != null) {
                    loc.setType(LandingSpecialStateType.valueOf(resultSet.getString("type")));
                    loaded.add(loc.getId());
                }
            }
        } catch (Exception e) {
            log.warn(I18n.get("log.5b9d0f9920d3", e));
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
     *
     * @param location 特殊登陆点 / special landing location
     */
    @Override
    public void store(LandingSpecialLocation location) {
        updateLandingSpecialLocation(location);
    }

    /**
     * 更新特殊登陆点类型。
     * Updates the type of a special landing location.
     *
     * @param locations 特殊登陆点 / special landing location
     * @return 是否更新成功 / whether update succeeded
     */
    @Override
    public boolean updateLandingSpecialLocation(final LandingSpecialLocation locations) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setString(1, locations.getType().toString());
            stmt.setInt(2, locations.getId());
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            log.error(I18n.get("log.2e2028503d2e", locations.getId(), e));
            return false;
        }
    }

    /**
     * 插入新的特殊登陆点（默认 NO_ACTIVE）。
     * Inserts a new special landing location (default NO_ACTIVE).
     *
     * @param locations 特殊登陆点 / special landing location
     * @return 是否插入成功 / whether insert succeeded
     */
    private boolean insertLandingSpecialLocation(final LandingSpecialLocation locations) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            stmt.setInt(1, locations.getId());
            stmt.setString(2, LandingSpecialStateType.NO_ACTIVE.toString());
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            log.error(I18n.get("log.7029001b4c33", locations.getId(), e));
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
        return MySQL8DAOUtils.supports(databaseName, majorVersion, minorVersion);
    }
}
