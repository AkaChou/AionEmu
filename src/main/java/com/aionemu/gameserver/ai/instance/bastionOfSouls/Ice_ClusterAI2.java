package com.aionemu.gameserver.ai.instance.bastionOfSouls;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Bastion Of Souls 副本 NPC AI：Ice Cluster（@AIName "IDAb1_Ere_Boss_Final_Ice"），继承 AggressiveNpcAI2。
 * Bastion Of Souls instance NPC AI: Ice Cluster (@AIName "IDAb1_Ere_Boss_Final_Ice"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("IDAb1_Ere_Boss_Final_Ice")
public class Ice_ClusterAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameEngineServices.skillEngine().getSkill(getOwner(), 21638, 60, getOwner()).useNoAnimationSkill();
		startLifeTask();
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Ice_ClusterAI2.this);
			}
		}, 2500);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
