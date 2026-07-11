package com.aionemu.gameserver.model.templates.item.actions;

import com.aionemu.gameserver.model.gameobjects.HouseObject;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * 房屋 Dye 动作接口。
 * House Dye Action interface.
 */

public interface HouseDyeAction {
	boolean canAct(Player player, Item parentItem, HouseObject<?> targetHouseObject);

	void act(Player player, Item parentItem, HouseObject<?> targetHouseObject);
}
