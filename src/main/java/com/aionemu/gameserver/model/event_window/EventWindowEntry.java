package com.aionemu.gameserver.model.event_window;

import java.sql.Timestamp;
import lombok.Getter;

/**
 * 活动窗口条目。
 * Event Window Entry model.
 *
 * @author Ranastic
 */
@Getter
public class EventWindowEntry {

	private int id;
	private Timestamp lastStamp;
	private int elapsed;

	public EventWindowEntry(int id, Timestamp lastStamp, int elapsed) {
		this.id = id;
		this.lastStamp = lastStamp;
		this.elapsed = elapsed;
	}

}
