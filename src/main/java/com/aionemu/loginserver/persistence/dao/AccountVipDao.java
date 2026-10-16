package com.aionemu.loginserver.persistence.dao;

import com.aionemu.loginserver.model.Vip;
import com.aionemu.loginserver.persistence.entity.AccountVipPo;
import com.mybatisflex.core.service.IService;

/**
 * 账号 VIP 状态（ls_account_vip）数据访问层。
 */
public interface AccountVipDao extends IService<AccountVipPo> {

	Vip findByAccountId(int accountId);

	/** 为全部账号授予 VIP，已有记录同样提升（方言 SQL：INSERT ... SELECT ... ON DUPLICATE KEY UPDATE）。 */
	int grantToAllAccounts(int level, int days);

	/** 单账号补齐 VIP 行，已存在时不插入（方言 SQL：INSERT IGNORE）。 */
	boolean insertIfAbsent(int accountId, int level, int days);
}
