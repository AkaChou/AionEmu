package com.aionemu.gameserver.ai.instance.bastionOfSouls;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Bastion Of Souls 副本 NPC AI：Hail Storm（@AIName "IDAb1_Ere_Boss_Final_HailStorm"），继承 AggressiveNpcAI2。
 * Bastion Of Souls instance NPC AI: Hail Storm (@AIName "IDAb1_Ere_Boss_Final_HailStorm"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("IDAb1_Ere_Boss_Final_HailStorm")
public class HailStormAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				GameEngineServices.skillEngine().getSkill(getOwner(), 17648, 60, getOwner()).useNoAnimationSkill(); //IDAb1_Ere_Boss_HailStorm.
				startLifeTask();
			}
		}, 1000);
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(HailStormAI2.this);
			}
		}, 10000);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
