package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Iluma 区域 NPC AI：Blood Fungus Skinwalker（@AIName "blood_fungus_skinwalker"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Blood Fungus Skinwalker (@AIName "blood_fungus_skinwalker"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("blood_fungus_skinwalker")
public class Blood_Fungus_SkinwalkerAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(243235, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Aether Fungus Rotron.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
