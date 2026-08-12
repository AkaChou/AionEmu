package com.aionemu.gameserver.ai.worlds.reshanta.abyssLanding;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.model.gameobjects.player.Player;

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
		int skillId = getBlessingSkillId(getNpcId());
		if (skillId == 0) {
			return;
		}
		for (int otherSkillId = 22739; otherSkillId <= 22742; otherSkillId++) {
			if (otherSkillId != skillId) {
				effectController.removeEffect(otherSkillId);
			}
		}
		GameEngineServices.skillEngine().applyEffectDirectly(skillId, getOwner(), player, 0);
	}

	static int getBlessingSkillId(int npcId) {
		return switch (npcId) {
			case 883956, 883960 -> 22742; // Flight Energy.
			case 883957, 883961 -> 22741; // Life Energy.
			case 883958, 883962 -> 22740; // Battle Energy.
			case 883959, 883963 -> 22739; // Defense Energy.
			default -> 0;
		};
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
