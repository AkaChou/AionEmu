package com.aionemu.gameserver.ai.classAi;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * 职业技能召唤物/陷阱 AI：Explosive Trap（@AIName "explosive_trap"），继承 AggressiveNpcAI2。
 * Class-skill summon/trap AI: Explosive Trap (@AIName "explosive_trap"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("explosive_trap")
public class Explosive_TrapAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			startLifeTask();
			AI2Actions.useSkill(Explosive_TrapAI2.this, 18905);
		}, 1000);
	}

	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(() -> AI2Actions.deleteOwner(Explosive_TrapAI2.this), 5000);
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
