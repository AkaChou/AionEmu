package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Iluma 区域 NPC AI：Plateau Gihla Chameleon（@AIName "plateau_gihla_chameleon"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Plateau Gihla Chameleon (@AIName "plateau_gihla_chameleon"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("plateau_gihla_chameleon")
public class Plateau_Gihla_ChameleonAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(243063, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 年幼 Plateau Gihlos / Plateau Gihlos Runt.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
