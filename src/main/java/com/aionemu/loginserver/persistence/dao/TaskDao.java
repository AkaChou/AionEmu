package com.aionemu.loginserver.persistence.dao;

import java.util.List;

import com.aionemu.loginserver.persistence.entity.TaskPo;
import com.mybatisflex.core.service.IService;

/**
 * 数据库驱动定时任务（ls_tasks）数据访问层。
 */
public interface TaskDao extends IService<TaskPo> {

	/**
	 * 按 ID 升序返回全部任务行。
	 * Returns all task rows ordered by id ascending.
	 * @return 任务行列表 / task rows
	 */
	List<TaskPo> getAllTasks();
}
