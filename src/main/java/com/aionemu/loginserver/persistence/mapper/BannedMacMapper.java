package com.aionemu.loginserver.persistence.mapper;

import java.sql.Timestamp;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.loginserver.persistence.entity.BannedMacPo;
import com.mybatisflex.core.BaseMapper;

/**
 * MAC 封禁映射层。
 * Mapper for MAC bans.
 * <p>
 * {@code replaceBan} 的 {@code REPLACE INTO} 是 MySQL 方言，Flex 1.11.8 无 SQL 级 upsert，故逐字保留。
 * The {@code REPLACE INTO} of {@code replaceBan} is MySQL dialect; Flex 1.11.8 has no SQL-level upsert, so it is kept.
 * </p>
 */
public interface BannedMacMapper extends BaseMapper<BannedMacPo> {

	/** 按地址替换写入封禁条目。 */
	@Insert("REPLACE INTO ls_banned_mac (address, time, details) VALUES (#{address}, #{time}, #{details})")
	int replaceBan(@Param("address") String address, @Param("time") Timestamp time, @Param("details") String details);
}
