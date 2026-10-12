package com.aionemu.loginserver.persistence.dao.impl;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.loginserver.persistence.dao.TasksDao;
import com.aionemu.loginserver.persistence.entity.TasksPo;
import com.aionemu.loginserver.persistence.mapper.TasksMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据库驱动定时任务数据访问实现。
 */
@Slf4j
@Service
@Lazy
public class TasksDaoImpl extends ServiceImpl<TasksMapper, TasksPo> implements TasksDao {

	@Override
	public List<TasksPo> getAllTasks() {
		return mapper.selectListByQuery(QueryWrapper.create().orderBy(TasksPo::getId, true));
	}
}
