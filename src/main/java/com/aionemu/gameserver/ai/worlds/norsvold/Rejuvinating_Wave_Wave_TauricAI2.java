package com.aionemu.gameserver.ai.worlds.norsvold;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Norsvold 区域 NPC AI：Rejuvinating Wave Wave Tauric（@AIName "rejuvinating_wave_wave_tauric"），继承 AggressiveNpcAI2。
 * Norsvold zone NPC AI: Rejuvinating Wave Wave Tauric (@AIName "rejuvinating_wave_wave_tauric"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("rejuvinating_wave_wave_tauric")
public class Rejuvinating_Wave_Wave_TauricAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(241863, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Youngling Wave Tauric.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
