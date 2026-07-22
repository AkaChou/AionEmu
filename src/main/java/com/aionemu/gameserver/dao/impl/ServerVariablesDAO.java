package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 服务器变量 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of ServerVariablesDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author Ben
 */
@Slf4j(topic = "com.aionemu.gameserver.dao.ServerVariablesDAO")
public class ServerVariablesDAO extends com.aionemu.gameserver.dao.ServerVariablesDAO {

	/** 查询变量 SQL / Select variable SQL*/
	private static final String SELECT_QUERY = "SELECT `value` FROM `server_variables` WHERE `key`=?";
	/** Replace/upsert variable SQL / Replace/upsert variable SQL */
	private static final String REPLACE_QUERY = "REPLACE INTO `server_variables` (`key`,`value`) VALUES (?,?)";

	/**
	 * 按键加载服务器变量整数值。
	 * Loads a server variable integer value by key.
	 *
	 * variable key
	 *
	 * @param var
	 * @return 变量值，不存在则 0 / value, or 0 if missing
	 */
	@Override
	public int load(String var) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(SELECT_QUERY)) {

			ps.setString(1, var);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return Integer.parseInt(rs.getString("value"));
				}
			}
		} catch (SQLException e) {
			log.error(I18n.get("log.dc2937405e21", e), e);
		}
		return 0;
	}

	/**
	 * 存储服务器变量。
	 * Stores a server variable.
	 *
	 * variable key
	 * integer value
	 *
	 * @return 是否写入成功 / whether the write succeeded
	 */
	@Override
	public boolean store(String var, int time) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement ps = con.prepareStatement(REPLACE_QUERY)) {

			ps.setString(1, var);
			ps.setString(2, String.valueOf(time));
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error(I18n.get("log.38420527c181", e), e);
			return false;
		}
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 *
	 * database name
	 * major version
	 * minor version
	 * whether supported
	 */
	@Override
	public boolean supports(String databaseName, int majorVersion, int minorVersion) {
		return DAOUtils.supports(databaseName, majorVersion, minorVersion);
	}
}
