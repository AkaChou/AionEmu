package com.aionemu.gameserver.ai.worlds.norsvold;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Norsvold 区域 NPC AI：Colossal Forest Flavia（@AIName "colossal_forest_flavia"），继承 AggressiveNpcAI2。
 * Norsvold zone NPC AI: Colossal Forest Flavia (@AIName "colossal_forest_flavia"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("colossal_forest_flavia")
public class Colossal_Forest_FlaviaAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242523, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Dwarf Forest Flavia.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
