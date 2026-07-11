package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Iluma 区域 NPC AI：Devious Manduri Beacon（@AIName "devious_manduri_beacon"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Devious Manduri Beacon (@AIName "devious_manduri_beacon"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("devious_manduri_beacon")
public class Devious_Manduri_BeaconAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242923, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Baby Manduri Beacon.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
