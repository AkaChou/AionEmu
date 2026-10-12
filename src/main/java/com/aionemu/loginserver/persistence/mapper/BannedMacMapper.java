package com.aionemu.loginserver.persistence.mapper;

import java.sql.Timestamp;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.loginserver.persistence.entity.BannedMacPo;
import com.mybatisflex.core.BaseMapper;

/**
 * MAC 封禁映射层，REPLACE 与过期清理为方言 SQL。
 */
public interface BannedMacMapper extends BaseMapper<BannedMacPo> {

	/** 按地址替换写入封禁条目。 */
	@Insert("REPLACE INTO ls_banned_mac (address, time, details) VALUES (#{address}, #{time}, #{details})")
	int replaceBan(@Param("address") String address, @Param("time") Timestamp time, @Param("details") String details);

	/** 删除日期已过期的封禁记录。 */
	@Delete("DELETE FROM ls_banned_mac WHERE time < CURDATE()")
	int cleanExpiredBans();
}
