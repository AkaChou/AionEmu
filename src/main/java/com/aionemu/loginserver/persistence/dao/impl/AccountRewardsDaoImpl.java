package com.aionemu.loginserver.persistence.dao.impl;

import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.loginserver.persistence.dao.AccountRewardsDao;
import com.aionemu.loginserver.persistence.entity.AccountRewardsPo;
import com.aionemu.loginserver.persistence.mapper.AccountRewardsMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * 账号待领取奖励数据访问实现。
 */
@Slf4j
@Service
@Lazy
public class AccountRewardsDaoImpl extends ServiceImpl<AccountRewardsMapper, AccountRewardsPo> implements AccountRewardsDao {

	@Override
	public long claimPendingRewards(int accountId) {
		List<AccountRewardsPo> pending;
		try {
			pending = mapper.selectListByQuery(QueryWrapper.create()
				.eq(AccountRewardsPo::getAccountId, accountId)
				.eq(AccountRewardsPo::getRewarded, 0));
		} catch (DataAccessException e) {
			return 0L;
		}
		long points = 0;
		for (AccountRewardsPo reward : pending) {
			points += reward.getPoints() == null ? 0 : reward.getPoints().longValue();
			log.info(I18n.get("log.e7ab6f9f7f1a", accountId, reward.getUniqId()));
		}
		if (!pending.isEmpty()) {
			try {
				UpdateChain.of(AccountRewardsPo.class)
					.set(AccountRewardsPo::getRewarded, 1)
					.setRaw("received", "NOW()")
					.in(AccountRewardsPo::getUniqId, pending.stream().map(AccountRewardsPo::getUniqId).toList())
					.update();
			} catch (DataAccessException e) {
				// 领取标记失败不影响点数返回，与旧实现一致 / marking failure keeps the summed points, same as before
			}
		}
		return points;
	}
}
