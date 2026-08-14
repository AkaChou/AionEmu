package com.aionemu.gameserver.ai.worlds.tiamaranta_eye;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Tiamaranta eye 区域 NPC AI：Master At Arms Ranigan（@AIName "Master_At_Arms_Ranigan"），继承 AggressiveNpcAI2。
 * Tiamaranta eye zone NPC AI: Master At Arms Ranigan (@AIName "Master_At_Arms_Ranigan"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Master_At_Arms_Ranigan")
public class Master_At_Arms_RaniganAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 50) {
			spawn(218558, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 武器教官拉尼甘 / Master-At-Arms Ranigan.
			AI2Actions.deleteOwner(this);
		}
	}
}
