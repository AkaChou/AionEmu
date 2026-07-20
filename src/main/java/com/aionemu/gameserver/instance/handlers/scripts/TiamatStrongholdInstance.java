package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;

@InstanceID(300510000)
public class TiamatStrongholdInstance extends GeneralInstanceHandler {

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		if (npc.getNpcId() == 701523) {
			setDoorState(22, true);
		}
	}
}
