package com.aionemu.gameserver.model.templates.tasks;

import java.util.Objects;

import com.aionemu.gameserver.persistence.dao.TaskDao;
import lombok.Setter;

/**
 * 任务 FromDB 处理器模板（静态数据/XML）。
 * XML template.
 * @author Divinity
 */
public abstract class TaskFromDBHandler implements Runnable {

	/**
	 * Spring 绑定的数据库任务 DAO / Spring-bound task-from-DB DAO.
	 */
	private static volatile TaskDao boundTaskFromDBDAO;

	@Setter
	protected int id;
	protected String[] params;

	/**
	 * 设置任务参数。 / Task's param(s).
	 */
	public void setParam(String[] params) {
		this.params = params;
	}

	/**
	 * 由 Spring 配置类在启动期绑定数据库任务 DAO。
	 * Binds the task-from-DB DAO from the Spring configuration at startup.
	 * @param dao 数据库任务 DAO / task-from-DB DAO
	 */
	public static void bindTaskFromDBDAO(TaskDao dao) {
		boundTaskFromDBDAO = Objects.requireNonNull(dao, "taskFromDBDAO must not be null");
	}

	/**
	 * 返回数据库任务 DAO；必须由 Spring 在启动期绑定，未绑定即失败。
	 * Returns the task-from-DB DAO; Spring must bind it at startup, otherwise it fails fast.
	 * @return 数据库任务 DAO / task-from-DB DAO
	 */
	private static TaskDao taskFromDBDAO() {
		TaskDao current = boundTaskFromDBDAO;
		if (current == null) {
			throw new IllegalStateException("TaskDao is not bound; Spring must bind it at startup");
		}
		return current;
	}

	/**
	 * 任务名称，用于与表字段 task 对照。 / The task's name This allow to check with the table column "task"
	 */
	public abstract String getTaskName();

	/**
	 * 检查任务参数是否有效。 / Check if the task's parameters are valid
	 * @return 有效返回 true，否则 false / true if valid, false otherwise
	 */
	public abstract boolean isValid();

	/**
	 * 经绑定的数据库任务 DAO 记录本次激活时间。 / Records the last activation via the bound task-from-DB DAO.
	 */
	protected void setLastActivation() {
		taskFromDBDAO().setLastActivation(id);
	}
}
