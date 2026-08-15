package com.aionemu.gameserver.ai.worlds.verteron;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;

/**
 * Verteron 区域 NPC AI：Poisonous Bubblegut（@AIName "poisonous_bubblegut"），继承 AggressiveNpcAI2。
 * Verteron zone NPC AI: Poisonous Bubblegut (@AIName "poisonous_bubblegut"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("poisonous_bubblegut")
public class Poisonous_BubblegutAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleSpawned() {
		protectionFluid();
		super.handleSpawned();
	}

	private void protectionFluid() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 16447, 1, getOwner()).useNoAnimationSkill(); // 吐出粘稠的保护液。 / Spout Sticky Protection Fluid.
	}

}
