package com.aionemu.gameserver.ai.instance.rentusBase;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Rentus Base 副本 NPC AI：Inhibitor Sikar（@AIName "inhibitorsikar"），继承 AggressiveNpcAI2。
 * Rentus Base instance NPC AI: Inhibitor Sikar (@AIName "inhibitorsikar"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("inhibitorsikar")
public class InhibitorSikarAI2 extends AggressiveNpcAI2
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
				GameEngineServices.skillEngine().getSkill(getOwner(), 19657, 1, getOwner()).useNoAnimationSkill(); //Dragon Breath.
				startLifeTask();
			}
		}, 1000);
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(InhibitorSikarAI2.this);
			}
		}, 15000);
	}
	
	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
