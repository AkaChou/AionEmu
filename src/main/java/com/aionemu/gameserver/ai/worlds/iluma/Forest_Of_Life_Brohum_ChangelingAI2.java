package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Iluma 区域 NPC AI：Forest Of Life Brohum Changeling（@AIName "forest_of_life_brohum_changeling"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Forest Of Life Brohum Changeling (@AIName "forest_of_life_brohum_changeling"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("forest_of_life_brohum_changeling")
public class Forest_Of_Life_Brohum_ChangelingAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242883, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Young Forest Of Life Brohum.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
