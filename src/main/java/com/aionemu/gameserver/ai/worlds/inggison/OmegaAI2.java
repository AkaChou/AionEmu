package com.aionemu.gameserver.ai.worlds.inggison;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Inggison 区域 NPC AI：Omega（@AIName "omega"），继承 AggressiveNpcAI2。
 * Inggison zone NPC AI: Omega (@AIName "omega"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("omega")
public class OmegaAI2 extends AggressiveNpcAI2
{
	private Future<?> phaseTask;
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	private AtomicBoolean isStartedEvent = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
		}
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 90) {
			if (isStartedEvent.compareAndSet(false, true)) {
				startPhaseTask();
			}
		} if (hpPercentage <= 50) {
			if (isStartedEvent.compareAndSet(false, true)) {
				startPhaseTask();
			}
		} if (hpPercentage <= 10) {
			if (isStartedEvent.compareAndSet(false, true)) {
				startPhaseTask();
			}
		}
	}
	
	private void startPhaseTask() {
		phaseTask = GameThreadPoolServices.threadPoolManager().scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				if (isAlreadyDead()) {
					cancelPhaseTask();
				} else {
					GameEngineServices.skillEngine().getSkill(getOwner(), 18671, 60, getOwner()).useNoAnimationSkill(); // 魔法护盾 / Magic Ward.
					List<Player> players = getLifedPlayers();
					if (!players.isEmpty()) {
						int size = players.size();
						if (players.size() < 6) {
							for (Player p: players) {
								spawnOmegaClone(p);
							}
						} else {
							int count = Rnd.get(6, size);
							for (int i = 0; i < count; i++) {
								if (players.isEmpty()) {
									break;
								}
								spawnOmegaClone(players.get(Rnd.get(players.size())));
							}
						}
					}
				}
			}
		}, 3000, 15000);
	}
	
	private void spawnOmegaClone(Player player) {
		final float x = player.getX();
		final float y = player.getY();
		final float z = player.getZ();
		if (x > 0 && y > 0 && z > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					if (!isAlreadyDead()) {
						switch (Rnd.get(1, 5)) {
						    case 1:
							    // 欧米伽召唤生物。 / Omega summons a creature.
								GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1400606, 0);
								spawn(281945, x, y, z, (byte) 0); // 力量分身 / Clone Of Power.
							break;
							case 2:
							    // 欧米伽召唤强大生物。 / Omega summons a powerful creature.
								GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1400607, 0);
							    spawn(281946, x, y, z, (byte) 0); // 爆炸分身 / Clone Of Explosion.
							break;
							case 3:
							    // 欧米伽召唤治疗生物。 / Omega summons a healing creature.
								GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1400608, 0);
							    spawn(281947, x, y, z, (byte) 0); // 治疗分身 / Clone Of Healing.
							break;
							case 4:
							    // 欧米伽召唤制造屏障的生物。 / Omega summons a creature that creates barriers.
								GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1400609, 0);
							    spawn(281948, x, y, z, (byte) 0); // 物理屏障分身 / Clone Of Physical Barrier.
							break;
							case 5:
							    // 欧米伽召唤制造屏障的生物。 / Omega summons a creature that creates barriers.
								GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1400609, 0);
							    spawn(281949, x, y, z, (byte) 0); // 魔法屏障分身 / Clone Of Magical Barrier.
							break;
						}
					}
				}
			}, 1000);
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
		if (phaseTask != null && !phaseTask.isDone()) {
			phaseTask.cancel(true);
		}
	}
	
	@Override
	protected void handleBackHome() {
		cancelPhaseTask();
		isStartedEvent.set(false);
		isAggred.set(false);
		super.handleBackHome();
	}
	
	@Override
	protected void handleDespawned() {
		cancelPhaseTask();
		super.handleDespawned();
	}
	
	@Override
	protected void handleDied() {
		final WorldPosition p = getPosition();
		if (p != null) {
			deleteNpcs(p.getWorldMapInstance().getNpcs(281945)); // 力量分身 / Clone Of Power.
			deleteNpcs(p.getWorldMapInstance().getNpcs(281946)); // 爆炸分身 / Clone Of Explosion.
			deleteNpcs(p.getWorldMapInstance().getNpcs(281947)); // 治疗分身 / Clone Of Healing.
			deleteNpcs(p.getWorldMapInstance().getNpcs(281948)); // 物理屏障分身 / Clone Of Physical Barrier.
			deleteNpcs(p.getWorldMapInstance().getNpcs(281949)); // 魔法屏障分身 / Clone Of Magical Barrier.
		}
		cancelPhaseTask();
		super.handleDied();
	}
	
	private void deleteNpcs(List<Npc> npcs) {
		for (Npc npc: npcs) {
			if (npc != null) {
				npc.getController().onDelete();
			}
		}
	}
}
