package com.aionemu.gameserver.ai.instance.kromedesTrial;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.handler.CreatureEventHandler;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Kromedes Trial 副本 NPC AI：Altar Of Healing（@AIName "Altar_Of_Healing"），继承 AggressiveNpcAI2。
 * Kromedes Trial instance NPC AI: Altar Of Healing (@AIName "Altar_Of_Healing"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Altar_Of_Healing")
public class Altar_Of_HealingAI2 extends AggressiveNpcAI2
{
	@Override
    protected void handleCreatureMoved(Creature creature) {
        CreatureEventHandler.onCreatureSee(this, creature);
	if (creature instanceof Player) {
			final Player player = (Player) creature;
		if (!creature.getEffectController().hasAbnormalEffect(17560)) { // 守护之泉的祝福 / Bless Of Guardian Spring.
		    GameEngineServices.skillEngine().getSkill(getOwner(), 17560, 1, (Player) creature).useWithoutPropSkill(); // 守护之泉的祝福 / Bless Of Guardian Spring.
			}
	}
    }
}
