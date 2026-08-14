package com.aionemu.gameserver.ai.instance.cradleOfEternity;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Cradle Of Eternity 副本 NPC AI：Insane Jotun Warrior（@AIName "Insane_Jotun_Warrior"），继承 AggressiveNpcAI2。
 * Cradle Of Eternity instance NPC AI: Insane Jotun Warrior (@AIName "Insane_Jotun_Warrior"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Insane_Jotun_Warrior")
public class Insane_Jotun_WarriorAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(220492, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); // 狂暴约腾战士 / Furious Jotun Warrior.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
