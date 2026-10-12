package com.aionemu.loginserver.persistence.dao;

import com.aionemu.loginserver.model.Vip;
import com.aionemu.loginserver.persistence.entity.AccountVipPo;
import com.mybatisflex.core.service.IService;

/**
 * 账号 VIP 状态（ls_account_vip）数据访问层。
 */
public interface AccountVipDao extends IService<AccountVipPo> {

	Vip findByAccountId(int accountId);

	/** 为全部缺失 VIP 行的账号补齐记录（方言 SQL：INSERT IGNORE ... SELECT）。 */
	int syncMissingAccounts(int level);

	/** 单账号补齐 VIP 行，已存在时不插入（方言 SQL：INSERT IGNORE）。 */
	boolean insertIfAbsent(int accountId, int level);
}
