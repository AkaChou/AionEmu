package com.aionemu.gameserver.dao;

import java.util.ArrayList;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.tasks.TaskFromDB;

/**
 * 数据库定时任务数据访问抽象层。
 * DAO for scheduled tasks loaded from the database.
 *
 * @author Divinity
 */
public abstract class TaskFromDBDAO implements DAO {

	/**
	 * 查询数据库中全部定时任务。
	 * Returns all tasks from the database.
	 *
	 * task list
	 */
	public abstract ArrayList<TaskFromDB> getAllTasks();

	/**
	 * 将指定任务的最后激活时间设为 NOW()。
	 * Sets the last activation time of the task to NOW().
	 *
	 * @param id 任务 ID / task id
	 */
	public abstract void setLastActivation(final int id);

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return TaskFromDBDAO.class.getName();
	}
}
