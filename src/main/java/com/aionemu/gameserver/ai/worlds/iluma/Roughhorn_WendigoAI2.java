package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Iluma 区域 NPC AI：Roughhorn Wendigo（@AIName "roughhorn_wendigo"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Roughhorn Wendigo (@AIName "roughhorn_wendigo"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("roughhorn_wendigo")
public class Roughhorn_WendigoAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(243043, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Roughhorn Runt.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
