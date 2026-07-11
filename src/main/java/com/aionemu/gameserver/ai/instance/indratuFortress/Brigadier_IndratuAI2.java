package com.aionemu.gameserver.ai.instance.indratuFortress;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;

/**
 * Indratu Fortress 副本 NPC AI：Brigadier Indratu（@AIName "brigadier_indratu"），继承 AggressiveNpcAI2。
 * Indratu Fortress instance NPC AI: Brigadier Indratu (@AIName "brigadier_indratu"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("brigadier_indratu")
public class Brigadier_IndratuAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkForSupport(creature);
	}
	
	private void checkForSupport(Creature creature) {
		for (VisibleObject object: getKnownList().getKnownObjectsSnapshot()) {
			if (object instanceof Npc && isInRange(object, 40)) {
				((Npc) object).getAi2().onCreatureEvent(AIEventType.CREATURE_AGGRO, creature);
			}
		}
	}
}
