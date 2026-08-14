package com.aionemu.gameserver.ai.instance.darkPoeta;

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
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dark Poeta 副本 NPC AI：Inferno Demon（@AIName "inferno_demon"），继承 AggressiveNpcAI2。
 * Dark Poeta instance NPC AI: Inferno Demon (@AIName "inferno_demon"), extends AggressiveNpcAI2.
 *
 * @author Rinzler
 * @author Ranastic (Encom)
 */
@AIName("inferno_demon")
public class Inferno_DemonAI2 extends AggressiveNpcAI2
{
	private Future<?> phaseTask;
	private boolean canThink = true;
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	private AtomicBoolean isStartedEvent = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			switch (getNpcId()) {
				case 237373: // 炼狱恶魔。 / Inferno Demon.
					// 你有 15 分钟找到辉煌元素，否则它将逃走。 / You have 15 minutes to find the Brilliant Elemental before it flees.
					PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_Teo_T_TimeAttack_01, 0);
					// 破碎元素正在积蓄力量。聚集在一起以分摊伤害。 / The Fractured Elemental is building up power. Gather together to disperse the damage.
					PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_Teo_T_Boss_Skill_02, 4000);
				break;
			}
		}
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 95) {
			if (isStartedEvent.compareAndSet(false, true)) {
				// 破碎元素领主即将爆发。聚集在一起分摊伤害。 / Fractured Elemental Lord is boiling over. Gather together to spread the damage out.
				PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_Teo_T_Boss_Skill_01, 0);
				startPhaseTask();
			}
		} if (hpPercentage <= 70) {
			if (isStartedEvent.compareAndSet(false, true)) {
				startPhaseTask();
			}
		} if (hpPercentage <= 50) {
			if (isStartedEvent.compareAndSet(false, true)) {
				startPhaseTask();
			}
		} if (hpPercentage <= 30) {
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
					// 元素力量即将以毁灭性爆炸释放。 / A massive blast of elemental power will soon explode with destructive force.
					PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_Teo_T_Boss_Skill_03, 0);
					GameEngineServices.skillEngine().getSkill(getOwner(), 19567, 46, getOwner()).useNoAnimationSkill(); // 引力位移。 / Gravitational Shift.
					List<Player> players = getLifedPlayers();
					if (!players.isEmpty()) {
						int size = players.size();
						if (players.size() < 6) {
							for (Player p: players) {
								spawnDimensionalIntruder(p);
							}
						} else {
							int count = Rnd.get(6, size);
							for (int i = 0; i < count; i++) {
								if (players.isEmpty()) {
									break;
								}
								spawnDimensionalIntruder(players.get(Rnd.get(players.size())));
							}
						}
					}
				}
			}
		}, 20000, 40000);
	}
	
	private void spawnDimensionalIntruder(Player player) {
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
							    spawn(237374, x, y, z, (byte) 0); // 变异的次元入侵者。 / Mutated Dimensional Intruder.
								spawn(856597, x, y, z, (byte) 0);
							break;
							case 2:
							    spawn(237381, x, y, z, (byte) 0); // 警惕的次元入侵者。 / Wary Dimensional Intruder.
								spawn(856597, x, y, z, (byte) 0);
							break;
						}
					}
				}
			}, 1000);
		}
	}
	
	@Override
	public boolean canThink() {
		return canThink;
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
	
	private void deleteHelpers() {
		WorldMapInstance instance = getPosition().getWorldMapInstance();
		if (instance != null) {
			deleteNpcs(instance.getNpcs(237374)); // 变异的次元入侵者。 / Mutated Dimensional Intruder.
			deleteNpcs(instance.getNpcs(237381)); // 警惕的次元入侵者。 / Wary Dimensional Intruder.
			deleteNpcs(instance.getNpcs(856597));
		}
	}
	
	@Override
	protected void handleDied() {
		cancelPhaseTask();
		final WorldPosition p = getPosition();
		if (p != null) {
			deleteNpcs(p.getWorldMapInstance().getNpcs(237374)); // 变异的次元入侵者。 / Mutated Dimensional Intruder.
			deleteNpcs(p.getWorldMapInstance().getNpcs(237381)); // 警惕的次元入侵者。 / Wary Dimensional Intruder.
			deleteNpcs(p.getWorldMapInstance().getNpcs(856597));
		}
		// 辉煌元素已消失。 / The Brilliant Elemental has disappeared.
		PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_Teo_T_End_01, 0);
		// 辉煌元素未能找到力量焦点而消散。 / The Brilliant Elemental has failed to find a focus for its power and has faded.
		PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_Teo_T_End_02, 4000);
		super.handleDied();
	}
	
	private void deleteNpcs(List<Npc> npcs) {
		for (Npc npc: npcs) {
			if (npc != null) {
				npc.getController().onDelete();
			}
		}
	}
	
	@Override
	protected void handleDespawned() {
		cancelPhaseTask();
		super.handleDespawned();
	}
	
	@Override
	protected void handleBackHome() {
		canThink = true;
		cancelPhaseTask();
		deleteHelpers();
		isAggred.set(false);
		isStartedEvent.set(false);
		super.handleBackHome();
	}
}
