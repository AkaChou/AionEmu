package com.aionemu.gameserver.persistence.dao;

import java.util.Set;

import com.aionemu.gameserver.model.veteranrewards.VeteranRewards;
import com.aionemu.gameserver.persistence.entity.VeteranRewardPo;
import com.mybatisflex.core.service.IService;

/**
 * 老兵奖励数据访问抽象层。
 * DAO for veteran reward persistence.
 */
public interface VeteranRewardDao extends IService<VeteranRewardPo> {

	/**
	 * 查询全部老兵奖励。
	 * Returns all veteran rewards.
	 * @return 老兵奖励集合 / veteran reward set
	 */
	Set<VeteranRewards> getVeteranReward();

	/**
	 * 在调用方事务内按 ID 删除老兵奖励。
	 * Deletes a veteran reward by id inside the caller's transaction.
	 * @param rewardId 老兵奖励 ID / veteran reward id
	 */
	void deleteInTransaction(int rewardId);
}
