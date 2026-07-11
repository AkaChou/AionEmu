package com.aionemu.gameserver.ai.beritraInvasion;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * 贝里特拉入侵相关 NPC AI：World Raid Directing Effect（@AIName "directing_effect"），继承 NpcAI2。
 * Beritra-invasion related NPC AI: World Raid Directing Effect (@AIName "directing_effect"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("directing_effect")
public class WorldRaid_Directing_EffectAI2 extends NpcAI2
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
				AI2Actions.deleteOwner(WorldRaid_Directing_EffectAI2.this);
			}
		}, 410000);
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
