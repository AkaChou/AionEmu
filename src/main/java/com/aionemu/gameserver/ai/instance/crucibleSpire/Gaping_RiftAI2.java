package com.aionemu.gameserver.ai.instance.crucibleSpire;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Crucible Spire 副本 NPC AI：Gaping Rift（@AIName "tower_mine"），继承 AggressiveNpcAI2。
 * Crucible Spire instance NPC AI: Gaping Rift (@AIName "tower_mine"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("tower_mine")
public class Gaping_RiftAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleCreatureAggro(Creature creature) {
		AI2Actions.useSkill(this, 18058);
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Gaping_RiftAI2.this);
			}
		}, 10000);
	}
}
