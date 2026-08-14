package com.aionemu.gameserver.ai.instance.transidiumAnnex;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Transidium Annex 副本 NPC AI：Hangar Barricade（@AIName "hangar_barricade"），继承 NpcAI2。
 * Transidium Annex instance NPC AI: Hangar Barricade (@AIName "hangar_barricade"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("hangar_barricade")
public class Hangar_BarricadeAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameEngineServices.skillEngine().getSkill(getOwner(), 21515, 1, getOwner()).useNoAnimationSkill(); // 不屈奥德 / Unyielding Aether
		GameEngineServices.skillEngine().getSkill(getOwner(), 22783, 1, getOwner()).useNoAnimationSkill();
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
