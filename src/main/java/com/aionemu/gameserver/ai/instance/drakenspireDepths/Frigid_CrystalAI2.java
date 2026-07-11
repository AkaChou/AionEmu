package com.aionemu.gameserver.ai.instance.drakenspireDepths;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Drakenspire Depths 副本 NPC AI：Frigid Crystal（@AIName "frigid_crystal"），继承 AggressiveNpcAI2。
 * Drakenspire Depths instance NPC AI: Frigid Crystal (@AIName "frigid_crystal"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("frigid_crystal")
public class Frigid_CrystalAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameEngineServices.skillEngine().getSkill(getOwner(), 21638, 46, getOwner()).useNoAnimationSkill(); //Frozen Blur.
		startLifeTask();
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Frigid_CrystalAI2.this);
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
