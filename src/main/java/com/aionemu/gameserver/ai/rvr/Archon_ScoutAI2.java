package com.aionemu.gameserver.ai.rvr;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.services.NpcShoutsService;

/**
 * RvR 相关 NPC AI：Archon Scout（@AIName "archon_scout"），继承 AggressiveNpcAI2。
 * RvR-related NPC AI: Archon Scout (@AIName "archon_scout"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("archon_scout")
public class Archon_ScoutAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		switch (getNpcId()) {
			case 240887: //Archon Warrior.
			case 240888: //Archon Mage.
			case 240889: //Archon Scout.
			case 240890: //Archon Marksman.
				startLifeTask();
			break;
        }
		// 卑鄙的家伙！！你们的抵抗终将徒劳。 / Wretches!! Your resistance shall be futile.
		sendMsg(1501534, getObjectId(), false, 5000);
		// 让我们向这些懦弱的天族展示魔族的力量！ / Let's show these cowardly Elyos the might of the Asmodians!
		sendMsg(1501535, getObjectId(), false, 8000);
		// 不要放弃！主神阿兹菲尔的意志与我们同在。 / Don't give up! The will of Empyrean Lord Azphel is with us.
		sendMsg(1501536, getObjectId(), false, 11000);
		// 主神阿兹菲尔！请赐予我力量。 / Empyrean Lord Azphel! Please give me strength.
		sendMsg(1501540, getObjectId(), false, 14000);
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Archon_ScoutAI2.this);
			}
		}, 300000); //5 分钟。 / 5 Minutes.
	}
	
	private void sendMsg(int msg, int Obj, boolean isShout, int time) {
		GameFeatureServices.npcShoutsService().sendMsg(getPosition().getWorldMapInstance(), msg, Obj, isShout, 0, time);
	}
}
