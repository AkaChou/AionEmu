package com.aionemu.gameserver.ai.worlds.norsvold;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Norsvold 区域 NPC AI：Nightbloom Gargonops Shifter（@AIName "nightbloom_gargonops_shifter"），继承 AggressiveNpcAI2。
 * Norsvold zone NPC AI: Nightbloom Gargonops Shifter (@AIName "nightbloom_gargonops_shifter"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("nightbloom_gargonops_shifter")
public class Nightbloom_Gargonops_ShifterAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242503, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Nightbloom Baby Gargaonops.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
