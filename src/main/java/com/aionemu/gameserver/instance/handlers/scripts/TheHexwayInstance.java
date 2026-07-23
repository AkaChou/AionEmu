package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;

@InstanceID(300700000)
public class TheHexwayInstance extends GeneralInstanceHandler {
	@Override
	public void onDie(Npc npc) {
		if (npc.getNpcId() == 219617) {
			npc.getController().onDelete();
		}
	}
}
