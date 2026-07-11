package com.aionemu.gameserver.ai.instance.drakenspireDepths;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.skillengine.SkillEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Drakenspire Depths 副本 NPC AI：Orissan（@AIName "orissan"），继承 AggressiveNpcAI2。
 * Drakenspire Depths instance NPC AI: Orissan (@AIName "orissan"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("orissan")
public class OrissanAI2 extends AggressiveNpcAI2
{
	private int orissanPhase = 0;
	private Future<?> crystalTask;
	private boolean canThink = true;
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
		}
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage == 95 && orissanPhase < 1) {
			orissanPhase = 1;
			spawnFrigidCrystal();
		} if (hpPercentage == 75 && orissanPhase < 2) {
			orissanPhase = 2;
			spawnFrigidCrystal();
		} if (hpPercentage == 55 && orissanPhase < 3) {
			orissanPhase = 3;
			spawnFrigidCrystal();
		} if (hpPercentage == 35 && orissanPhase < 4) {
			orissanPhase = 4;
			spawnFrigidCrystal();
		} if (hpPercentage == 15 && orissanPhase < 5) {
			orissanPhase = 5;
			spawnFrigidCrystal();
		}
	}
	
	private void spawnFrigidCrystal() {
		crystalTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (isAlreadyDead()) {
					cancelPhaseTask();
				} else {
					GameEngineServices.skillEngine().getSkill(getOwner(), 21635, 46, getOwner()).useNoAnimationSkill(); //Summon Crystal.
					List<Player> players = getLifedPlayers();
					if (!players.isEmpty()) {
						int size = players.size();
						if (players.size() < 6) {
							for (Player p: players) {
								spawnFrigidCrystal(p);
							}
						} else {
							int count = Rnd.get(6, size);
							for (int i = 0; i < count; i++) {
								if (players.isEmpty()) {
									break;
								}
								spawnFrigidCrystal(players.get(Rnd.get(players.size())));
							}
						}
					}
				}
			}
		}, 20000, 40000);
	}
	
	private void spawnFrigidCrystal(Player player) {
		final float x = player.getX();
		final float y = player.getY();
		final float z = player.getZ();
		if (x > 0 && y > 0 && z > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					if (!isAlreadyDead()) {
						spawn(855699, x, y, z, (byte) 0); //Frigid Crystal.
					}
				}
			}, 3000);
		}
	}
	
	private List<Player> getLifedPlayers() {
		List<Player> players = new ArrayList<Player>();
		for (Player player: getKnownList().getKnownPlayers().values()) {
			if (!PlayerActions.isAlreadyDead(player)) {
				players.add(player);
			}
		}
		return players;
	}
	
	private void cancelPhaseTask() {
		if (crystalTask != null && !crystalTask.isDone()) {
			crystalTask.cancel(true);
		}
	}
	
	@Override
	protected void handleDespawned() {
		cancelPhaseTask();
		super.handleDespawned();
	}
	
	@Override
	protected void handleDied() {
		super.handleDied();
	}
	
	@Override
	public boolean canThink() {
		return canThink;
	}
	
	@Override
	protected void handleBackHome() {
		canThink = true;
		cancelPhaseTask();
		isAggred.set(false);
		super.handleBackHome();
	}
}
