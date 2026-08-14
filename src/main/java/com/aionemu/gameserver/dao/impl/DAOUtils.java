package com.aionemu.gameserver.dao.impl;

/**
 * MySQL 8 DAO 工具类。
 * Utility helpers for MySQL 8 DAOs.
 *
 * @author SoulKeeper
 */
public class DAOUtils {

	/** MySQL 数据库产品名 / MySQL database product name */
	public static final String MYSQL_DB_NAME = "MySQL";

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 *
	 * @param databaseName 数据库名 / database name
	 * @param majorVersion 主版本 / major version
	 * @param minorVersion 次版本 / minor version
	 * @return 是否支持 / whether supported
	 */
	public static boolean supports(String db, int majorVersion, int minorVersion) {
		return MYSQL_DB_NAME.equals(db) && majorVersion >= 8;
	}
}
