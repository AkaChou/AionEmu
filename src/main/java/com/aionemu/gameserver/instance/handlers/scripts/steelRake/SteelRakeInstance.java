package com.aionemu.gameserver.instance.handlers.scripts.steelRake;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;

@InstanceID(300100000)
public class SteelRakeInstance extends GeneralInstanceHandler {

	@Override
	public void onDie(Npc npc) {
		if (npc.getNpcId() == 214968) {
			RetailConditionSpawnEngine.setVariable(instance, "IDSHULACKSHIP_PH_KILL", 1, 0);
		}
	}

	@Override
	public void onExitInstance(Player player) {
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}
}
