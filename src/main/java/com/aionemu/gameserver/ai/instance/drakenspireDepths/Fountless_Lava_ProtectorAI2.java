package com.aionemu.gameserver.ai.instance.drakenspireDepths;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Drakenspire Depths 副本 NPC AI：Fountless Lava Protector（@AIName "fountless_lava_protector"），继承 AggressiveNpcAI2。
 * Drakenspire Depths instance NPC AI: Fountless Lava Protector (@AIName "fountless_lava_protector"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("fountless_lava_protector")
public class Fountless_Lava_ProtectorAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 30) {
			spawn(236227, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Lava Protector.
			AI2Actions.deleteOwner(this);
			AI2Actions.scheduleRespawn(this);
		}
	}
}
