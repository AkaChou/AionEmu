package com.aionemu.gameserver.ai.worlds.cygnea;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.handler.CreatureEventHandler;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Cygnea 区域 NPC AI：Lightbinder（@AIName "lightbinder"），继承 AggressiveNpcAI2。
 * Cygnea zone NPC AI: Lightbinder (@AIName "lightbinder"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("lightbinder")
public class LightbinderAI2 extends AggressiveNpcAI2
{
	@Override
    protected void handleCreatureMoved(Creature creature) {
        CreatureEventHandler.onCreatureSee(this, creature);
	if (creature instanceof Player) {
			final Player player = (Player) creature;
		if (!creature.getEffectController().hasAbnormalEffect(20664)) { //Conqueror's Passion.
				if (player.getCommonData().getRace() == Race.ELYOS) {
		        GameEngineServices.skillEngine().getSkill(getOwner(), 20664, 1, (Player) creature).useWithoutPropSkill(); //Conqueror's Passion.
				}
			}
	}
    }

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		switch (getNpcId()) {
			case 236028:
			case 236034:
			case 236041:
			case 236047:
			case 236054:
			case 236061:
			case 236067:
			case 236073:
				conquerorPassion();
			break;
		}
	}

	private void conquerorPassion() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 20665, 1, getOwner()).useNoAnimationSkill(); //Conqueror's Passion.
	}
}
