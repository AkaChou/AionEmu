package com.aionemu.loginserver.persistence.dao;

import com.aionemu.loginserver.persistence.entity.AccountRewardsPo;
import com.mybatisflex.core.service.IService;

/**
 * 账号待领取奖励（ls_account_rewards）数据访问层。
 */
public interface AccountRewardsDao extends IService<AccountRewardsPo> {

	/** 汇总并领取待发放奖励（rewarded=0）：逐条记日志、标记已领取，返回点数合计。 */
	long claimPendingRewards(int accountId);
}
