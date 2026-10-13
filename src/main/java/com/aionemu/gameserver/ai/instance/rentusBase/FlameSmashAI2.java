package com.aionemu.gameserver.ai.instance.rentusBase;



import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.ThreadPoolManager;

/**
 * Rentus Base 副本 NPC AI：Flame Smash（@AIName "flame_smash"），继承 NpcAI2。
 * Rentus Base instance NPC AI: Flame Smash (@AIName "flame_smash"), extends NpcAI2.
 * @author Encom
 */
@AIName("flame_smash")
public class FlameSmashAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		starLifeTask();
		ThreadPoolManager.getInstance().schedule(() -> {
			if (!isAlreadyDead()) {
				SkillEngine.getInstance().getSkill(getOwner(), getNpcId() == 283008 ? 20540 : 20539, 60, getOwner()).useNoAnimationSkill();
			}
		}, 500);
	}

	private void starLifeTask() {
		ThreadPoolManager.getInstance().schedule(this::despawn, 7000);
	}

	private void despawn() {
		if (!isAlreadyDead()) {
			AI2Actions.deleteOwner(this);
		}
	}
}
