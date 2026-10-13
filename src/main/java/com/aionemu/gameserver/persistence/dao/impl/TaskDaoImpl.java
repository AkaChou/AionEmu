package com.aionemu.gameserver.persistence.dao.impl;

import java.util.ArrayList;
import java.util.Locale;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.tasks.TaskFromDB;
import com.aionemu.gameserver.persistence.dao.TaskDao;
import com.aionemu.gameserver.persistence.entity.TaskPo;
import com.aionemu.gameserver.persistence.mapper.TaskMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据库定时任务数据访问实现。
 * Data access implementation for tasks loaded from the database.
 */
@Slf4j
@Service
@Lazy
public class TaskDaoImpl extends ServiceImpl<TaskMapper, TaskPo> implements TaskDao {

	@Override
	public ArrayList<TaskFromDB> getAllTasks() {
		ArrayList<TaskFromDB> result = new ArrayList<>();
		try {
			// 库中 task_type 为大写枚举，处理器任务名为小写常量 / the column enum is upper-case, handler names are lower-case
			mapper.selectListByQuery(QueryWrapper.create().orderBy(TaskPo::getId, true)).forEach(po -> result.add(
				new TaskFromDB(po.getId(), po.getTaskType().toLowerCase(Locale.ROOT), po.getTriggerType(),
					po.getTriggerParam(), po.getExecParam())));
		} catch (DataAccessException e) {
			log.error(I18n.get("log.1261465dcfcf"), e);
		}
		return result;
	}
}
