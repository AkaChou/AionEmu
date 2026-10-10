package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * 服务器变量 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of ServerVariablesDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author Ben
 */
@Slf4j(topic = "com.aionemu.gameserver.dao.ServerVariablesDAO")
@Repository
public class ServerVariablesDAO extends com.aionemu.gameserver.dao.ServerVariablesDAO {

	/** 查询变量 SQL / Select variable SQL*/
	private static final String SELECT_QUERY = "SELECT `value` FROM `server_variables` WHERE `key`=?";
	/** Replace/upsert variable SQL / Replace/upsert variable SQL */
	private static final String REPLACE_QUERY = "REPLACE INTO `server_variables` (`key`,`value`) VALUES (?,?)";

	private final JdbcClient jdbcClient;

	public ServerVariablesDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public ServerVariablesDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 按键加载服务器变量整数值。
	 * Loads a server variable integer value by key.
	 * @param var 变量键 / variable key
	 * @return 变量值，不存在则 0 / value, or 0 if missing
	 */
	@Override
	public int load(String var) {
		try {
			String value = jdbcClient.sql(SELECT_QUERY)
				.param(var)
				.query(String.class)
				.optional()
				.orElse(null);
			if (value != null) {
				return Integer.parseInt(value);
			}
		} catch (DataAccessException e) {
			log.error(I18n.get("log.dc2937405e21"), e);
		}
		return 0;
	}

	/**
	 * 存储服务器变量。
	 * Stores a server variable.
	 * @param var 变量键 / variable key
	 * @param time 整数值 / integer value
	 * @return 是否写入成功 / whether the write succeeded
	 */
	@Override
	public boolean store(String var, int time) {
		try {
			int affected = jdbcClient.sql(REPLACE_QUERY)
				.param(var)
				.param(String.valueOf(time))
				.update();
			return affected > 0;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.38420527c181"), e);
			return false;
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 * @param databaseName 数据库名 / database name
	 * @param majorVersion 主版本 / major version
	 * @param minorVersion 次版本 / minor version
	 * @return 是否支持 / whether supported
	 */
	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
