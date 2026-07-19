package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;

@InstanceID(300150000)
public class UdasTempleInstance extends GeneralInstanceHandler {

	@Override
	public void onLeaveInstance(Player player) {
		removeKeys(player);
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeKeys(player);
	}

	private static void removeKeys(Player player) {
		var inventory = player.getInventory();
		inventory.decreaseByItemId(185000083, inventory.getItemCountByItemId(185000083));
		inventory.decreaseByItemId(185000084, inventory.getItemCountByItemId(185000084));
		inventory.decreaseByItemId(185000085, inventory.getItemCountByItemId(185000085));
	}
}
