package com.aionemu.gameserver.ai.worlds.theobomos;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * Theobomos 区域 NPC AI：Flame Eternal（@AIName "flame_eternal"），继承 NpcAI2。
 * Theobomos zone NPC AI: Flame Eternal (@AIName "flame_eternal"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("flame_eternal")
public class FlameEternalAI2 extends NpcAI2
{
	@Override
	protected void handleDied() {
		final WorldPosition p = getPosition();
		if (p != null) {
			spawn(214552, p.getX(), p.getY(), p.getZ(), (byte) 0); //Burnt Zombies.
		}
		super.handleDied();
		AI2Actions.deleteOwner(this);
		AI2Actions.scheduleRespawn(this);
	}
}
