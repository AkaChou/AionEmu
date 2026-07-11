package com.aionemu.gameserver.ai.classAi;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * 职业技能召唤物/陷阱 AI：Trap Of Slowing（@AIName "trap_of_slowing"），继承 AggressiveNpcAI2。
 * Class-skill summon/trap AI: Trap Of Slowing (@AIName "trap_of_slowing"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("trap_of_slowing")
public class Trap_Of_SlowingAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				startLifeTask();
				AI2Actions.useSkill(Trap_Of_SlowingAI2.this, 18503);
			}
		}, 1000);
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Trap_Of_SlowingAI2.this);
			}
		}, 5000);
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
