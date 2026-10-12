package com.aionemu.loginserver.persistence.dao.impl;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.loginserver.persistence.dao.TaskDao;
import com.aionemu.loginserver.persistence.entity.TaskPo;
import com.aionemu.loginserver.persistence.mapper.LoginTaskMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据库驱动定时任务数据访问实现。
 * <p>与游戏服同名 {@code TaskDaoImpl} 做 bean 名区分：登录服显式命名为 {@code loginTaskDaoImpl}，
 * 避免跨域组件扫描的 bean 名冲突。</p>
 * <p>Bean name is disambiguated from the same-named game-server {@code TaskDaoImpl}: the login server
 * uses the explicit name {@code loginTaskDaoImpl} to avoid a cross-domain component-scan collision.</p>
 */
@Slf4j
@Service("loginTaskDaoImpl")
@Lazy
public class TaskDaoImpl extends ServiceImpl<LoginTaskMapper, TaskPo> implements TaskDao {

	@Override
	public List<TaskPo> getAllTasks() {
		return mapper.selectListByQuery(QueryWrapper.create().orderBy(TaskPo::getId, true));
	}
}
