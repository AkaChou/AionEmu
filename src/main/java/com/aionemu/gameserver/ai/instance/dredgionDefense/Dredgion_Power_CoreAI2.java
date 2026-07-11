package com.aionemu.gameserver.ai.instance.dredgionDefense;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Dredgion Defense 副本 NPC AI：Dredgion Power Core（@AIName "Dredgion_Power_Core"），继承 NpcAI2。
 * Dredgion Defense instance NPC AI: Dredgion Power Core (@AIName "Dredgion_Power_Core"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Dredgion_Power_Core")
public class Dredgion_Power_CoreAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameEngineServices.skillEngine().getSkill(getOwner(), 18298, 60, getOwner()).useNoAnimationSkill(); //Dredgion’s Power Core Barrier.
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
