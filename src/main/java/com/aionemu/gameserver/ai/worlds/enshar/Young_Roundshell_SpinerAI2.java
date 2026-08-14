package com.aionemu.gameserver.ai.worlds.enshar;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Enshar 区域 NPC AI：Young Roundshell Spiner（@AIName "young_roundshell_spiner"），继承 AggressiveNpcAI2。
 * Enshar zone NPC AI: Young Roundshell Spiner (@AIName "young_roundshell_spiner"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("young_roundshell_spiner")
public class Young_Roundshell_SpinerAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(219787, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 生成狂暴的暗海刺虫 / Frenzied Darksea Spiner.
		super.handleDied();
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
	}
}
