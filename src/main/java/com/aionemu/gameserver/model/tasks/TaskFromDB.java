package com.aionemu.gameserver.model.tasks;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TaskFromDB that)) {
			return false;
		}
		return id == that.id
			&& delay == that.delay
			&& Objects.equals(name, that.name)
			&& Objects.equals(type, that.type)
			&& Objects.equals(lastActivation, that.lastActivation)
			&& Objects.equals(startTime, that.startTime)
			&& Arrays.equals(params, that.params);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(id, name, type, lastActivation, startTime, delay);
		return 31 * result + Arrays.hashCode(params);
	}

	@Override
	public String toString() {
		return "TaskFromDB[id=" + id + ", name=" + name + ", type=" + type + ", lastActivation=" + lastActivation
			+ ", startTime=" + startTime + ", delay=" + delay + ", params=" + Arrays.toString(params) + "]";
	}
}
