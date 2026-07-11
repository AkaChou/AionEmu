package com.aionemu.gameserver.ai.instance.dragonLordRefuge;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Dragon Lord Refuge 副本 NPC AI：Gravity Tornado（@AIName "gravity_tornado"），继承 AggressiveNpcAI2。
 * Dragon Lord Refuge instance NPC AI: Gravity Tornado (@AIName "gravity_tornado"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("gravity_tornado")
public class GravityTornadoAI2 extends AggressiveNpcAI2
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
				GameEngineServices.skillEngine().getSkill(getOwner(), 20966, 60, getOwner()).useNoAnimationSkill();
				startLifeTask();
			}
		}, 1000);
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(GravityTornadoAI2.this);
			}
		}, 10000);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
