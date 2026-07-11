package com.aionemu.gameserver.ai.instance.drakenspireDepths;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Drakenspire Depths 副本 NPC AI：Tornado（@AIName "tornado"），继承 AggressiveNpcAI2。
 * Drakenspire Depths instance NPC AI: Tornado (@AIName "tornado"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("tornado")
public class TornadoAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				startLifeTask();
			}
		}, 1000);
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(TornadoAI2.this);
			}
		}, 20000);
	}
}
