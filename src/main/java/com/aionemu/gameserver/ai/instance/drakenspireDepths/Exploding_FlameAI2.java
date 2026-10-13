package com.aionemu.gameserver.ai.instance.drakenspireDepths;



import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;

import java.util.concurrent.Future;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Drakenspire Depths 副本 NPC AI：Exploding Flame（@AIName "exploding_flame"），继承 AggressiveNpcAI2。
 * Drakenspire Depths instance NPC AI: Exploding Flame (@AIName "exploding_flame"), extends AggressiveNpcAI2.
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
		eventTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(() -> {
			if (isAlreadyDead()) {
				cancelEventTask();
			} else {
				SkillEngine.getInstance().getSkill(getOwner(), 21516, 46, getOwner()).useNoAnimationSkill();
			}
		}, 3000, 8000);
	}
}
