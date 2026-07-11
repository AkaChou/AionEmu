package com.aionemu.gameserver.dao.mysql8;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.dao.TaskFromDBDAO;
import com.aionemu.gameserver.model.tasks.TaskFromDB;
import java.sql.*;
import java.util.ArrayList;

/**
 * 数据库定时任务 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of TaskFromDBDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author Divinity
 */
@Slf4j
public class MySQL8TaskFromDBDAO extends TaskFromDBDAO {


	/** 查询全部任务 SQL / Select all tasks SQL*/
	private static final String SELECT_ALL_QUERY = "SELECT * FROM tasks ORDER BY id";
	/** 更新上次激活时间 SQL / Update last activation SQL */
	private static final String UPDATE_QUERY = "UPDATE tasks SET last_activation = ? WHERE id = ?";

	/**
	 * 加载全部数据库任务。
	 * Loads all database-driven tasks.
	 *
	 * task list
	 */
	@Override
	public ArrayList<TaskFromDB> getAllTasks() {
		final ArrayList<TaskFromDB> result = new ArrayList<TaskFromDB>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_ALL_QUERY);
			 ResultSet rset = stmt.executeQuery()) {

			while (rset.next()) {
				result.add(new TaskFromDB(
					rset.getInt("id"),
					rset.getString("task"),
					rset.getString("type"),
					rset.getTimestamp("last_activation"),
					rset.getString("start_time"),
					rset.getInt("delay"),
					rset.getString("param")
				));
            }
		} catch (SQLException e) {
			log.error(I18n.get("log.1261465dcfcf", e));
		}
		return result;
	}

	/**
	 * 更新任务最近激活时间。
	 * Updates the last activation time of a task.
	 *
	 * @param id 任务 ID / task id
	 */
	@Override
	public void setLastActivation(final int id) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

			stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
			stmt.setInt(2, id);
			stmt.executeUpdate();
		} catch (SQLException e) {
			log.error(I18n.get("log.b39cd8a3758d", e));
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 *
	 * @param s 数据库名 / database name
	 * @param i 主版本 / major version
	 * @param i1 次版本 / minor version
	 * whether supported
	 */
	@Override
	public boolean supports(String s, int i, int i1) {
		return MySQL8DAOUtils.supports(s, i, i1);
	}
}
