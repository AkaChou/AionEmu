package com.aionemu.gameserver.ai.instance.empyreanCrucible;


import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Empyrean Crucible 副本 NPC AI：Baby Consierd（@AIName "babyconsierd"），继承 AggressiveNpcAI2。
 * Empyrean Crucible instance NPC AI: Baby Consierd (@AIName "babyconsierd"), extends AggressiveNpcAI2.
 * @author Encom
 */
@AIName("babyconsierd")
public class BabyConsierdAI2 extends AggressiveNpcAI2
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
		ThreadPoolManager.getInstance().schedule(() -> AI2Actions.deleteOwner(BabyConsierdAI2.this), 10000);
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
