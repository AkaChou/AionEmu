package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Iluma 区域 NPC AI：Giant Razorback Frillneck（@AIName "giant_razorback_frillneck"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Giant Razorback Frillneck (@AIName "giant_razorback_frillneck"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("giant_razorback_frillneck")
public class Giant_Razorback_FrillneckAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242963, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 年幼 Razorback Frillneck / Razorback Frillneck Runt.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
