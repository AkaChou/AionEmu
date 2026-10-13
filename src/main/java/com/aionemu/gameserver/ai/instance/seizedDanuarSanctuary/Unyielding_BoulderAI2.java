package com.aionemu.gameserver.ai.instance.seizedDanuarSanctuary;


import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Seized Danuar Sanctuary 副本 NPC AI：Unyielding Boulder（@AIName "unyielding_boulder"），继承 NpcAI2。
 * Seized Danuar Sanctuary instance NPC AI: Unyielding Boulder (@AIName "unyielding_boulder"), extends NpcAI2.
 * @author Encom
 */
@AIName("unyielding_boulder")
public class Unyielding_BoulderAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		SkillEngine.getInstance().getSkill(getOwner(), 22776, 1, getOwner()).useNoAnimationSkill();
		SkillEngine.getInstance().getSkill(getOwner(), 22783, 1, getOwner()).useNoAnimationSkill();
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
