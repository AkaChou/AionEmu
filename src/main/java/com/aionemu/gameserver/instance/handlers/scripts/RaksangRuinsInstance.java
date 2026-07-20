package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.gameobjects.Npc;

@InstanceID(300610000)
public class RaksangRuinsInstance extends GeneralInstanceHandler {

	@Override
	public void onDie(Npc npc) {
		if (npc.getNpcId() == 236306) {
			spawn(730445, 648.5508f, 700.05725f, 522.0487f, (byte) 80);
		}
	}
}
