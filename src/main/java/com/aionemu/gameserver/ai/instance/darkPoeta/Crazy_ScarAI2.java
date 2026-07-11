package com.aionemu.gameserver.ai.instance.darkPoeta;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Dark Poeta 副本 NPC AI：Crazy Scar（@AIName "crazy_scar"），继承 AggressiveNpcAI2。
 * Dark Poeta instance NPC AI: Crazy Scar (@AIName "crazy_scar"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("crazy_scar")
public class Crazy_ScarAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 75) {
			spawn(281116, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Crazy Scar.
			AI2Actions.deleteOwner(this);
		}
	}
}
