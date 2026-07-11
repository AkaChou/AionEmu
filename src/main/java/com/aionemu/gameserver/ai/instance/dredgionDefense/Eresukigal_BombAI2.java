package com.aionemu.gameserver.ai.instance.dredgionDefense;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Dredgion Defense 副本 NPC AI：Eresukigal Bomb（@AIName "Eresukigal_Bomb"），继承 AggressiveNpcAI2。
 * Dredgion Defense instance NPC AI: Eresukigal Bomb (@AIName "Eresukigal_Bomb"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Eresukigal_Bomb")
public class Eresukigal_BombAI2 extends AggressiveNpcAI2
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
				GameEngineServices.skillEngine().getSkill(getOwner(), 21709, 60, getOwner()).useNoAnimationSkill();
				startLifeTask();
			}
		}, 1000);
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Eresukigal_BombAI2.this);
				AI2Actions.scheduleRespawn(Eresukigal_BombAI2.this);
			}
		}, 10000);
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
