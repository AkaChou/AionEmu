package com.aionemu.gameserver.ai.worlds.beluslan;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Beluslan 区域 NPC AI：Mine Mage（@AIName "mine_mage"），继承 AggressiveNpcAI2。
 * Beluslan zone NPC AI: Mine Mage (@AIName "mine_mage"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("mine_mage")
public class Mine_MageAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		switch (Rnd.get(1, 2)) {
			case 1:
			    spawnArchmageMegran();
			break;
			case 2:
			break;
		}
		super.handleDied();
	}
	
	private void spawnArchmageMegran() {
		spawn(213716, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Archmage Megran.
	}
}
