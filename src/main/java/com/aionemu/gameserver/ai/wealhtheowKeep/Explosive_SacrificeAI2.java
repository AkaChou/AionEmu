package com.aionemu.gameserver.ai.wealhtheowKeep;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * 维尔索要塞相关 NPC AI：Explosive Sacrifice（@AIName "explosive_sacrifice"），继承 AggressiveNpcAI2。
 * Wealhtheow Keep related NPC AI: Explosive Sacrifice (@AIName "explosive_sacrifice"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("explosive_sacrifice")
public class Explosive_SacrificeAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameEngineServices.skillEngine().getSkill(getOwner(), 21760, 46, getOwner()).useNoAnimationSkill(); //Explosion.
		startLifeTask();
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Explosive_SacrificeAI2.this);
			}
		}, 5000);
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
