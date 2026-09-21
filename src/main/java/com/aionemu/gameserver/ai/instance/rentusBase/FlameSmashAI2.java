package com.aionemu.gameserver.ai.instance.rentusBase;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * Rentus Base 副本 NPC AI：Flame Smash（@AIName "flame_smash"），继承 NpcAI2。
 * Rentus Base instance NPC AI: Flame Smash (@AIName "flame_smash"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("flame_smash")
public class FlameSmashAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		starLifeTask();
		GameThreadPoolServices.threadPoolManager().schedule(() -> {
			if (!isAlreadyDead()) {
				GameEngineServices.skillEngine().getSkill(getOwner(), getNpcId() == 283008 ? 20540 : 20539, 60, getOwner()).useNoAnimationSkill();
			}
		}, 500);
	}

	private void starLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(() -> despawn(), 7000);
	}

	private void despawn() {
		if (!isAlreadyDead()) {
			AI2Actions.deleteOwner(this);
		}
	}
}
