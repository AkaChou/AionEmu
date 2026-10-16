package com.aionemu.loginserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.loginserver.persistence.entity.AccountVipPo;
import com.mybatisflex.core.BaseMapper;

/**
 * 账号 VIP 映射层，INSERT IGNORE 与 UPSERT 语义无法用 CRUD 表达。
 */
public interface AccountVipMapper extends BaseMapper<AccountVipPo> {

	/** 为全部账号授予 VIP；已有记录同样提升到该等级与有效期（UPSERT）。 */
	@Insert("INSERT INTO ls_account_vip (account_id, vip_level, expire_time) "
		+ "SELECT id, #{level}, UNIX_TIMESTAMP() + #{days} * 24 * 3600 FROM ls_account_data "
		+ "ON DUPLICATE KEY UPDATE vip_level = #{level}, "
		+ "expire_time = UNIX_TIMESTAMP() + #{days} * 24 * 3600")
	int grantToAllAccounts(@Param("level") int level, @Param("days") int days);

	/** 单账号补齐 VIP 行，有效期为当前时间加 {@code days} 天。 */
	@Insert("INSERT IGNORE INTO ls_account_vip (account_id, vip_level, expire_time) "
		+ "VALUES (#{accountId}, #{level}, UNIX_TIMESTAMP() + #{days} * 24 * 3600)")
	int insertIfAbsent(@Param("accountId") int accountId, @Param("level") int level, @Param("days") int days);
}
