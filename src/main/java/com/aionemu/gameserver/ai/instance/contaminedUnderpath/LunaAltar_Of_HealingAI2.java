package com.aionemu.gameserver.ai.instance.contaminedUnderpath;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.handler.CreatureEventHandler;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Contamined Underpath 副本 NPC AI：Luna Altar Of Healing（@AIName "LunaAltar_Of_Healing"），继承 AggressiveNpcAI2。
 * Contamined Underpath instance NPC AI: Luna Altar Of Healing (@AIName "LunaAltar_Of_Healing"), extends AggressiveNpcAI2.
 *
 * @author Rinzler (Encom)
 */
@AIName("LunaAltar_Of_Healing")
public class LunaAltar_Of_HealingAI2 extends AggressiveNpcAI2
{
	@Override
    protected void handleCreatureMoved(Creature creature) {
        CreatureEventHandler.onCreatureSee(this, creature);
	if (creature instanceof Player) {
			final Player player = (Player) creature;
		if (!creature.getEffectController().hasAbnormalEffect(17560)) { // 对玩家使用技能 Bless of Guardian Spring / Use skill Bless of Guardian Spring on the player
		    GameEngineServices.skillEngine().getSkill(getOwner(), 17560, 1, (Player) creature).useNoAnimationSkill(); // 使用技能 Bless of Guardian Spring / Use skill Bless of Guardian Spring
			}
	}
    }
}
