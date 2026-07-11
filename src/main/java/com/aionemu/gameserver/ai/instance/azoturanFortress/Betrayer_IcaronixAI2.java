package com.aionemu.gameserver.ai.instance.azoturanFortress;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;

/**
 * Azoturan Fortress 副本 NPC AI：Betrayer Icaronix（@AIName "betrayer_icaronix"），继承 AggressiveNpcAI2。
 * Azoturan Fortress instance NPC AI: Betrayer Icaronix (@AIName "betrayer_icaronix"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("betrayer_icaronix")
public class Betrayer_IcaronixAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkForSupport(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkForSupport(Creature creature) {
		for (VisibleObject object: getKnownList().getKnownObjectsSnapshot()) {
			if (object instanceof Npc && isInRange(object, 40)) {
				((Npc) object).getAi2().onCreatureEvent(AIEventType.CREATURE_AGGRO, creature);
			}
		}
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 75) {
			spawn(214599, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Icaronix The Betrayer.
			AI2Actions.deleteOwner(this);
		}
	}
}
