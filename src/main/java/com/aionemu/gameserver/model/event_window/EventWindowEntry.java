package com.aionemu.gameserver.model.event_window;

import java.sql.Timestamp;

/**
 * 活动窗口条目。
 * Event Window Entry model.
 *
 * @author Ranastic
 */
public class EventWindowEntry {

	private int id;
	private Timestamp lastStamp;
	private int elapsed;

	public EventWindowEntry(int id, Timestamp lastStamp, int elapsed) {
		this.id = id;
		this.lastStamp = lastStamp;
		this.elapsed = elapsed;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回上次盖章 / Returns the last stamp*/
	public Timestamp getLastStamp() {
		return lastStamp;
	}

	/** 返回 elapsed / Returns the elapsed */
	public int getElapsed() {
		return elapsed;
	}
}
