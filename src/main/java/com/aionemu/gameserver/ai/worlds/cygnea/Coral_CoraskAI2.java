package com.aionemu.gameserver.ai.worlds.cygnea;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Cygnea 区域 NPC AI：Coral Corask（@AIName "coral_corask"），继承 AggressiveNpcAI2。
 * Cygnea zone NPC AI: Coral Corask (@AIName "coral_corask"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("coral_corask")
public class Coral_CoraskAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(235832, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Aggressive Coral Corask.
		super.handleDied();
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
	}
}
