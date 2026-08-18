package com.aionemu.gameserver.ai.instance.empyreanCrucible;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Empyrean Crucible 副本 NPC AI：Rideable Antiaircraft Gun（@AIName "rideable_antiaircraft_gun"），继承 ActionItemNpcAI2。
 * Empyrean Crucible instance NPC AI: Rideable Antiaircraft Gun (@AIName "rideable_antiaircraft_gun"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("rideable_antiaircraft_gun")
public class Rideable_Antiaircraft_GunAI2 extends ActionItemNpcAI2
{
	private AtomicBoolean canUse = new AtomicBoolean(true);
	
	@Override
	protected void handleDialogStart(Player player) {
		super.handleDialogStart(player);
	}
	
	@Override
	protected void handleUseItemFinish(Player player) {
		if (canUse.compareAndSet(true, false)) {
			int morphSkill = getMorphSkill();
			GameEngineServices.skillEngine().getSkill(getOwner(), morphSkill >> 8, morphSkill & 0xFF, player).useWithoutPropSkill();
			AI2Actions.deleteOwner(this);
		}
	}
	
	private int getMorphSkill() {
		switch (getNpcId()) {
			case 701199: // 骑乘型对空炮 / Rideable Antiaircraft Gun
				return 0x4E5133;
		}
		return 0;
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
