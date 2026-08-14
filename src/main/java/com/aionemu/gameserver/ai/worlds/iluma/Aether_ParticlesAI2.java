package com.aionemu.gameserver.ai.worlds.iluma;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Iluma 区域 NPC AI：Aether Particles（@AIName "aether_particles"），继承 AggressiveNpcAI2。
 * Iluma zone NPC AI: Aether Particles (@AIName "aether_particles"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("aether_particles")
public class Aether_ParticlesAI2 extends AggressiveNpcAI2
{
	@Override
	public void think() {
	}
	
	private AtomicBoolean startedEvent = new AtomicBoolean(false);
	
	@Override
	protected void handleCreatureMoved(Creature creature) {
		if (creature instanceof Player) {
			final Player player = (Player) creature;
			if (MathUtil.getDistance(getOwner(), player) <= 5) {
				if (startedEvent.compareAndSet(false, true)) {
					GameEngineServices.skillEngine().getSkill(player, 22894, 1, player).useNoAnimationSkill(); // 消失技能 / Vanish.
					AI2Actions.deleteOwner(Aether_ParticlesAI2.this);
					AI2Actions.scheduleRespawn(this);
				}
			}
		}
	}
	
	@Override
	public boolean isMoveSupported() {
		return false;
	}
}
