package com.aionemu.gameserver.ai.siege;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * 攻城战相关 NPC AI：Bomber（@AIName "Bomber"），继承 AggressiveNpcAI2。
 * Siege-related NPC AI: Bomber (@AIName "Bomber"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Bomber")
public class BomberAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleCreatureAggro(Creature creature) {
		AI2Actions.useSkill(this, 21866); //Wide Area Explosion.
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(BomberAI2.this);
			}
		}, 1000);
	}
}
