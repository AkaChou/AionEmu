package com.aionemu.gameserver.ai.event;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 活动事件 NPC AI：Beritora（@AIName "beritora"），继承 AggressiveNpcAI2。
 * Event NPC AI: Beritora (@AIName "beritora"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("beritora")
public class BeritoraAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleDied() {
		final WorldPosition p = getPosition();
		if (p != null) {
			spawn(832262, p.getX(), p.getY(), p.getZ(), (byte) 0); //Treasure Chest.
		}
		super.handleDied();
	}
}
