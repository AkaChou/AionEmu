package com.aionemu.gameserver.model.atreian_bestiary;

import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * AB 列表。
 * AB List interface.
 *
 * @author Ranastic
 */

public interface ABList<T extends Creature> {
	boolean add(T creature, int id, int killCount, int level, int claimReward);

	boolean remove(T creature, int id);

	int size();
}
