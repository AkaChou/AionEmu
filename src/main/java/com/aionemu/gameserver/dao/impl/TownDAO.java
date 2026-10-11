package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.town.Town;

import lombok.extern.slf4j.Slf4j;

/**
 * 城镇（阵营城镇等级/积分）DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of TownDAO using JdbcClient.
 *
 * @author ViAl, Updated for MySQL 8
 */
@Slf4j
@Repository
public class TownDAO extends com.aionemu.gameserver.dao.TownDAO {

	/**
	 * 按阵营加载城镇 / Select towns by race
	 */
	private static final String SELECT_QUERY = "SELECT * FROM `towns` WHERE `race` = ? ORDER BY `id`";
	/**
	 * 加载全部城镇 / Select all towns
	 */
	private static final String SELECT_ALL_QUERY = "SELECT * FROM `towns` ORDER BY `race`, `id`";
	/**
	 * 插入城镇 / Insert a town
	 */
	private static final String INSERT_QUERY = "INSERT INTO `towns` (`id`, `level`, `points`, `race`, `level_up_date`) VALUES (?, ?, ?, ?, ?)";
	/**
	 * 更新城镇等级与积分 / Update town level and points
	 */
	private static final String UPDATE_QUERY = "UPDATE `towns` SET `level` = ?, `points` = ?, `level_up_date` = ? WHERE `id` = ?";
	/**
	 * 插入或更新城镇 / Upsert a town
	 */
	private static final String UPSERT_QUERY = "INSERT INTO `towns` (`id`, `level`, `points`, `race`, `level_up_date`) VALUES (?, ?, ?, ?, ?) " + "ON DUPLICATE KEY UPDATE `level` = VALUES(`level`), `points` = VALUES(`points`), `level_up_date` = VALUES(`level_up_date`)";

	private final JdbcClient jdbcClient;

	public TownDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public TownDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

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

		try {
			jdbcClient.sql(SELECT_QUERY)
				.param(race.toString())
				.query((rs, _) -> extractTownFromResultSet(rs))
				.list()
				.forEach(town -> towns.put(town.getId(), town));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.da2be87857a1", race), e);
		}

		return towns;
	}

	/**
	 * 从结果集提取城镇对象。
	 * Extracts a Town from the result set.
	 *
	 * @param rset 结果集 / result set
	 * @return 村庄 / town
	 * @throws SQLException SQL exception
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
		try {
			Timestamp levelUpDate = resolveLevelUpDate(town.getLevelUpDate());
			jdbcClient.sql(INSERT_QUERY)
				.param(town.getId())
				.param(town.getLevel())
				.param(town.getPoints())
				.param(town.getRace().toString())
				.param(levelUpDate)
				.update();
			town.setPersistentState(PersistentState.UPDATED);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.83ac20817a76", town.getId()), e);
		}
	}

	/**
	 * 更新城镇等级与积分；无行受影响时回退为插入。
	 * Updates town level and points; falls back to insert if no row is affected.
	 *
	 * @param town 村庄 / town
	 */
	private void updateTown(Town town) {
		try {
			Timestamp levelUpDate = resolveLevelUpDate(town.getLevelUpDate());
			int updated = jdbcClient.sql(UPDATE_QUERY)
				.param(town.getLevel())
				.param(town.getPoints())
				.param(levelUpDate)
				.param(town.getId())
				.update();

			if (updated == 0) {
				insertTown(town);
			} else {
				town.setPersistentState(PersistentState.UPDATED);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.86ef9ddbc0ca", town.getId()), e);
		}
	}

	/**
	 * 使用 UPSERT 写入城镇数据。
	 * Upserts town data.
	 *
	 * @param town 村庄 / town
	 */

	private static Timestamp resolveLevelUpDate(Timestamp levelUpDate) {
		if (levelUpDate == null || levelUpDate.getTime() < 1000000) {
			return new Timestamp(System.currentTimeMillis());
		}
		return levelUpDate;
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
