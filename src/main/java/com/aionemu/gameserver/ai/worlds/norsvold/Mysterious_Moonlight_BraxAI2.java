package com.aionemu.gameserver.ai.worlds.norsvold;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Norsvold 区域 NPC AI：Mysterious Moonlight Brax（@AIName "mysterious_moonlight_brax"），继承 AggressiveNpcAI2。
 * Norsvold zone NPC AI: Mysterious Moonlight Brax (@AIName "mysterious_moonlight_brax"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("mysterious_moonlight_brax")
public class Mysterious_Moonlight_BraxAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242343, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Miniature Moonlight Brax.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
