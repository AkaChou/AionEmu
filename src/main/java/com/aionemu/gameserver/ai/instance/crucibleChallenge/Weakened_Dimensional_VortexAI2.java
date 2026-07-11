package com.aionemu.gameserver.ai.instance.crucibleChallenge;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Crucible Challenge 副本 NPC AI：Weakened Dimensional Vortex（@AIName "weakened_dimensional_vortex"），继承 AggressiveNpcAI2。
 * Crucible Challenge instance NPC AI: Weakened Dimensional Vortex (@AIName "weakened_dimensional_vortex"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("weakened_dimensional_vortex")
public class Weakened_Dimensional_VortexAI2 extends AggressiveNpcAI2
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
				GameEngineServices.skillEngine().getSkill(getOwner(), 19570, 46, getOwner()).useNoAnimationSkill(); //Dimensional Vortex.
				startLifeTask();
			}
		}, 1000);
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Weakened_Dimensional_VortexAI2.this);
			}
		}, 15000);
	}
	
	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
