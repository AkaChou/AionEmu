package com.aionemu.gameserver.ai.instance.cradleOfEternity;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Cradle Of Eternity 副本 NPC AI：Fiery Typhon（@AIName "Fiery_Typhon"），继承 AggressiveNpcAI2。
 * Cradle Of Eternity instance NPC AI: Fiery Typhon (@AIName "Fiery_Typhon"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Fiery_Typhon")
public class Fiery_TyphonAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameEngineServices.skillEngine().getSkill(getOwner(), 23035, 60, getOwner()).useNoAnimationSkill(); // 提芬的污染物 / Typhons Pollutant.
		startLifeTask();
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Fiery_TyphonAI2.this);
			}
		}, 10000);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
