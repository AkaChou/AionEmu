package com.aionemu.gameserver.ai.worlds.heiron;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.ai.GeneralNpcAI2;

/**
 * Heiron 区域 NPC AI：warrior monument（@AIName "warrior_monument"），继承 GeneralNpcAI2。
 * Heiron zone NPC AI: warrior monument (@AIName "warrior_monument"), extends GeneralNpcAI2.
 *
 * @author cheatkiller
 */
@AIName("warrior_monument")
public class warrior_monumentAI2 extends GeneralNpcAI2 {

	@Override
	public boolean canThink() {
		return false;
	}

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		Npc npc = getOwner().getPosition().getWorldMapInstance().getNpc(216239);
		if (npc == null) {
			if (Rnd.get(0, 100) < 10) {
				spawn(216239, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0);
				AI2Actions.dieSilently(this, creature);
			}
		}
	}

	@Override
	public int modifyDamage(int damage) {
		return 1;
	}

	@Override
	protected void handleDied() {
		super.handleDied();
		AI2Actions.deleteOwner(this);
	}
}
