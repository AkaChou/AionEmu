package com.aionemu.gameserver.persistence.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.aionemu.gameserver.persistence.entity.ServerVariablePo;
import com.mybatisflex.core.BaseMapper;

/**
 * 表 server_variables 的映射层。
 * Mapper for table server_variables.
 * <p>
 * {@code key} 为 MySQL 保留字，故两条语句均以注解 SQL 保留反引号，不经 Flex CRUD。
 * {@code key} is a MySQL reserved word, so both statements keep the backticks as annotated SQL instead of Flex CRUD.
 * </p>
 */
public interface ServerVariableMapper extends BaseMapper<ServerVariablePo> {

	/**
	 * 读取变量值（原 {@code SELECT `value` ...}）。
	 * Selects a variable value (the legacy {@code SELECT `value` ...}).
	 * @param var 变量键 / variable key
	 * @return 变量值，不存在时 null / value, or null if missing
	 */
	@Select("SELECT `value` FROM `server_variables` WHERE `key`=#{var}")
	String selectValue(@Param("var") String var);

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
