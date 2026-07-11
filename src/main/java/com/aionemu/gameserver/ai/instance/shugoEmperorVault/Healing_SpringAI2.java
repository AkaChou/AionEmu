package com.aionemu.gameserver.ai.instance.shugoEmperorVault;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.handler.CreatureEventHandler;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;

/**
 * Shugo Emperor Vault 副本 NPC AI：Healing Spring（@AIName "healing_spring"），继承 AggressiveNpcAI2。
 * Shugo Emperor Vault instance NPC AI: Healing Spring (@AIName "healing_spring"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("healing_spring")
public class Healing_SpringAI2 extends AggressiveNpcAI2
{
	@Override
    protected void handleCreatureMoved(Creature creature) {
        CreatureEventHandler.onCreatureSee(this, creature);
	if (creature instanceof Player) {
			final Player player = (Player) creature;
		if (!creature.getEffectController().hasAbnormalEffect(17560)) { //Bless Of Guardian Spring.
		    GameEngineServices.skillEngine().getSkill(getOwner(), 17560, 1, (Player) creature).useNoAnimationSkill(); //Bless Of Guardian Spring.
			}
	}
    }

	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
