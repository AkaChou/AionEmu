package com.aionemu.gameserver.persistence.dao;

import java.sql.Connection;
import java.sql.SQLException;
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
	 * 在调用方事务连接上按 ID 删除老兵奖励（原生 PreparedStatement，同事务）。
	 * Deletes a veteran reward by id on the caller's transactional connection (raw PreparedStatement).
	 * @param connection 调用方事务连接 / caller's transactional connection
	 * @param rewardId 老兵奖励 ID / veteran reward id
	 * @throws SQLException 底层 JDBC 异常 / underlying JDBC failure
	 */
	void deleteInTransaction(Connection connection, int rewardId) throws SQLException;
}
