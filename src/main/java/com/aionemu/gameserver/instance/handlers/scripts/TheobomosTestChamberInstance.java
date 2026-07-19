package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;

@InstanceID(301610000)
public class TheobomosTestChamberInstance extends GeneralInstanceHandler {

	@Override
	public void onDie(Npc npc) {
		if (npc.getNpcId() == 220426) {
			spawn(806221, 284.93094f, 119.47065f, 196.01285f, (byte) 1);
		}
	}
}
