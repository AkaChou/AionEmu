package com.aionemu.gameserver.ai.worlds.norsvold;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Norsvold 区域 NPC AI：Masquerading Desert Gehkros（@AIName "masquerading_desert_gehkros"），继承 AggressiveNpcAI2。
 * Norsvold zone NPC AI: Masquerading Desert Gehkros (@AIName "masquerading_desert_gehkros"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("masquerading_desert_gehkros")
public class Masquerading_Desert_GehkrosAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(241983, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Desert Gihlos Hatchling.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
