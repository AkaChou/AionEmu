package com.aionemu.gameserver.dao;

/**
 * MySQL 8 相关的 DAO 工具类，用于判断数据库是否支持 MySQL 8。
 * Utility helpers for MySQL 8 DAOs, used to detect MySQL 8 support.
 */
public class DAOUtils {

	/**
	 * MySQL 数据库名称常量。
	 * Constant for the MySQL database name.
	 */
	public static final String MYSQL_DB_NAME = "MySQL";

	/**
	 * 判断给定数据库与版本是否视为 MySQL 8 支持。
	 * Checks whether the given database and version are treated as MySQL 8 support.
	 *
	 * @param db 数据库产品名 / database product name
	 * major version
	 * minor version
	 *
	 * @return 若 supported 则为 true / true if supported
	 */
	public static boolean supports(String db, int majorVersion, int minorVersion) {
		return "MySQL".equals(db) && (minorVersion == 8 || majorVersion == 8);
	}
}
