package com.aionemu.loginserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.loginserver.persistence.entity.AccountPlaytimePo;
import com.mybatisflex.core.BaseMapper;

/**
 * 账号累计在线时长映射层，UPSERT 累加语义无法用 CRUD 表达。
 */
public interface AccountPlaytimeMapper extends BaseMapper<AccountPlaytimePo> {

	/** 插入或累加在线时长（重复键时累加既有值）。 */
	@Insert("INSERT INTO ls_account_playtime (account_id, accumulated_online) VALUES (#{accountId}, #{onlineTime}) "
		+ "ON DUPLICATE KEY UPDATE accumulated_online = accumulated_online + #{onlineTime}")
	int accumulateOnlineTime(@Param("accountId") long accountId, @Param("onlineTime") long onlineTime);
}
