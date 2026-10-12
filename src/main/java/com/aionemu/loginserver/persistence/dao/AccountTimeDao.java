package com.aionemu.loginserver.persistence.dao;

import com.aionemu.loginserver.model.AccountTime;
import com.aionemu.loginserver.persistence.entity.AccountTimePo;
import com.mybatisflex.core.service.IService;

/**
 * 账号会话/惩罚时间（ls_account_time）数据访问层。
 */
public interface AccountTimeDao extends IService<AccountTimePo> {

	/** 写入或替换账号时间数据（方言 SQL：REPLACE INTO）。 */
	boolean updateAccountTime(int accountId, AccountTime accountTime);

	AccountTime getAccountTime(int accountId);
}
