package com.aionemu.gameserver.ai.worlds.heiron;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.ai.GeneralNpcAI2;

/**
 * Heiron 区域 NPC AI：Klawspawn（@AIName "klawspawn"），继承 GeneralNpcAI2。
 * Heiron zone NPC AI: Klawspawn (@AIName "klawspawn"), extends GeneralNpcAI2.
 *
 * @author cheatkiller
 */
@AIName("klawspawn")
public class KlawspawnAI2 extends GeneralNpcAI2 {

	@Override
	public boolean canThink() {
		return false;
	}

	/**
	 * 受击处理：当对应 NPC 不存在且 10% 概率触发时，生成 NPC 并静默死亡。
	 * Attack handler: spawns the NPC with a 10% chance when absent, then dies silently.
	 *
	 * @param creature 攻击者 / attacker
	 */
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		Npc npc = getOwner().getPosition().getWorldMapInstance().getNpc(212120);
		if (npc == null) {
			if (Rnd.get(0, 100) < 10) {
				spawn(212120, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) 0);
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
