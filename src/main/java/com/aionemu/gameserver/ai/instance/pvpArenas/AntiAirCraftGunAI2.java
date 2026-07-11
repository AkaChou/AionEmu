package com.aionemu.gameserver.ai.instance.pvpArenas;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Pvp Arenas 副本 NPC AI：Anti Air Craft Gun（@AIName "antiaircraftgun"），继承 ActionItemNpcAI2。
 * Pvp Arenas instance NPC AI: Anti Air Craft Gun (@AIName "antiaircraftgun"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("antiaircraftgun")
public class AntiAirCraftGunAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		InstanceReward<?> instance = getPosition().getWorldMapInstance().getInstanceHandler().getInstanceReward();
		if (instance != null && !instance.isStartProgress()) {
			return;
		}
		super.handleDialogStart(player);
	}
	
	@Override
	protected void handleUseItemFinish(Player player) {
		Npc owner = getOwner();
		player.getController().stopProtectionActiveTask();
		int morphSkill = 0;
		switch (getNpcId()) {
			case 701185:
			case 701321:
				morphSkill = 0x4E502E;
			break;
			case 701322:
				morphSkill = 0x4E5133;
			break;
			case 701213:
			case 701323:
				morphSkill = 0x4E5238;
			break;
		}
		GameEngineServices.skillEngine().getSkill(getOwner(), morphSkill >> 8, morphSkill & 0xFF, player).useNoAnimationSkill();
		AI2Actions.scheduleRespawn(this);
		AI2Actions.deleteOwner(this);
	}
}
