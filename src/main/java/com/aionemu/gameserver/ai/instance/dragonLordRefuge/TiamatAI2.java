package com.aionemu.gameserver.ai.instance.dragonLordRefuge;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Dragon Lord Refuge 副本 NPC AI：Tiamat（@AIName "tiamat"），继承 AggressiveNpcAI2。
 * Dragon Lord Refuge instance NPC AI: Tiamat (@AIName "tiamat"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("tiamat")
public class TiamatAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameEngineServices.skillEngine().getSkill(getOwner(), 20975, 1, getOwner()).useNoAnimationSkill(); //Fissure Incarnate.
		GameEngineServices.skillEngine().getSkill(getOwner(), 20976, 1, getOwner()).useNoAnimationSkill(); //Wrath Incarnate.
		GameEngineServices.skillEngine().getSkill(getOwner(), 20977, 1, getOwner()).useNoAnimationSkill(); //Gravity Incarnate.
		GameEngineServices.skillEngine().getSkill(getOwner(), 20978, 1, getOwner()).useNoAnimationSkill(); //Petrification Incarnate.
		GameEngineServices.skillEngine().getSkill(getOwner(), 20984, 1, getOwner()).useNoAnimationSkill(); //Unbreakable Wing.
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
