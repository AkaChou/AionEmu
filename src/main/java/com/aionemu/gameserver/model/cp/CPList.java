package com.aionemu.gameserver.model.cp;

import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 创造点列表。
 * CP List interface.
 */

public interface CPList<T extends Creature> {

	boolean addPoint(T creature, int slot, int point);

	boolean removePoint(T creature, int slot);

	int size();
}
