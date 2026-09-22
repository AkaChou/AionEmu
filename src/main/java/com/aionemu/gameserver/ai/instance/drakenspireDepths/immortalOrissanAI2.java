package com.aionemu.gameserver.ai.instance.drakenspireDepths;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

@AIName("immortal_orissan_quest")
// 237230

public class immortalOrissanAI2 extends AggressiveNpcAI2 {

	/** 虚脱的奥里萨（任务击杀目标）/ Exhausted Orissan (quest kill target) */
	private static final int EXHAUSTED_ORISSAN_NPC_ID = 237231;

	private final AtomicBoolean exhaustedOrissanSpawned = new AtomicBoolean(false);

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}

	/**
	 * 死亡兜底：一击/爆发致死时阈值检查不会触发，仍需生成任务击杀目标。
	 * Death fallback: a lethal blow skips the threshold check, so the quest kill target is spawned here.
	 */
	@Override
	protected void handleDied() {
		spawnExhaustedOrissanOnce();
		super.handleDied();
	}

	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 80 && spawnExhaustedOrissanOnce()) {
			AI2Actions.deleteOwner(this);
		}
	}

	private boolean spawnExhaustedOrissanOnce() {
		if (!exhaustedOrissanSpawned.compareAndSet(false, true)) {
			return false;
		}
		spawn(EXHAUSTED_ORISSAN_NPC_ID, getOwner().getX(), getOwner().getY(), getOwner().getZ(),
			getOwner().getHeading());
		return true;
	}
}
