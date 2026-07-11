package com.aionemu.gameserver.ai.worlds.norsvold;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Norsvold 区域 NPC AI：Bighorn Wendigo（@AIName "bighorn_wendigo"），继承 AggressiveNpcAI2。
 * Norsvold zone NPC AI: Bighorn Wendigo (@AIName "bighorn_wendigo"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("bighorn_wendigo")
public class Bighorn_WendigoAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(241991, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Bighorn Denmaster.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
