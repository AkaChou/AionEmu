package com.aionemu.gameserver.ai.instance.admaStronghold;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Adma Stronghold 副本 NPC AI：Suspicious Boy（@AIName "suspicious_boy"），继承 AggressiveNpcAI2。
 * Adma Stronghold instance NPC AI: Suspicious Boy (@AIName "suspicious_boy"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("suspicious_boy")
public class Suspicious_BoyAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 50) {
			spawn(214701, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Suspicious Boy.
			AI2Actions.deleteOwner(this);
		}
	}
}
