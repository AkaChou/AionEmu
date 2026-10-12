package com.aionemu.gameserver.persistence.mapper;

import com.aionemu.gameserver.persistence.entity.VeteranRewardPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 veteran_rewards 的映射层。
 * Mapper for table veteran_rewards.
 *
 * <p>删除语句经 deleteInTransaction 的原生 PreparedStatement 执行，故保留 ? 原文。
 * The delete statement runs through the raw PreparedStatement inside deleteInTransaction, so `?` is kept.
 */
public interface VeteranRewardMapper extends BaseMapper<VeteranRewardPo> {

	/**
	 * 删除老兵奖励；经 deleteInTransaction 的原生 PreparedStatement 执行，故保留 ? 原文。
	 * Delete statement; executed by raw PreparedStatement inside deleteInTransaction, so `?` is kept.
	 */
	String DELETE_QUERY_JDBC = "DELETE FROM veteran_rewards WHERE id = ?";
}
