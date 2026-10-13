package com.aionemu.gameserver.ai.instance.idgelDome;



import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

import java.util.concurrent.Future;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Idgel Dome 副本 NPC AI：Idgel Dome Fire（@AIName "fire"），继承 AggressiveNpcAI2。
 * Idgel Dome instance NPC AI: Idgel Dome Fire (@AIName "fire"), extends AggressiveNpcAI2.
 * @author Encom
 */
@AIName("fire")
public class IdgelDomeFireAI2 extends AggressiveNpcAI2
{
	private Future<?> eventTask;

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startEventTask();
		startLifeTask();
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

	private void startLifeTask() {
		ThreadPoolManager.getInstance().schedule(() -> AI2Actions.deleteOwner(IdgelDomeFireAI2.this), 30000);
	}

	private void startEventTask() {
		eventTask = ThreadPoolManager.getInstance().scheduleAtFixedRate(() -> {
			if (isAlreadyDead()) {
				cancelEventTask();
			} else {
				SkillEngine.getInstance().getSkill(getOwner(), 20070, 1, getOwner()).useNoAnimationSkill();
			}
		}, 1000, 1000);
	}
}
