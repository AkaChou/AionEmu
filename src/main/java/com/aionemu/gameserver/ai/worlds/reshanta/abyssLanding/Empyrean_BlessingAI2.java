package com.aionemu.gameserver.ai.worlds.reshanta.abyssLanding;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Reshanta 区域 NPC AI：Empyrean Blessing（@AIName "empyrean_blessing"），继承 ActionItemNpcAI2。
 * Reshanta zone NPC AI: Empyrean Blessing (@AIName "empyrean_blessing"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("empyrean_blessing")
public class Empyrean_BlessingAI2 extends ActionItemNpcAI2
{
	@Override
	protected void handleDialogStart(Player player) {
		super.handleDialogStart(player);
	}
	
	@Override
	protected void handleUseItemFinish(Player player) {
		PlayerEffectController effectController = player.getEffectController();
		switch (getNpcId()) {
		    case 883956: //Redemption Flight Energy.
			case 883960: //Harbinger Flight Energy.
                effectController.removeEffect(22739);
				effectController.removeEffect(22740);
				effectController.removeEffect(22741);
                GameEngineServices.skillEngine().getSkill(player, 22742, 1, player).useNoAnimationSkill();
            break;
			case 883957: //Redemption Life Energy.
			case 883961: //Harbinger Life Energy.
			    effectController.removeEffect(22739);
				effectController.removeEffect(22740);
				effectController.removeEffect(22742);
                GameEngineServices.skillEngine().getSkill(player, 22741, 1, player).useNoAnimationSkill();
            break;
			case 883958: //Redemption Battle Energy.
			case 883962: //Harbinger Battle Energy.
			    effectController.removeEffect(22739);
				effectController.removeEffect(22741);
				effectController.removeEffect(22742);
                GameEngineServices.skillEngine().getSkill(player, 22740, 1, player).useNoAnimationSkill();
            break;
			case 883959: //Redemption Defense Energy.
			case 883963: //Harbinger Defense Energy.
			    effectController.removeEffect(22740);
				effectController.removeEffect(22741);
				effectController.removeEffect(22742);
                GameEngineServices.skillEngine().getSkill(player, 22739, 1, player).useNoAnimationSkill();
            break;
		}
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
	
	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
