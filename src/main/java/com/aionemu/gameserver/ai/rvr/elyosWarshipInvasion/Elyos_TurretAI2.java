package com.aionemu.gameserver.ai.rvr.elyosWarshipInvasion;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * RvR 相关 NPC AI：Elyos Turret（@AIName "LF6_Event_G1_SWHowitzer"），继承 ActionItemNpcAI2。
 * RvR-related NPC AI: Elyos Turret (@AIName "LF6_Event_G1_SWHowitzer"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("LF6_Event_G1_SWHowitzer")
public class Elyos_TurretAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		super.handleDialogStart(player);
	}
	
	@Override
	protected void handleUseItemFinish(Player player) {
		GameEngineServices.skillEngine().getSkill(player, 21517, 1, player).useNoAnimationSkill();
		AI2Actions.deleteOwner(this);
	}
}
