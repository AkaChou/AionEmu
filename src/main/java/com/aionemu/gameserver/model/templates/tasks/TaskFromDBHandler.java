package com.aionemu.gameserver.model.templates.tasks;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.TaskFromDBDAO;

/**
 * 任务 FromDB 处理器模板（静态数据/XML）。
 * XML template.
 *
 * @author Divinity
 */
public abstract class TaskFromDBHandler implements Runnable {

	protected int id;
	protected String params[];

	/**
	 * @param id 设置任务 ID。 / Task's id
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * 设置任务参数。 / Task's param(s).
	 */
	public void setParam(String params[]) {
		this.params = params;
	}

	/**
	 * 任务名称，用于与表字段 task 对照。 / The task's name This allow to check with the table column "task"
	 */
	public abstract String getTaskName();

	/**
	 * 检查任务参数是否有效。 / Check if the task's parameters are valid
	 *
	 * @return true if valid, false otherwise
	 */
	public abstract boolean isValid();

	/**
	 * 返回 TaskFromDBDAO（快捷方法）。 / Retuns {@link com.aionemu.gameserver.dao.TaskFromDBDAO} , just a shortcut.
	 */
	protected void setLastActivation() {
		DAOManager.getDAO(TaskFromDBDAO.class).setLastActivation(id);
	}
}
