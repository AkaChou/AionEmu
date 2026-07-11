package com.aionemu.gameserver.ai.event.conquestOffering;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Conquest Offering 活动 NPC AI：Conquest Npc Buff（@AIName "conquest_npc_buff"），继承 ActionItemNpcAI2。
 * Conquest Offering event NPC AI: Conquest Npc Buff (@AIName "conquest_npc_buff"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("conquest_npc_buff")
public class Conquest_Npc_BuffAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		super.handleDialogStart(player);
	}
	
	@Override
	protected void handleUseItemFinish(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		switch (getNpcId()) {
		    case 856175: //Pawrunerk.
			    effectController.removeEffect(21925);
				effectController.removeEffect(21926);
				effectController.removeEffect(21927);
		        GameEngineServices.skillEngine().getSkill(player, 21924, 1, player).useNoAnimationSkill(); //Boost Attack Power.
		    break;
			case 856176: //Chitrunerk.
			    effectController.removeEffect(21924);
				effectController.removeEffect(21926);
				effectController.removeEffect(21927);
			    GameEngineServices.skillEngine().getSkill(player, 21925, 1, player).useNoAnimationSkill(); //Movement Speed Increase.
			break;
			case 856177: //Rapirunerk.
			    effectController.removeEffect(21924);
				effectController.removeEffect(21925);
				effectController.removeEffect(21927);
			    GameEngineServices.skillEngine().getSkill(player, 21926, 1, player).useNoAnimationSkill(); //Attack Speed/Casting Speed Increase.
			break;
			case 856178: //Dandrunerk.
			    effectController.removeEffect(21924);
				effectController.removeEffect(21925);
				effectController.removeEffect(21926);
			    GameEngineServices.skillEngine().getSkill(player, 21927, 1, player).useNoAnimationSkill(); //Boost Defense.
			break;
		}
		AI2Actions.deleteOwner(this);
	}
}
