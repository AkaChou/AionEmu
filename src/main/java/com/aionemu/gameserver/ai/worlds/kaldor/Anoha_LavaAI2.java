package com.aionemu.gameserver.ai.worlds.kaldor;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Kaldor 区域 NPC AI：Anoha Lava（@AIName "anoha_lava"），继承 AggressiveNpcAI2。
 * Kaldor zone NPC AI: Anoha Lava (@AIName "anoha_lava"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("anoha_lava")
public class Anoha_LavaAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameEngineServices.skillEngine().getSkill(getOwner(), 21767, 46, getOwner()).useNoAnimationSkill(); //Infernal Flame Explosion.
		startLifeTask();
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Anoha_LavaAI2.this);
			}
		}, 4000);
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
