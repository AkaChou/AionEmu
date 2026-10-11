package com.aionemu.gameserver.model.tasks;

import java.sql.Timestamp;

/**
 * 数据库任务配置。
 * Database-backed task configuration.
 *
 * @author Divinity
 */
public record TaskFromDB(int id, String name, String type, Timestamp lastActivation, String startTime, int delay,
                         String[] params) {

	/**
	 * 以空格分隔的单字符串参数构造，param 为 null 时参数表为空。
	 * Constructs from a single space-separated parameter string; null yields an empty parameter array.
	 */
	public TaskFromDB(int id, String name, String type, Timestamp lastActivation, String startTime, int delay,
			String param) {
		this(id, name, type, lastActivation, startTime, delay, param != null ? param.split(" ") : new String[0]);
	}

}
