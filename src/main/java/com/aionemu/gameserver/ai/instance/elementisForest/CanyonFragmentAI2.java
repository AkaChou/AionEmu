package com.aionemu.gameserver.ai.instance.elementisForest;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import java.util.concurrent.Future;

/**
 * Elementis Forest 副本 NPC AI：Canyon Fragment（@AIName "canyonfragment"），继承 AggressiveNpcAI2。
 * Elementis Forest instance NPC AI: Canyon Fragment (@AIName "canyonfragment"), extends AggressiveNpcAI2.
 *
 * @author Luzien
 */
@AIName("canyonfragment")
public class CanyonFragmentAI2 extends AggressiveNpcAI2 {

	private Future<?> task;

	@Override
	public boolean canThink() {
		return false;
	}

	@Override
	public void handleSpawned() {
		super.handleSpawned();
		schedule();
	}

	@Override
	public void handleDied() {
		super.handleDied();
		if (!task.isDone()) {
			task.cancel(false);
		}
	}

	private void schedule() {
		task = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (!isAlreadyDead()) {
					spawn(282430, getPosition().getX(), getPosition().getY(), getPosition().getZ(), (byte) 0);
					AI2Actions.deleteOwner(CanyonFragmentAI2.this);
				}
			}
		}, 25000);
	}
}
