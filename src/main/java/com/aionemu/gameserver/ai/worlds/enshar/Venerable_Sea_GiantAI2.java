package com.aionemu.gameserver.ai.worlds.enshar;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Enshar 区域 NPC AI：Venerable Sea Giant（@AIName "venerable_sea_giant"），继承 AggressiveNpcAI2。
 * Enshar zone NPC AI: Venerable Sea Giant (@AIName "venerable_sea_giant"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("venerable_sea_giant")
public class Venerable_Sea_GiantAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(219788, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 生成远古海巨人 / Primeval Sea Giant.
		super.handleDied();
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
	}
}
