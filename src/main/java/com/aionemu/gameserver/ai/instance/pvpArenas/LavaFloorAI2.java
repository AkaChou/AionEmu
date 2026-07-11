package com.aionemu.gameserver.ai.instance.pvpArenas;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.concurrent.Future;

/**
 * Pvp Arenas 副本 NPC AI：Lava Floor（@AIName "lava_floor"），继承 AggressiveNpcAI2。
 * Pvp Arenas instance NPC AI: Lava Floor (@AIName "lava_floor"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("lava_floor")
public class LavaFloorAI2 extends AggressiveNpcAI2
{
	private Future<?> eventTask;
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startEventTask();
	}
	
	@Override
	protected void handleDied() {
		cancelEventTask();
		super.handleDied();
	}
	
	@Override
	protected void handleDespawned() {
		cancelEventTask();
		super.handleDespawned();
	}
	
	private void cancelEventTask() {
		if (eventTask != null &&
		   !eventTask.isDone()) {
			eventTask.cancel(true);
		}
	}
	
	private void startEventTask() {
		eventTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (isAlreadyDead()) {
					cancelEventTask();
				} else {
					GameEngineServices.skillEngine().getSkill(getOwner(), 20069, 1, getOwner()).useNoAnimationSkill();
				}
			}
		}, 1000, 1000);
	}
}
