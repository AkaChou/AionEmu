package com.aionemu.gameserver.ai.worlds.idianDepths;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;

/**
 * Idian Depths 区域 NPC AI：Blackened Grave（@AIName "blackened_grave"），继承 NpcAI2。
 * Idian Depths zone NPC AI: Blackened Grave (@AIName "blackened_grave"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("blackened_grave")
public class Blackened_GraveAI2 extends NpcAI2
{
	@Override
	protected void handleDied() {
		spawn(284262, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading()); //Executioner Penemon.
		super.handleDied();
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
	}
}
