package com.aionemu.gameserver.ai.instance.tallocsHollow;

import com.aionemu.gameserver.ai.RetailPatternAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.gameobjects.Creature;

import java.util.concurrent.Future;

/**
 * 真端烟雾 AI 的生命周期适配器，实际技能逻辑由 Elim_SmogEffect pattern 执行。
 * Retail smoke AI lifecycle adapter; the Elim_SmogEffect pattern performs the actual skills.
 *
 * @author Encom
 */
@AIName("Elim_SmogEffect")
public class KinquidDebuffAI2 extends RetailPatternAI2
{
	private static final long RETAIL_LIFE_TIME_MS = 20_000L;
	private Future<?> lifeTask;

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startLifeTask();
	}

	@Override
	protected void handleDespawned() {
		cancelLifeTask();
		super.handleDespawned();
	}

	@Override
	protected void handleDied() {
		cancelLifeTask();
		super.handleDied();
	}

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		despawnForRetailLife();
	}

	private void startLifeTask() {
		cancelLifeTask();
		lifeTask = GameThreadPoolServices.threadPoolManager().schedule(this::despawnForRetailLife,
			RETAIL_LIFE_TIME_MS);
	}

	private void cancelLifeTask() {
		if (lifeTask != null && !lifeTask.isDone()) {
			lifeTask.cancel(false);
		}
		lifeTask = null;
	}

	private void despawnForRetailLife() {
		if (isAlreadyDead() || !getPosition().isSpawned()) {
			return;
		}
		getOwner().getController().scheduleRespawn();
		getOwner().getController().onDelete();
	}
}
