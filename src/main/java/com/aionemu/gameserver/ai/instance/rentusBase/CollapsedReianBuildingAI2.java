package com.aionemu.gameserver.ai.instance.rentusBase;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Rentus Base 副本 NPC AI：Collapsed Reian Building（@AIName "collapsed_reian_building"），继承 NpcAI2。
 * Rentus Base instance NPC AI: Collapsed Reian Building (@AIName "collapsed_reian_building"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("collapsed_reian_building")
public class CollapsedReianBuildingAI2 extends NpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		GameEngineServices.skillEngine().getSkill(getOwner(), 20088, 60, getOwner()).useNoAnimationSkill();
	}
}
