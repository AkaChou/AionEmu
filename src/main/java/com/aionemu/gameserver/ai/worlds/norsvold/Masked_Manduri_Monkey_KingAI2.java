package com.aionemu.gameserver.ai.worlds.norsvold;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Norsvold 区域 NPC AI：Masked Manduri Monkey King（@AIName "masked_manduri_monkey_king"），继承 AggressiveNpcAI2。
 * Norsvold zone NPC AI: Masked Manduri Monkey King (@AIName "masked_manduri_monkey_king"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("masked_manduri_monkey_king")
public class Masked_Manduri_Monkey_KingAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(242187, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Masked Manduri Trickster.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
