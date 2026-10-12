package com.aionemu.loginserver.persistence.mapper;

import java.sql.Timestamp;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.loginserver.persistence.entity.AccountTimePo;
import com.mybatisflex.core.BaseMapper;

/**
 * 账号时间映射层，REPLACE 语义无法用 CRUD 表达。
 */
public interface AccountTimeMapper extends BaseMapper<AccountTimePo> {

	/** 替换写入账号时间行（按 account_id 主键）。 */
	@Insert("REPLACE INTO ls_account_time (account_id, last_active, expiration_time, session_duration, "
		+ "accumulated_online, accumulated_rest, penalty_end) "
		+ "VALUES (#{accountId}, #{lastActive}, #{expirationTime}, #{sessionDuration}, "
		+ "#{accumulatedOnline}, #{accumulatedRest}, #{penaltyEnd})")
	int replaceAccountTime(@Param("accountId") int accountId, @Param("lastActive") Timestamp lastActive,
		@Param("expirationTime") Timestamp expirationTime, @Param("sessionDuration") int sessionDuration,
		@Param("accumulatedOnline") int accumulatedOnline, @Param("accumulatedRest") int accumulatedRest,
		@Param("penaltyEnd") Timestamp penaltyEnd);
}
