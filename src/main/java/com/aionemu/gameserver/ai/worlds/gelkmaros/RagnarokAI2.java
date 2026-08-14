package com.aionemu.gameserver.ai.worlds.gelkmaros;

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
 * Gelkmaros 区域 NPC AI：Ragnarok（@AIName "ragnarok"），继承 AggressiveNpcAI2。
 * Gelkmaros zone NPC AI: Ragnarok (@AIName "ragnarok"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("ragnarok")
public class RagnarokAI2 extends AggressiveNpcAI2
{
	// 阶段定时任务：周期性施放自伤技能并召唤诸神黄昏仆从。 / Phase task: periodically casts the self-harm skill and spawns Ragnarok minions.
	private Future<?> phaseTask;
	// 是否已进入攻击状态（首次受到攻击置位）。 / Whether already aggroed (set on first attack).
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	// 各血量阶段事件是否已启动。 / Whether the phase event for a given HP threshold has started.
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
				// 毒素与麻痹攻击开始。 / Attack of poison and paralysis begins.
				GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1400610, 0);
				startPhaseTask();
			}
		} if (hpPercentage <= 50) {
			if (isStartedEvent.compareAndSet(false, true)) {
				// 限制物理与魔法攻击的攻势开始。 / Attack that restricts physical and magical assaults begins.
				GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1400611, 0);
				startPhaseTask();
			}
		} if (hpPercentage <= 10) {
			if (isStartedEvent.compareAndSet(false, true)) {
				// 强力持续攻击与反射开始。 / Powerful continuous attacks and reflections begin.
				GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1400613, 0);
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
					GameEngineServices.skillEngine().getSkill(getOwner(), 18679, 60, getOwner()).useNoAnimationSkill(); //Self Harm.
					List<Player> players = getLifedPlayers();
					if (!players.isEmpty()) {
						int size = players.size();
						if (players.size() < 6) {
							for (Player p: players) {
								spawnRagnarok(p);
							}
						} else {
							int count = Rnd.get(6, size);
							for (int i = 0; i < count; i++) {
								if (players.isEmpty()) {
									break;
								}
								spawnRagnarok(players.get(Rnd.get(players.size())));
							}
						}
					}
				}
			}
		}, 3000, 15000);
	}
	
	private void spawnRagnarok(Player player) {
		final float x = player.getX();
		final float y = player.getY();
		final float z = player.getZ();
		if (x > 0 && y > 0 && z > 0) {
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					if (!isAlreadyDead()) {
						switch (Rnd.get(1, 2)) {
						    case 1:
								spawn(281950, x, y, z, (byte) 0); //Ragnarok's Parasite.
							break;
							case 2:
							    // 诸神黄昏的酸液出现。 / Ragnarok's acidic fluid appears.
								GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1400612, 2000);
							    spawn(281951, x, y, z, (byte) 0); //Ragnarok Slime.
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
			deleteNpcs(p.getWorldMapInstance().getNpcs(281950)); //Ragnarok's Parasite.
			deleteNpcs(p.getWorldMapInstance().getNpcs(281951)); //Ragnarok Slime.
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
