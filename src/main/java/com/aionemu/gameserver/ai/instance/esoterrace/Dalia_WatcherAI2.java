package com.aionemu.gameserver.ai.instance.esoterrace;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Esoterrace 副本 NPC AI：Dalia Watcher（@AIName "daliawatcher"），继承 AggressiveNpcAI2。
 * Esoterrace instance NPC AI: Dalia Watcher (@AIName "daliawatcher"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("daliawatcher")
public class Dalia_WatcherAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(() -> startLifeTask(), 1000);
	}

	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(() -> AI2Actions.deleteOwner(Dalia_WatcherAI2.this), 20000);
	}

	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
