package com.aionemu.gameserver.ai.instance.bastionOfSouls;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * Bastion Of Souls 副本 NPC AI：Purification Jail（@AIName "Prison_Ice"），继承 NpcAI2。
 * Bastion Of Souls instance NPC AI: Purification Jail (@AIName "Prison_Ice"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Prison_Ice")
public class Purification_JailAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startLifeTask();
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Purification_JailAI2.this);
			}
		}, 900000);
	}
}
