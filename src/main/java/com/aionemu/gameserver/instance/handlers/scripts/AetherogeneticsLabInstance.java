package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;

@InstanceID(310050000)
public class AetherogeneticsLabInstance extends GeneralInstanceHandler {

	private static final int[] INSTANCE_KEYS = { 185000001, 185000002, 185000003, 185000004, 185000005 };

	@Override
	public void onPlayerLogOut(Player player) {
		removeKeys(player);
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeKeys(player);
	}

	private static void removeKeys(Player player) {
		Storage inventory = player.getInventory();
		for (int itemId : INSTANCE_KEYS) {
			inventory.decreaseByItemId(itemId, inventory.getItemCountByItemId(itemId));
		}
	}
}
