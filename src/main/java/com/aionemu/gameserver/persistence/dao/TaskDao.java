package com.aionemu.gameserver.persistence.dao;

import java.util.ArrayList;

import com.aionemu.gameserver.model.tasks.TaskFromDB;
import com.aionemu.gameserver.persistence.entity.TaskPo;
import com.mybatisflex.core.service.IService;

/**
 * 数据库定时任务数据访问层。
 * Data access layer for scheduled tasks loaded from the database.
 */
public interface TaskDao extends IService<TaskPo> {

	/**
	 * 查询数据库中全部定时任务。
	 * Returns all tasks from the database.
	 * @return 任务列表 / task list
	 */
	ArrayList<TaskFromDB> getAllTasks();
}
