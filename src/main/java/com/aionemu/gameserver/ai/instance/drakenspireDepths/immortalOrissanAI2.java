package com.aionemu.gameserver.ai.instance.drakenspireDepths;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

@AIName("immortal_orissan_quest")
// 237230

/**
 * Drakenspire Depths 副本 NPC AI：immortal Orissan（@AIName "immortal_orissan_quest"），继承 AggressiveNpcAI2。
 * Drakenspire Depths instance NPC AI: immortal Orissan (@AIName "immortal_orissan_quest"), extends AggressiveNpcAI2.
 *
 * @author Falke_34
 */
public class immortalOrissanAI2 extends AggressiveNpcAI2 {

	private AtomicBoolean isStartEvent = new AtomicBoolean(false);

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}

	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 80) {
			if (isStartEvent.compareAndSet(false, true)) {
				scheduleSpawnExhaustedOrissan(this.getPosition().getX(), this.getPosition().getY(), this.getPosition().getZ(), this.getPosition().getHeading());
				AI2Actions.deleteOwner(this);
			}
		}
	}

	private void scheduleSpawnExhaustedOrissan(final float x, final float y, final float z, final byte h) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			@Override
			public void run() {
				spawn(237231, x, y, z, h); // Exhausted Orissan
			}
		}, 1000);
	}
}
