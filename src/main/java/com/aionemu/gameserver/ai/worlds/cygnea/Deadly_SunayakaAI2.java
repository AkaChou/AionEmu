package com.aionemu.gameserver.ai.worlds.cygnea;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cygnea 区域 NPC AI：Deadly Sunayaka（@AIName "deadly_sunayaka"），继承 AggressiveNpcAI2。
 * Cygnea zone NPC AI: Deadly Sunayaka (@AIName "deadly_sunayaka"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("deadly_sunayaka")
public class Deadly_SunayakaAI2 extends AggressiveNpcAI2
{
	private AtomicBoolean isAggred = new AtomicBoolean(false);

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			simmeringRage();
		}
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		rageOfTheDragonLords();
	}

	private void simmeringRage() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 20651, 1, getOwner()).useNoAnimationSkill(); //Simmering Rage.
		getOwner().getEffectController().removeEffect(8763);
	}

	private void rageOfTheDragonLords() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 8763, 1, getOwner()).useNoAnimationSkill(); //Rage of the Dragon Lords
	}
}
