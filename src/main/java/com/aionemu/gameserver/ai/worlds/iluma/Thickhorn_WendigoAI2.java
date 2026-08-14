package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Iluma 区域 NPC AI：Thickhorn Wendigo（@AIName "thickhorn_wendigo"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Thickhorn Wendigo (@AIName "thickhorn_wendigo"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("thickhorn_wendigo")
public class Thickhorn_WendigoAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242903, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 年幼 Thickhorn / Thickhorn Runt.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
