package com.aionemu.gameserver.ai.instance.darkPoeta;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Dark Poeta 副本 NPC AI：Faithful Subordinate（@AIName "faithfulsubordinate"），继承 AggressiveNpcAI2。
 * Dark Poeta instance NPC AI: Faithful Subordinate (@AIName "faithfulsubordinate"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("faithfulsubordinate")
public class FaithfulSubordinateAI2 extends AggressiveNpcAI2
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
		GameThreadPoolServices.threadPoolManager().schedule(() -> AI2Actions.deleteOwner(FaithfulSubordinateAI2.this), 10000);
	}

	@Override
	public boolean isMoveSupported() {
		return false;
	}

	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
