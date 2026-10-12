package com.aionemu.loginserver.persistence.mapper;

import org.apache.ibatis.annotations.Delete;

import com.aionemu.loginserver.persistence.entity.BannedIpPo;
import com.mybatisflex.core.BaseMapper;

/**
 * IP 封禁映射层，过期清理为方言 SQL。
 */
public interface BannedIpMapper extends BaseMapper<BannedIpPo> {

	/** 删除已过期的封禁记录。 */
	@Delete("DELETE FROM ls_banned_ip WHERE time_end < CURRENT_TIMESTAMP AND time_end IS NOT NULL")
	int cleanExpiredBans();
}
