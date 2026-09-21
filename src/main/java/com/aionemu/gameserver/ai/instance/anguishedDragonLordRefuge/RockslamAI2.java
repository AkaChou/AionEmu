package com.aionemu.gameserver.ai.instance.anguishedDragonLordRefuge;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Anguished Dragon Lord Refuge 副本 NPC AI：Rockslam（@AIName "rockslam"），继承 AggressiveNpcAI2。
 * Anguished Dragon Lord Refuge instance NPC AI: Rockslam (@AIName "rockslam"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("anguished_dragon_lord_refuge_rockslam")
public class RockslamAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			GameEngineServices.skillEngine().getSkill(getOwner(), 20969, 60, getOwner()).useNoAnimationSkill();
			startLifeTask();
		}, 1000);
	}

	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(() -> AI2Actions.deleteOwner(RockslamAI2.this), 10000);
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
