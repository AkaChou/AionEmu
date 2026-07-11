package com.aionemu.gameserver.ai.worlds.norsvold;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Norsvold 区域 NPC AI：Hugehorn Wendigo（@AIName "hugehorn_wendigo"），继承 AggressiveNpcAI2。
 * Norsvold zone NPC AI: Hugehorn Wendigo (@AIName "hugehorn_wendigo"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("hugehorn_wendigo")
public class Hugehorn_WendigoAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242103, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Hugehorn Runt.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
