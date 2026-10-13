package com.aionemu.gameserver.model.templates.tasks;

import lombok.Setter;

/**
 * 任务 FromDB 处理器模板（静态数据/XML）。
 * XML template.
 * @author Divinity
 */
public abstract class TaskFromDBHandler implements Runnable {

	/**
	 * 库中的任务 ID，仅用于日志。
	 * Task id in the database, used for logging only.
	 */
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
	 * 任务名称，与库中 task_type 列对照（DAO 读取时已归一为小写）。
	 * The task's name, matched against the task_type column (normalised to lower case when read).
	 */
	public abstract String getTaskName();

	/**
	 * 检查任务参数是否有效。 / Check if the task's parameters are valid
	 * @return 有效返回 true，否则 false / true if valid, false otherwise
	 */
	public abstract boolean isValid();
}
