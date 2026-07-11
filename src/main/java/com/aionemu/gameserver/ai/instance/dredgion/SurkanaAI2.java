package com.aionemu.gameserver.ai.instance.dredgion;

import com.aionemu.gameserver.ai.OneDmgPerHitAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;

/**
 * Dredgion 副本 NPC AI：Surkana（@AIName "surkana"），继承 OneDmgPerHitAI2。
 * Dredgion instance NPC AI: Surkana (@AIName "surkana"), extends OneDmgPerHitAI2.
 *
 * @author Encom
 */
@AIName("surkana")
public class SurkanaAI2 extends OneDmgPerHitAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkForSupport(creature);
	}
	
	private void checkForSupport(Creature creature) {
		for (VisibleObject object : getKnownList().getKnownObjectsSnapshot()) {
			if (object instanceof Npc && isInRange(object, 20)) {
				((Npc) object).getAi2().onCreatureEvent(AIEventType.CREATURE_AGGRO, creature);
			}
		}
	}
}
