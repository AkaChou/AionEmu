package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Iluma 区域 NPC AI：Ruthless Wave Tauric（@AIName "ruthless_wave_tauric"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Ruthless Wave Tauric (@AIName "ruthless_wave_tauric"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("ruthless_wave_tauric")
public class Ruthless_Wave_TauricAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242723, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 新手 Tauric Stoneheart / Tyro Tauric Stoneheart.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
