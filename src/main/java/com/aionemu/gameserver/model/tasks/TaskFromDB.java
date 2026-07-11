package com.aionemu.gameserver.model.tasks;

import java.sql.Timestamp;

/**
 * 任务 FromDB，用于 tasks 相关逻辑。
 * Task From DB for tasks logic.
 *
 * @author Divinity
 */
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

	/**
	 * @return Task's id @return int。 / Task's id @return int
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return Task's name @return String。 / Task's name @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Task's type : - FIXED_IN_TIME (HH:MM:SS) @return String。 / Task's type : - FIXED_IN_TIME (HH:MM:SS) @return String
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return 任务上次激活时间。@return Timestamp / Task's last activation @return Timestamp
	 */
	public Timestamp getLastActivation() {
		return lastActivation;
	}

	/**
	 * @return 任务开始时间（HH:MM:SS）。@return String / Task's starting time (HH:MM:SS format) @return String
	 */
	public String getStartTime() {
		return startTime;
	}

	/**
	 * @return Task's delay @return int。 / Task's delay @return int
	 */
	public int getDelay() {
		return delay;
	}

	/**
	 * @return Task's param(s) @return String[]。 / Task's param(s) @return String[]
	 */
	public String[] getParams() {
		return params;
	}
}
