package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Objects;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.tasks.TaskFromDB;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据库定时任务 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of TaskFromDBDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author Divinity
 */
@Slf4j
@Repository("gameTaskFromDBDAO")
public class TaskFromDBDAO extends com.aionemu.gameserver.dao.TaskFromDBDAO {

	/** 查询全部任务 SQL / Select all tasks SQL*/
	private static final String SELECT_ALL_QUERY = "SELECT * FROM tasks ORDER BY id";
	/** 更新上次激活时间 SQL / Update last activation SQL */
	private static final String UPDATE_QUERY = "UPDATE tasks SET last_activation = ? WHERE id = ?";

	private final JdbcClient jdbcClient;

	public TaskFromDBDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public TaskFromDBDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 加载全部数据库任务。
	 * Loads all database-driven tasks.
	 * @return 任务列表 / task list
	 */
	@Override
	public ArrayList<TaskFromDB> getAllTasks() {
		ArrayList<TaskFromDB> result = new ArrayList<>();

		try {
			result.addAll(jdbcClient.sql(SELECT_ALL_QUERY)
				.query((rset, rowNum) -> new TaskFromDB(
					rset.getInt("id"),
					rset.getString("task"),
					rset.getString("type"),
					rset.getTimestamp("last_activation"),
					rset.getString("start_time"),
					rset.getInt("delay"),
					rset.getString("param")
				))
				.list());
		} catch (DataAccessException e) {
			log.error(I18n.get("log.1261465dcfcf"), e);
		}
		return result;
	}

	/**
	 * 更新任务最近激活时间。
	 * Updates the last activation time of a task.
	 * @param id 任务 ID / task id
	 */
	@Override
	public void setLastActivation(final int id) {
		try {
			jdbcClient.sql(UPDATE_QUERY)
				.param(new Timestamp(System.currentTimeMillis()))
				.param(id)
				.update();
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b39cd8a3758d"), e);
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 * @param database 数据库名 / database name
	 * @param majorVersion 主版本 / major version
	 * @param minorVersion 次版本 / minor version
	 * @return 是否支持 / whether supported
	 */
	@Override
	public boolean supports(String database, int majorVersion, int minorVersion) {
		return DAOUtils.supports(database, majorVersion, minorVersion);
	}
}
