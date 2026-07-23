package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.model.gameobjects.player.Player;

abstract class AbyssStoreroomInstance extends GeneralInstanceHandler {
	private final int keyStartId;

	protected AbyssStoreroomInstance(int keyStartId) {
		this.keyStartId = keyStartId;
	}

	@Override
	public void onPlayerLogOut(Player player) {
		for (int itemId = keyStartId; itemId <= keyStartId + 4; itemId++) {
			player.getInventory().decreaseByItemId(itemId, player.getInventory().getItemCountByItemId(itemId));
		}
	}
}
