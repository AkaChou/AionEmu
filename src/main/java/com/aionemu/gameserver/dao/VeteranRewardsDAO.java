package com.aionemu.gameserver.dao;

import java.util.Set;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.veteranrewards.VeteranRewards;

/**
 * 老兵奖励数据访问抽象层。
 * DAO for veteran reward persistence.
 */
public abstract class VeteranRewardsDAO implements DAO {

	/**
	 * 查询全部老兵奖励。
	 * Returns all veteran rewards.
	 *
	 * @return 老兵奖励集合 / veteran reward set
	 */
	public abstract Set<VeteranRewards> getVeteranReward();

	/**
	 * 删除指定老兵奖励。
	 * Deletes a veteran reward by id.
	 *
	 * veteran reward id
	 */
	public abstract void delVeteranReward(final int id_veteran_reward);

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return VeteranRewardsDAO.class.getName();
	}
}
