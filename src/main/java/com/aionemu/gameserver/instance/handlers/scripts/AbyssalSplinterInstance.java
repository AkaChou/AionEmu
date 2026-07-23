package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;

@InstanceID(300220000)
public class AbyssalSplinterInstance extends GeneralInstanceHandler {

	@Override
	public void onLeaveInstance(Player player) {
		removeFragment(player);
	}

	private static void removeFragment(Player player) {
		player.getInventory().decreaseByItemId(185000104, player.getInventory().getItemCountByItemId(185000104));
	}
}
