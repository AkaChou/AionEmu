package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Iluma 区域 NPC AI：Auronos（@AIName "auronos"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Auronos (@AIName "auronos"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("auronos")
public class AuronosAI2 extends AggressiveNpcAI2
{
	private AtomicBoolean isAggred = new AtomicBoolean(false);

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			removeTerraShield();
		}
	}

	private void removeTerraShield() {
		getOwner().getEffectController().removeEffect(22847); //Terrashield.
	}

	private void terraShield() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 22847, 1, getOwner()).useNoAnimationSkill(); //Terrashield.
	}

	@Override
	protected void handleSpawned() {
		terraShield();
		super.handleSpawned();
	}

	@Override
	protected void handleBackHome() {
		terraShield();
		super.handleBackHome();
	}
}
