package com.aionemu.gameserver.ai.worlds.norsvold;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Norsvold 区域 NPC AI：Skulking Forsaken Zaif（@AIName "skulking_forsaken_zaif"），继承 AggressiveNpcAI2。
 * Norsvold zone NPC AI: Skulking Forsaken Zaif (@AIName "skulking_forsaken_zaif"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("skulking_forsaken_zaif")
public class Skulking_Forsaken_ZaifAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		spawn(241963, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Forsaken Zaif Pup.
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
