package com.aionemu.gameserver.ai.worlds.cygnea;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Cygnea 区域 NPC AI：Gatorback Skilex（@AIName "gatorback_skilex"），继承 AggressiveNpcAI2。
 * Cygnea zone NPC AI: Gatorback Skilex (@AIName "gatorback_skilex"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("gatorback_skilex")
public class Gatorback_SkilexAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(235830, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Mutated Gatorback Skilex.
		super.handleDied();
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
	}
}
