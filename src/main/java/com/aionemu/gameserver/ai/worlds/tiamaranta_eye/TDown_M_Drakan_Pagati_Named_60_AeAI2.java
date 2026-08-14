package com.aionemu.gameserver.ai.worlds.tiamaranta_eye;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Tiamaranta eye 区域 NPC AI：T Down M Drakan Pagati Named 60 Ae（@AIName "TDown_M_Drakan_Pagati_Named_60_Ae"），继承 AggressiveNpcAI2。
 * Tiamaranta eye zone NPC AI: T Down M Drakan Pagati Named 60 Ae (@AIName "TDown_M_Drakan_Pagati_Named_60_Ae"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("TDown_M_Drakan_Pagati_Named_60_Ae")
public class TDown_M_Drakan_Pagati_Named_60_AeAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 50) {
			spawn(249099, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // TDown_M_Drakan_Pagati_Named_60_Ae 精英 / TDown_M_Drakan_Pagati_Named_60_Ae.
			AI2Actions.deleteOwner(this);
		}
	}
}
