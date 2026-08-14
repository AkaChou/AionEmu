package com.aionemu.gameserver.ai.worlds.tiamaranta_eye;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Tiamaranta eye 区域 NPC AI：Aide Iranati（@AIName "Aide_Iranati"），继承 AggressiveNpcAI2。
 * Tiamaranta eye zone NPC AI: Aide Iranati (@AIName "Aide_Iranati"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Aide_Iranati")
public class Aide_IranatiAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 50) {
			spawn(218555, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 辅助官伊拉纳提 / Aide Iranati.
			AI2Actions.deleteOwner(this);
		}
	}
}
