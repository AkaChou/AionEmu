package com.aionemu.gameserver.ai.instance.drakenspireDepths;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Drakenspire Depths 副本 NPC AI：Fountless Heatvent Protector（@AIName "fountless_heatvent_protector"），继承 AggressiveNpcAI2。
 * Drakenspire Depths instance NPC AI: Fountless Heatvent Protector (@AIName "fountless_heatvent_protector"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("fountless_heatvent_protector")
public class Fountless_Heatvent_ProtectorAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 30) {
			spawn(236228, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Heatvent Protector.
			AI2Actions.deleteOwner(this);
			AI2Actions.scheduleRespawn(this);
		}
	}
}
