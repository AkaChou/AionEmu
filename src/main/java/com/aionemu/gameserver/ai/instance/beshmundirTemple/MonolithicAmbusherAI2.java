package com.aionemu.gameserver.ai.instance.beshmundirTemple;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;

/**
 * Beshmundir Temple 副本 NPC AI：Monolithic Ambusher（@AIName "monolithicambusher"），继承 AggressiveNpcAI2。
 * Beshmundir Temple instance NPC AI: Monolithic Ambusher (@AIName "monolithicambusher"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("monolithicambusher")
public class MonolithicAmbusherAI2 extends AggressiveNpcAI2
{
	private boolean hasHelped;
	
	@Override
	protected void handleBackHome() {
		super.handleBackHome();
		hasHelped = false;
	}
	
	@Override
	protected void handleCreatureAggro(Creature creature) {
		super.handleCreatureAggro(creature);
		if (!hasHelped) {
			hasHelped = true;
			help(creature);
		}
	}
	
	private void help(Creature creature) {
		for (VisibleObject object : getKnownList().getKnownObjectsSnapshot()) {
			if (object instanceof Npc && isInRange(object, 60)) {
				Npc npc = (Npc) object;
				if (!npc.getLifeStats().isAlreadyDead() && npc.getNpcId() == 216215 && (int) npc.getSpawn().getY() == (int) getSpawnTemplate().getY()) {
					npc.getAi2().onCreatureEvent(AIEventType.CREATURE_AGGRO, creature);
				}
			}
		}
	}
}
