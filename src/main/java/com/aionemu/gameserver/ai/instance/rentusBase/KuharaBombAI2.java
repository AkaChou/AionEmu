package com.aionemu.gameserver.ai.instance.rentusBase;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.model.actions.NpcActions;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Rentus Base 副本 NPC AI：Kuhara Bomb（@AIName "kuhara_bomb"），继承 AggressiveNpcAI2。
 * Rentus Base instance NPC AI: Kuhara Bomb (@AIName "kuhara_bomb"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("kuhara_bomb")
public class KuharaBombAI2 extends AggressiveNpcAI2
{
	private Npc kuharaTheVolatile1;
	private Npc kuharaTheVolatile2;
	private AtomicBoolean isDestroyed = new AtomicBoolean(false);
	
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		this.setStateIfNot(AIState.FOLLOWING);
		kuharaTheVolatile1 = getPosition().getWorldMapInstance().getNpc(217311); //Kuhara The Volatile.
		kuharaTheVolatile2 = getPosition().getWorldMapInstance().getNpc(236298); //Kuhara The Volatile.
	}
	
	@Override
	protected void handleMoveArrived() {
		if (isDestroyed.compareAndSet(false, true)) {
			if (kuharaTheVolatile1 != null && !NpcActions.isAlreadyDead(kuharaTheVolatile1)) {
				GameEngineServices.skillEngine().getSkill(getOwner(), 19659, 60, kuharaTheVolatile1).useNoAnimationSkill();  //Bomb Explosion.
			} else if (kuharaTheVolatile2 != null && !NpcActions.isAlreadyDead(kuharaTheVolatile2)) {
				GameEngineServices.skillEngine().getSkill(getOwner(), 19659, 60, kuharaTheVolatile2).useNoAnimationSkill();  //Bomb Explosion.
			}
		}
	}
}
