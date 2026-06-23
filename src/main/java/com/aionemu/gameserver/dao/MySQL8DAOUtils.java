package com.aionemu.gameserver.dao;

public class MySQL8DAOUtils {

	public static final String MYSQL_DB_NAME = "MySQL";

	public static boolean supports(String db, int majorVersion, int minorVersion) {
		return "MySQL".equals(db) && (minorVersion == 8 || majorVersion == 8);
	}
}