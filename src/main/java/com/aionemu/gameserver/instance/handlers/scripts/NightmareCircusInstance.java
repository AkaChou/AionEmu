package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;

@InstanceID(301200000)
public class NightmareCircusInstance extends GeneralInstanceHandler {

	@Override
	public void onPlayerLogOut(Player player) {
		removeEffects(player);
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeEffects(player);
	}

	private void removeEffects(Player player) {
		player.getEffectController().removeEffect(21469);
		player.getEffectController().removeEffect(21470);
		player.getEffectController().removeEffect(21471);
		player.getEffectController().removeEffect(21472);
	}
}
