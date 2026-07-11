package com.aionemu.gameserver.ai.instance.admaStronghold;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Adma Stronghold 副本 NPC AI：Shape Change Zombie（@AIName "shape_change_zombie"），继承 AggressiveNpcAI2。
 * Adma Stronghold instance NPC AI: Shape Change Zombie (@AIName "shape_change_zombie"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("shape_change_zombie")
public class Shape_Change_ZombieAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startLifeTask();
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Shape_Change_ZombieAI2.this);
			}
		}, 10000);
	}
	
	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
