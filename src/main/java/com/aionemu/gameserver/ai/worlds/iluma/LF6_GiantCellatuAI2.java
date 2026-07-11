package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * Iluma 区域 NPC AI：LF6 Giant Cellatu（@AIName "LF6_GiantCellatu"），继承 NpcAI2。
 * Iluma zone NPC AI: LF6 Giant Cellatu (@AIName "LF6_GiantCellatu"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("LF6_GiantCellatu")
public class LF6_GiantCellatuAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startLifeTask();
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(LF6_GiantCellatuAI2.this);
			}
		}, 10000);
	}
}
