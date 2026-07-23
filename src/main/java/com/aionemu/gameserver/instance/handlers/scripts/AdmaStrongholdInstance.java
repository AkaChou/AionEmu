package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;

@InstanceID(320130000)
public class AdmaStrongholdInstance extends GeneralInstanceHandler {

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 700396 -> player.getEffectController().removeEffect(18462);
			case 700397 -> player.getEffectController().removeEffect(18463);
		}
	}

	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		for (int itemId = 185000026; itemId <= 185000032; itemId++) {
			storage.decreaseByItemId(itemId, storage.getItemCountByItemId(itemId));
		}
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
	}
}
