package com.aionemu.gameserver.model.event_window;

import java.sql.Timestamp;

import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 活动窗口列表。
 * Event Window List interface.
 *
 * @author Ranastic
 */
public interface EventWindowList<T extends Creature> {

	boolean add(T creature, int id, Timestamp lastStamp, int elapsed);

	boolean remove(T creature, int id);

	int size();
}
