package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Iluma 区域 NPC AI：Valley Torr Crumbler（@AIName "valley_torr_crumbler"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Valley Torr Crumbler (@AIName "valley_torr_crumbler"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("valley_torr_crumbler")
public class Valley_Torr_CrumblerAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242603, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Tiny Valley Torr.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
