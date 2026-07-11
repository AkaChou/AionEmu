package com.aionemu.gameserver.ai.worlds.norsvold;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Norsvold 区域 NPC AI：Razor Clawed Forest Cloke（@AIName "razor_clawed_forest_cloke"），继承 AggressiveNpcAI2。
 * Norsvold zone NPC AI: Razor Clawed Forest Cloke (@AIName "razor_clawed_forest_cloke"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("razor_clawed_forest_cloke")
public class Razor_Clawed_Forest_ClokeAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242283, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Pygmy Forest Cloke.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
