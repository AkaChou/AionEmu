package com.aionemu.loginserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.loginserver.persistence.entity.AccountVipPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 账号 VIP 映射层，INSERT IGNORE 语义无法用 CRUD 表达。
 */
public interface AccountVipMapper extends BaseMapper<AccountVipPo> {

	/** 按账号表批量补齐 VIP 行，默认 30 天有效期。 */
	@Insert("INSERT IGNORE INTO ls_account_vip (account_id, vip_level, expire_time) "
		+ "SELECT id, #{level}, UNIX_TIMESTAMP() + 30 * 24 * 3600 FROM ls_account_data")
	int syncMissingAccounts(@Param("level") int level);

	/** 单账号补齐 VIP 行，默认 30 天有效期。 */
	@Insert("INSERT IGNORE INTO ls_account_vip (account_id, vip_level, expire_time) "
		+ "VALUES (#{accountId}, #{level}, UNIX_TIMESTAMP() + 30 * 24 * 3600)")
	int insertIfAbsent(@Param("accountId") int accountId, @Param("level") int level);
}
