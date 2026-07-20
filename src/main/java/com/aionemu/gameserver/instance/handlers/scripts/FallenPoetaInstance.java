package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;

@InstanceID(301660000)
public class FallenPoetaInstance extends GeneralInstanceHandler {

	@Override
	public void onLeaveInstance(Player player) {
		cleanup(player);
	}

	@Override
	public void onPlayerLogOut(Player player) {
		cleanup(player);
	}

	private static void cleanup(Player player) {
		var inventory = player.getInventory();
		inventory.decreaseByItemId(164002346, inventory.getItemCountByItemId(164002346));
		player.getEffectController().removeEffect(21805);
		player.getEffectController().removeEffect(21806);
	}
}
