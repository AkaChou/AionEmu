package com.aionemu.gameserver.model.skill.linked_skill;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * Stigma 列表。
 * Stigma List interface.
 *
 * @author DrNism
 */
public interface StigmaList<T extends Creature> {

	boolean addItem(T creature, int itemId, String itemName);

	boolean remove(Player player, int itemId);

	boolean isItemPresent(int itemId);

	int size();

}
