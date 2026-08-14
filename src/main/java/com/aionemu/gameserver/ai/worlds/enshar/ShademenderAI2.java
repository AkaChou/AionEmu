package com.aionemu.gameserver.ai.worlds.enshar;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.handler.CreatureEventHandler;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Enshar 区域 NPC AI：Shademender（@AIName "shademender"），继承 AggressiveNpcAI2。
 * Enshar zone NPC AI: Shademender (@AIName "shademender"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("shademender")
public class ShademenderAI2 extends AggressiveNpcAI2
{
	/**
	 * 生物移动检测：对接近的魔族玩家施放征服者之热情增益（若尚未存在）。
	 * Creature-move handler: applies Conqueror's Passion to approaching Asmodian players if not already active.
	 *
	 * @param creature 移动的生物 / moving creature
	 */
	@Override
    protected void handleCreatureMoved(Creature creature) {
        CreatureEventHandler.onCreatureSee(this, creature);
	if (creature instanceof Player) {
			final Player player = (Player) creature;
		if (!creature.getEffectController().hasAbnormalEffect(20664)) { // 征服者之热情 / Conqueror's Passion.
				if (player.getCommonData().getRace() == Race.ASMODIANS) {
		        GameEngineServices.skillEngine().getSkill(getOwner(), 20664, 1, (Player) creature).useNoAnimationSkill(); // 征服者之热情 / Conqueror's Passion.
				}
			}
	}
    }

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		switch (getNpcId()) {
			case 219880:
			case 219886:
			case 219893:
			case 219899:
			case 219905:
			case 219912:
			case 219919:
			case 219925:
				conquerorPassion();
			break;
		}
	}

	private void conquerorPassion() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 20665, 1, getOwner()).useNoAnimationSkill(); // 征服者之热情 / Conqueror's Passion.
	}
}
