package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;

@InstanceID(301390000)
public class DrakenspireDepthsInstance extends GeneralInstanceHandler {

	@Override
	public void onExitInstance(Player player) {
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}
}
