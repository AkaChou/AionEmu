package com.aionemu.gameserver.ai.instance.drakenspireDepths;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.concurrent.Future;

/**
 * Drakenspire Depths 副本 NPC AI：Exploding Flame（@AIName "exploding_flame"），继承 AggressiveNpcAI2。
 * Drakenspire Depths instance NPC AI: Exploding Flame (@AIName "exploding_flame"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("exploding_flame")
public class Exploding_FlameAI2 extends AggressiveNpcAI2
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
					GameEngineServices.skillEngine().getSkill(getOwner(), 21516, 46, getOwner()).useNoAnimationSkill();
				}
			}
		}, 3000, 8000);
	}
}
