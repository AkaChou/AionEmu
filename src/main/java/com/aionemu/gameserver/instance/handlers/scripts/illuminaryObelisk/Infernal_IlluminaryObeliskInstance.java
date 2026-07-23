package com.aionemu.gameserver.instance.handlers.scripts.illuminaryObelisk;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.services.teleport.TeleportService2;

@InstanceID(301370000)
public class Infernal_IlluminaryObeliskInstance extends GeneralInstanceHandler {
	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
	}

	@Override
	public void onExitInstance(Player player) {
		removeItems(player);
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}

	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(164000289, storage.getItemCountByItemId(164000289));
		storage.decreaseByItemId(164000290, storage.getItemCountByItemId(164000290));
	}
}
