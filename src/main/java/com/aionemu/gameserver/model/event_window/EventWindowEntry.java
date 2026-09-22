package com.aionemu.gameserver.model.event_window;

import java.sql.Timestamp;
import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 活动窗口条目。
 * Event Window Entry model.
 * @author Ranastic
 */
@Getter
@AllArgsConstructor
public class EventWindowEntry {

	private final int id;
	private final Timestamp lastStamp;
	private final int elapsed;
}
