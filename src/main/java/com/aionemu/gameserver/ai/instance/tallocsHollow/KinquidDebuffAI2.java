package com.aionemu.gameserver.ai.instance.tallocsHollow;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Tallocs Hollow 副本 NPC AI：Kinquid Debuff（@AIName "kinquid_debuff"），继承 AggressiveNpcAI2。
 * Tallocs Hollow instance NPC AI: Kinquid Debuff (@AIName "kinquid_debuff"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("kinquid_debuff")
public class KinquidDebuffAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleCreatureMoved(Creature creature) {
		super.handleCreatureMoved(creature);
		if (creature instanceof Npc && isInRange(creature, 10)) {
			Npc npc = (Npc) creature;
			if (npc.getNpcId() == 215467) { // 金基德 / Kindquid.
				GameEngineServices.skillEngine().getSkill(getOwner(), getNpcId() == 282008 ? 19235 : 19236, 46, getOwner()).useNoAnimationSkill();
			}
		}
	}
}
