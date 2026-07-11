package com.aionemu.gameserver.ai.worlds.norsvold;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Norsvold 区域 NPC AI：Molting Honey Klaw（@AIName "molting_honey_klaw"），继承 AggressiveNpcAI2。
 * Norsvold zone NPC AI: Molting Honey Klaw (@AIName "molting_honey_klaw"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("molting_honey_klaw")
public class Molting_Honey_KlawAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(241843, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Sangor Scout.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
