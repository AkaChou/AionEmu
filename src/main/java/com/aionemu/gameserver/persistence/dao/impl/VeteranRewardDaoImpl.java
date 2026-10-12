package com.aionemu.gameserver.persistence.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.model.veteranrewards.VeteranRewards;
import com.aionemu.gameserver.persistence.dao.VeteranRewardDao;
import com.aionemu.gameserver.persistence.entity.VeteranRewardPo;
import com.aionemu.gameserver.persistence.mapper.VeteranRewardMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 老兵奖励数据访问实现。
 * Data access implementation for veteran rewards.
 */
@Slf4j
@Service
@Lazy
public class VeteranRewardDaoImpl extends ServiceImpl<VeteranRewardMapper, VeteranRewardPo>
	implements VeteranRewardDao {

	/**
	 * 加载全部老兵奖励记录。
	 * Loads all veteran reward records.
	 *
	 * @return 奖励集 / reward set
	 */
	@Override
	public Set<VeteranRewards> getVeteranReward() {
		final Set<VeteranRewards> result = new HashSet<>();

		try {
			mapper.selectListByQuery(QueryWrapper.create().orderBy(VeteranRewardPo::getId, true))
				.forEach(po -> result.add(new VeteranRewards(po.getId(), po.getPlayer(), po.getType(),
					po.getItem(), po.getCount(), po.getKinah(), po.getSender(), po.getTitle(), po.getMessage())));
		} catch (Exception e) {
			log.error(I18n.get("log.3324f8b8d45c"), e);
		}
		return result;
	}

	/**
	 * 在调用方事务连接上按 ID 删除老兵奖励。
	 * Deletes a veteran reward by id on the caller's transactional connection.
	 *
	 * @param con 调用方事务连接 / caller's transactional connection
	 * @param rewardId 奖励 ID / reward id
	 * @throws SQLException 底层 JDBC 异常 / underlying JDBC failure
	 */
	@Override
	public void deleteInTransaction(Connection con, int rewardId) throws SQLException {
		try (PreparedStatement stmt = con.prepareStatement(VeteranRewardMapper.DELETE_QUERY_JDBC)) {
			stmt.setInt(1, rewardId);
			if (stmt.executeUpdate() == 0) {
				throw new SQLException("No veteran reward row deleted for id " + rewardId);
			}
		}
	}
}
