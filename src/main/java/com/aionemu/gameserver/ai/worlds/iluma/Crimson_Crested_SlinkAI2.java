package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Iluma 区域 NPC AI：Crimson Crested Slink（@AIName "crimson_crested_slink"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Crimson Crested Slink (@AIName "crimson_crested_slink"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("crimson_crested_slink")
public class Crimson_Crested_SlinkAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(243263, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 小型 Crimson Crested Slink / Small Crimson Crested Slink.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
