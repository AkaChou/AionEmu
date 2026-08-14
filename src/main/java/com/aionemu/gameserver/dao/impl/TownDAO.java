package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.town.Town;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 城镇（阵营城镇等级/积分）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of TownDAO.
 *
 * @author ViAl, Updated for MySQL 8
 */
@Slf4j
public class TownDAO extends com.aionemu.gameserver.dao.TownDAO {

    /** 按阵营加载城镇 / Select towns by race */
    private static final String SELECT_QUERY = "SELECT * FROM `towns` WHERE `race` = ? ORDER BY `id`";
    /** 加载全部城镇 / Select all towns */
    private static final String SELECT_ALL_QUERY = "SELECT * FROM `towns` ORDER BY `race`, `id`";
    /** 插入城镇 / Insert a town */
    private static final String INSERT_QUERY = "INSERT INTO `towns` (`id`, `level`, `points`, `race`, `level_up_date`) VALUES (?, ?, ?, ?, ?)";
    /** 更新城镇等级与积分 / Update town level and points */
    private static final String UPDATE_QUERY = "UPDATE `towns` SET `level` = ?, `points` = ?, `level_up_date` = ? WHERE `id` = ?";
    /** 插入或更新城镇 / Upsert a town */
    private static final String UPSERT_QUERY = "INSERT INTO `towns` (`id`, `level`, `points`, `race`, `level_up_date`) VALUES (?, ?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE `level` = VALUES(`level`), `points` = VALUES(`points`), `level_up_date` = VALUES(`level_up_date`)";

    /**
     * 按阵营加载城镇数据。
     * Loads towns for the given race.
     *
     * @param race 阵营 / race
     * @return 城镇 ID 到城镇的映射 / map of town id to town
     */
    @Override
    public Map<Integer, Town> load(Race race) {
        Map<Integer, Town> towns = new ConcurrentHashMap<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

            stmt.setString(1, race.toString());

            try (ResultSet rset = stmt.executeQuery()) {
                while (rset.next()) {
                    Town town = extractTownFromResultSet(rset);
                    towns.put(town.getId(), town);
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.da2be87857a1", race, e));
        }

        return towns;
    }

    /**
     * 加载全部城镇数据。
     * Loads all towns.
     *
     * @return 城镇 ID 到城镇的映射 / map of town id to town
     */
    public Map<Integer, Town> loadAll() {
        Map<Integer, Town> towns = new ConcurrentHashMap<>();

        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(SELECT_ALL_QUERY);
             ResultSet rset = stmt.executeQuery()) {

            while (rset.next()) {
                Town town = extractTownFromResultSet(rset);
                towns.put(town.getId(), town);
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.faf0ffd96bb1", e));
        }

        return towns;
    }

    /**
     * 从结果集提取城镇对象。
     * Extracts a Town from the result set.
     *
     * @param rset 结果集 / result set
     * @return 村庄 / town
     * SQL exception
     */
    private Town extractTownFromResultSet(ResultSet rset) throws SQLException {
        int id = rset.getInt("id");
        int level = rset.getInt("level");
        int points = rset.getInt("points");
        Race race = Race.valueOf(rset.getString("race"));
        Timestamp levelUpDate = rset.getTimestamp("level_up_date");

        Town town = new Town(id, level, points, race, levelUpDate);
        town.setPersistentState(PersistentState.UPDATED);

        return town;
    }

    /**
     * 按持久化状态存储城镇（新增或更新）。
     * Stores a town according to its persistent state (insert or update).
     *
     * @param town 村庄 / town
     */
    @Override
    public void store(Town town) {
        if (town == null) {
            return;
        }

        switch (town.getPersistentState()) {
            case NEW:
                insertTown(town);
                break;
            case UPDATE_REQUIRED:
                updateTown(town);
                break;
            default:
                break;
        }
    }

    /**
     * 插入新城镇记录。
     * Inserts a new town record.
     *
     * @param town 村庄 / town
     */
    private void insertTown(Town town) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(INSERT_QUERY)) {

            setTownStatementParameters(stmt, town);
            stmt.executeUpdate();
            town.setPersistentState(PersistentState.UPDATED);

        } catch (SQLException e) {
            log.error(I18n.get("log.83ac20817a76", town.getId(), e));
        }
    }

    /**
     * 更新城镇等级与积分；无行受影响时回退为插入。
     * Updates town level and points; falls back to insert if no row is affected.
     *
     * @param town 村庄 / town
     */
    private void updateTown(Town town) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

            stmt.setInt(1, town.getLevel());
            stmt.setInt(2, town.getPoints());

            Timestamp levelUpDate = town.getLevelUpDate();
            if (levelUpDate == null || levelUpDate.getTime() < 1000000) {
                stmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            } else {
                stmt.setTimestamp(3, levelUpDate);
            }

            stmt.setInt(4, town.getId());

            int updated = stmt.executeUpdate();

            if (updated == 0) {
                insertTown(town);
            } else {
                town.setPersistentState(PersistentState.UPDATED);
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.86ef9ddbc0ca", town.getId(), e));
        }
    }

    /**
     * 使用 UPSERT 写入城镇数据。
     * Upserts town data.
     *
     * @param town 村庄 / town
     */
    public void upsertTown(Town town) {
        try (Connection con = DatabaseFactory.getConnection();
             PreparedStatement stmt = con.prepareStatement(UPSERT_QUERY)) {

            setTownStatementParameters(stmt, town);
            stmt.executeUpdate();
            town.setPersistentState(PersistentState.UPDATED);

        } catch (SQLException e) {
            log.error(I18n.get("log.2d0c2046cc72", town.getId(), e));
        }
    }

    /**
     * 为预编译语句填充城镇字段。
     * Binds town fields to the prepared statement.
     *
     * @param stmt 预编译语句 / prepared statement
     * @param town 村庄 / town
     * SQL exception
     */
    private void setTownStatementParameters(PreparedStatement stmt, Town town) throws SQLException {
        stmt.setInt(1, town.getId());
        stmt.setInt(2, town.getLevel());
        stmt.setInt(3, town.getPoints());
        stmt.setString(4, town.getRace().toString());

        Timestamp levelUpDate = town.getLevelUpDate();
        if (levelUpDate == null || levelUpDate.getTime() < 1000000) {
            stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
        } else {
            stmt.setTimestamp(5, levelUpDate);
        }
    }

    /**
     * 批量 UPSERT 多个城镇。
     * Batch-upserts multiple towns.
     *
     * @param towns 城镇集合 / iterable of towns
     */
    public void storeTowns(Iterable<Town> towns) {
        try (Connection con = DatabaseFactory.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement stmt = con.prepareStatement(UPSERT_QUERY)) {
                int batchCount = 0;

                for (Town town : towns) {
                    if (town == null) {
                        continue;
                    }

                    setTownStatementParameters(stmt, town);
                    stmt.addBatch();
                    batchCount++;

                    if (batchCount % 50 == 0) {
                        stmt.executeBatch();
                    }
                }

                if (batchCount % 50 != 0) {
                    stmt.executeBatch();
                }
            }

            con.commit();

            for (Town town : towns) {
                if (town != null) {
                    town.setPersistentState(PersistentState.UPDATED);
                }
            }

        } catch (SQLException e) {
            log.error(I18n.get("log.e7999a48b385", e));
        }
    }

    /**
     * 判断当前数据库是否受本 DAO 支持。
     * Checks whether the given database is supported by this DAO.
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
