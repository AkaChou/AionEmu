package com.aionemu.gameserver.ai.worlds.tiamaranta_eye;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.model.gameobjects.Creature;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tiamaranta eye 区域 NPC AI：Limb Render（@AIName "Limb_Render"），继承 NpcAI2。
 * Tiamaranta eye zone NPC AI: Limb Render (@AIName "Limb_Render"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Limb_Render")
public class Limb_RenderAI2 extends NpcAI2
{
	int attackCount;
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
    public void handleAttack(Creature creature) {
		if (isAggred.compareAndSet(false, true)) {
			// 肢体撕裂者遭受攻击。击败攻击水晶的玩家。 / A Limb Render is under attack. Defeat the player attacking the crystal.
			GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1401462);
		}
        attackCount++;
        if (attackCount == 195) {
            attackCount = 0;
			AI2Actions.useSkill(this, 20655); // 水晶碎片 / Crystal Frgament.
			// 肢体撕裂者已爆炸。 / A Limb Render has exploded.
            GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1401463);
        }
		super.handleAttack(creature);
    }
	
	@Override
	protected void handleSpawned() {
  		switch (getNpcId()) {
			// 肢体撕裂者。 / Limb Render.
			case 283072:
			case 858016:
			    // 肢体撕裂者已出现。摧毁时会爆炸并对附近造成严重伤害。 / A Limb Render has appeared. It explodes when destroyed, inflicting serious damage to those nearby.
				GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1401461);
			break;
		}
		super.handleSpawned();
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	@Override
	public int modifyOwnerDamage(int damage) {
		return 1;
	}
	
	@Override
	public int modifyDamage(int damage) {
		return 1;
	}
}
