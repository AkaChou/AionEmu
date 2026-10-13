package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import com.aionemu.gameserver.persistence.entity.ServerVariablePo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 server_variables 的映射层。
 * Mapper for table server_variables.
 * <p>
 * {@code replace} 的 {@code REPLACE INTO} 是 MySQL 方言，Flex 1.11.8 无 SQL 级 upsert，故逐字保留。
 * The {@code REPLACE INTO} of {@code replace} is MySQL dialect; Flex 1.11.8 has no SQL-level upsert, so it is kept.
 * </p>
 */
public interface ServerVariableMapper extends BaseMapper<ServerVariablePo> {

	/**
	 * 写入变量（{@code REPLACE INTO} 方言，仅 {@code ?} 改为具名参数）。
	 * Stores a variable ({@code REPLACE INTO} dialect, only {@code ?} turned into named parameters).
	 * @param var 变量键 / variable key
	 * @param value 变量值 / variable value
	 * @return 影响行数 / affected rows
	 */
	@Insert("REPLACE INTO `server_variables` (`key`,`value`) VALUES (#{var},#{value})")
	int replace(@Param("var") String var, @Param("value") String value);
}
