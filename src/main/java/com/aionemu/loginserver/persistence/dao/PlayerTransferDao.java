package com.aionemu.loginserver.persistence.dao;

import java.util.List;

import com.aionemu.loginserver.persistence.entity.PlayerTransferPo;
import com.aionemu.loginserver.service.ptransfer.PlayerTransferTask;
import com.mybatisflex.core.service.IService;

/**
 * 角色跨服转移任务（ls_player_transfers）数据访问层。
 */
public interface PlayerTransferDao extends IService<PlayerTransferPo> {

	/** 查询尚未处理的转移任务（status=0）。 */
	List<PlayerTransferTask> getNew();

	/** 更新任务状态与备注；按状态写入完成时间（NOW()）。 */
	boolean update(PlayerTransferTask task);
}
