package com.aionemu.gameserver.ai.instance.anguishedDragonLordRefuge;


import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Anguished Dragon Lord Refuge 副本 NPC AI：Divisive Creation（@AIName "divisive_creation"），继承 AggressiveNpcAI2。
 * Anguished Dragon Lord Refuge instance NPC AI: Divisive Creation (@AIName "divisive_creation"), extends AggressiveNpcAI2.
 * @author Encom
 */
@AIName("anguished_dragon_lord_refuge_divisive_creation")
public class DivisiveCreationAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		ThreadPoolManager.getInstance().schedule(this::startLifeTask, 1000);
	}

	private void startLifeTask() {
		ThreadPoolManager.getInstance().schedule(() -> AI2Actions.deleteOwner(DivisiveCreationAI2.this), 20000);
	}

	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
