package com.aionemu.gameserver.ai.instance.empyreanCrucible;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.services.NpcShoutsService;

/**
 * Empyrean Crucible 副本 NPC AI：Empyrean Administrator Arminos（@AIName "empadministratorarminos"），继承 NpcAI2。
 * Empyrean Crucible instance NPC AI: Empyrean Administrator Arminos (@AIName "empadministratorarminos"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("empadministratorarminos")
public class EmpyreanAdministratorArminosAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startEvent();
	}
	
	private void startEvent() {
		switch (getNpcId()) {
			case 217744: //Administrator Arminos. [3th Floor]
				sendMsg(1500247, getObjectId(), false, 8000);
				sendMsg(1500250, getObjectId(), false, 20000);
				sendMsg(1500251, getObjectId(), false, 60000);
			break;
			case 217749: //Administrator Arminos. [4th Floor]
				sendMsg(1500252, getObjectId(), false, 8000);
				sendMsg(1500253, getObjectId(), false, 16000);
				sendMsg(1400982, 0, false, 25000);
				sendMsg(1400988, 0, false, 27000);
				sendMsg(1400989, 0, false, 29000);
				sendMsg(1400990, 0, false, 31000);
				sendMsg(1401013, 0, false, 93000);
				sendMsg(1401014, 0, false, 113000);
				sendMsg(1401015, 0, false, 118000);
				sendMsg(1500255, getObjectId(), true, 118000);
			break;
		}
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
