package com.aionemu.loginserver.persistence.dao;

import com.aionemu.loginserver.model.AccountTime;
import com.aionemu.loginserver.persistence.entity.AccountPlaytimePo;
import com.mybatisflex.core.service.IService;

/**
 * 账号累计在线时长（ls_account_playtime）数据访问层。
 */
public interface AccountPlaytimeDao extends IService<AccountPlaytimePo> {

	/** 累加更新账号在线时长（方言 SQL：ON DUPLICATE KEY UPDATE 累加）。 */
	boolean update(Integer accountId, AccountTime accountTime);
}
