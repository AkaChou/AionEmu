package com.aionemu.gameserver.model.dorinerk_wardrobe;

import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 衣橱列表。
 * Wardrobe List interface.
 *
 * @author Ranastic
 */
public interface WardrobeList<T extends Creature> {

	boolean addItem(T creature, int itemId, int slot, int reskin_count);

	boolean removeItem(T creature, int itemId);

	int size();
}
