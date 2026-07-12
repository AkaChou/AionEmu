package com.aionemu.gameserver.model.tasks;

import java.sql.Timestamp;
import lombok.Getter;

/**
 * 数据库任务配置。
 * Database-backed task configuration.
 *
 * @author Divinity
 */
@Getter
public class TaskFromDB {

	private int id;
	private String name;
	private String type;
	private Timestamp lastActivation;
	private String startTime;
	private int delay;
	private String params[];

	/**
	 * 构造方法。 / Constructor.
	 */
	public TaskFromDB(int id, String name, String type, Timestamp lastActivation, String startTime, int delay,
			String param) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.lastActivation = lastActivation;
		this.startTime = startTime;
		this.delay = delay;

		if (param != null)
			this.params = param.split(" ");
		else
			this.params = new String[0];
	}

}
