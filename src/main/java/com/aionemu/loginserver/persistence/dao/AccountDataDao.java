package com.aionemu.loginserver.persistence.dao;

import com.aionemu.loginserver.model.Account;
import com.aionemu.loginserver.persistence.entity.AccountDataPo;
import com.mybatisflex.core.service.IService;

/**
 * 账号数据（ls_account_data）数据访问层。
 */
public interface AccountDataDao extends IService<AccountDataPo> {

	Account getAccount(String name);

	Account getAccount(int id);

	/** 插入新账号，回写数据库生成 ID；toll 初始为 0。 */
	boolean insertAccount(Account account);

	/** 更新账号业务字段（null 字段不落库）。 */
	boolean updateAccount(Account account);

	void updateLastServer(int accountId, byte lastServer);

	void updateLastIp(int accountId, String ip);

	String getLastIp(int accountId);

	boolean updateLastMac(int accountId, String mac);

	/** 会员到期后恢复旧会员等级（方言 SQL：expire < CURRENT_TIMESTAMP 条件更新）。 */
	boolean updateMembership(int accountId);

	/** 删除超过指定天数未活跃的账号（方言 SQL：子查询级联）。 */
	void deleteInactiveAccounts(int daysOfInactivity);

	/** 查询账号 Toll 点数余额，无记录时为 0。 */
	long getToll(int accountId);

	/** 写回扣减后的点数：toll = points - required。 */
	boolean updatePoints(int accountId, long points, long required);

	/** 查询账号 Luna 余额，无记录时为 0。 */
	long getLuna(int accountId);

	void updateLuna(int accountId, long luna);
}
