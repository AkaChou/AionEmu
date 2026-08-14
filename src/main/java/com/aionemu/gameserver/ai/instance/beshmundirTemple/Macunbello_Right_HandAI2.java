package com.aionemu.gameserver.ai.instance.beshmundirTemple;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.commons.network.util.ThreadPoolManager;

/**
 * Beshmundir Temple 副本 NPC AI：Macunbello Right Hand（@AIName "MacunbelloRightHand"），继承 AggressiveNpcAI2。
 * Beshmundir Temple instance NPC AI: Macunbello Right Hand (@AIName "MacunbelloRightHand"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("MacunbelloRightHand")
public class Macunbello_Right_HandAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startLifeTask();
		this.setStateIfNot(AIState.FOLLOWING);
	}
	
	@Override
	protected void handleMoveArrived() {
		AI2Actions.targetCreature(Macunbello_Right_HandAI2.this, getPosition().getWorldMapInstance().getNpc(216245)); // Boss：Macunbello / Macunbello.
		AI2Actions.useSkill(Macunbello_Right_HandAI2.this, 19049); // 吞噬灵魂 / Devour Soul.
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Macunbello_Right_HandAI2.this);
			}
		}, 33000);
	}
}
