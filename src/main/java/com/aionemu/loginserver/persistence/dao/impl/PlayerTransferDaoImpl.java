package com.aionemu.loginserver.persistence.dao.impl;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.loginserver.persistence.dao.PlayerTransferDao;
import com.aionemu.loginserver.persistence.entity.PlayerTransferPo;
import com.aionemu.loginserver.persistence.mapper.PlayerTransferMapper;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferTask;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 角色跨服转移任务数据访问实现。
 */
@Slf4j
@Service
@Lazy
public class PlayerTransferDaoImpl extends ServiceImpl<PlayerTransferMapper, PlayerTransferPo> implements PlayerTransferDao {

	@Override
	public List<PlayerTransferTask> getNew() {
		try {
			return mapper.selectListByQuery(QueryWrapper.create().eq(PlayerTransferPo::getStatus, 0)).stream()
				.map(PlayerTransferDaoImpl::toTask)
				.toList();
		} catch (DataAccessException e) {
			return List.of();
		}
	}

	@Override
	public boolean update(PlayerTransferTask task) {
		try {
			UpdateChain<PlayerTransferPo> chain = UpdateChain.of(PlayerTransferPo.class)
				.set(PlayerTransferPo::getStatus, (int) task.status)
				.set(PlayerTransferPo::getComment, task.comment);
			switch (task.status) {
				case PlayerTransferTask.STATUS_ACTIVE -> chain.setRaw("time_performed", "NOW()");
				case PlayerTransferTask.STATUS_DONE, PlayerTransferTask.STATUS_ERROR -> chain.setRaw("time_done", "NOW()");
			}
			return chain.where(PlayerTransferPo::getId).eq(task.id).update();
		} catch (DataAccessException e) {
			return false;
		}
	}

	private static PlayerTransferTask toTask(PlayerTransferPo po) {
		PlayerTransferTask task = new PlayerTransferTask();
		task.id = po.getId();
		task.sourceServerId = po.getSourceServer().byteValue();
		task.targetServerId = po.getTargetServer().byteValue();
		task.sourceAccountId = po.getSourceAccountId();
		task.targetAccountId = po.getTargetAccountId();
		task.playerId = po.getPlayerId();
		return task;
	}
}
