package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Iluma 区域 NPC AI：Hidden Swamp Bufo（@AIName "hidden_swamp_bufo"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Hidden Swamp Bufo (@AIName "hidden_swamp_bufo"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("hidden_swamp_bufo")
public class Hidden_Swamp_BufoAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242683, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Baby Swamp Bufo.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
