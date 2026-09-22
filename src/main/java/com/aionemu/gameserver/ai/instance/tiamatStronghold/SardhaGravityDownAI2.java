package com.aionemu.gameserver.ai.instance.tiamatStronghold;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Tiamat Stronghold 副本 NPC AI：Sardha Gravity Down（@AIName "sardhagravitydown"），继承 AggressiveNpcAI2。
 * Tiamat Stronghold instance NPC AI: Sardha Gravity Down (@AIName "sardhagravitydown"), extends AggressiveNpcAI2.
 * @author Encom
 */
@AIName("sardhagravitydown")
public class SardhaGravityDownAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			GameEngineServices.skillEngine().getSkill(getOwner(), 20737, 1, getOwner()).useNoAnimationSkill(); //Operate Gravity Control Device.
			startLifeTask();
		}, 1000);
	}

	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(() -> AI2Actions.deleteOwner(SardhaGravityDownAI2.this), 15000);
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
