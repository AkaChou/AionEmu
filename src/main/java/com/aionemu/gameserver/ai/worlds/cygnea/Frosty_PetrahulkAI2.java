package com.aionemu.gameserver.ai.worlds.cygnea;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Cygnea 区域 NPC AI：Frosty Petrahulk（@AIName "frosty_petrahulk"），继承 AggressiveNpcAI2。
 * Cygnea zone NPC AI: Frosty Petrahulk (@AIName "frosty_petrahulk"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("frosty_petrahulk")
public class Frosty_PetrahulkAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(235916, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Freezing Petrahulk.
		super.handleDied();
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
	}
}
