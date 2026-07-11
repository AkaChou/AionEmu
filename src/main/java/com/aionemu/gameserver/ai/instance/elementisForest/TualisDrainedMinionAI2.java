package com.aionemu.gameserver.ai.instance.elementisForest;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import java.util.concurrent.Future;

/**
 * Elementis Forest 副本 NPC AI：Tualis Drained Minion（@AIName "tualis_drained_minion"），继承 AggressiveNpcAI2。
 * Elementis Forest instance NPC AI: Tualis Drained Minion (@AIName "tualis_drained_minion"), extends AggressiveNpcAI2.
 *
 * @author xTz
 */
@AIName("tualis_drained_minion")
public class TualisDrainedMinionAI2 extends AggressiveNpcAI2 {

	private Future<?> lifeTask;

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startLifeTask();
	}

	private void startLifeTask() {
		lifeTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				if (!isAlreadyDead()) {
					AI2Actions.deleteOwner(TualisDrainedMinionAI2.this);
				}
			}
		}, 30000);
	}

	private void cancelTask() {
		if (lifeTask != null && !lifeTask.isDone()) {
			lifeTask.cancel(true);
		}
	}

	@Override
	protected void handleDied() {
		cancelTask();
		super.handleDied();
	}

	@Override
	protected void handleDespawned() {
		cancelTask();
		super.handleDespawned();
	}
}
