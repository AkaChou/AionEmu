package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;

@InstanceID(301520000)
public class DrakenspireDepthsQInstance extends GeneralInstanceHandler {

	@Override
	public void onLeaveInstance(Player player) {
		cleanup(player);
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeEffects(player);
	}

	private static void cleanup(Player player) {
		var inventory = player.getInventory();
		inventory.decreaseByItemId(185000219, inventory.getItemCountByItemId(185000219));
		removeEffects(player);
	}

	private static void removeEffects(Player player) {
		player.getEffectController().removeEffect(22778);
		player.getEffectController().removeEffect(22779);
	}
}
