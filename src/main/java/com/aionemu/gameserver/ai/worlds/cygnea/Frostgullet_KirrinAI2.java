package com.aionemu.gameserver.ai.worlds.cygnea;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Cygnea 区域 NPC AI：Frostgullet Kirrin（@AIName "frostgullet_kirrin"），继承 AggressiveNpcAI2。
 * Cygnea zone NPC AI: Frostgullet Kirrin (@AIName "frostgullet_kirrin"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("frostgullet_kirrin")
public class Frostgullet_KirrinAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(235918, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Frostshard Kirrin.
		super.handleDied();
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
	}
}
