package com.aionemu.gameserver.ai.instance.empyreanCrucible;

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
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.world.WorldPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Empyrean Crucible 副本 NPC AI：Priest Elyos Preceptor（@AIName "priest_elyos_preceptor"），继承 AggressiveNpcAI2。
 * Empyrean Crucible instance NPC AI: Priest Elyos Preceptor (@AIName "priest_elyos_preceptor"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("priest_elyos_preceptor")
public class PriestElyosPreceptorAI2 extends AggressiveNpcAI2 {

	private AtomicBoolean is75EventStarted = new AtomicBoolean(false);
	private AtomicBoolean is25EventStarted = new AtomicBoolean(false);
	
	@Override
	public void handleSpawned() {
		super.handleSpawned();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				GameEngineServices.skillEngine().getSkill(getOwner(), 19612, 46, getOwner()).useNoAnimationSkill();
			}
		}, 1000);
	}
	
	@Override
	public void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int percentage) {
		if (percentage <= 75) {
			if (is75EventStarted.compareAndSet(false, true)) {
				GameEngineServices.skillEngine().getSkill(getOwner(), 19611, 46, getTargetPlayer()).useNoAnimationSkill();
			}
		} if (percentage <= 25) {
			if (is25EventStarted.compareAndSet(false, true)) {
				startEvent();
			}
		}
	}
	
	private void startEvent() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 19610, 46, getOwner()).useNoAnimationSkill();
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				GameEngineServices.skillEngine().getSkill(getOwner(), 19614, 46, getOwner()).useNoAnimationSkill();
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						WorldPosition p = getPosition();
						switch (Rnd.get(1, 3)) {
							case 1:
								applySoulSickness((Npc) spawn(282366, p.getX(), p.getY(), p.getZ(), p.getHeading())); //Boreas.
							break;
							case 2:
								applySoulSickness((Npc) spawn(282367, p.getX(), p.getY(), p.getZ(), p.getHeading())); //Jumentis.
							break;
							case 3:
								applySoulSickness((Npc) spawn(282368, p.getX(), p.getY(), p.getZ(), p.getHeading())); //Charna.
							break;
						}
					}
				}, 5000);
			}
		}, 2000);
	}
	
	private Player getTargetPlayer() {
		List<Player> players = new ArrayList<Player>();
		for (Player player : getKnownList().getKnownPlayers().values()) {
			if (!PlayerActions.isAlreadyDead(player) && MathUtil.isIn3dRange(player, getOwner(), 25)) {
				players.add(player);
			}
		}
		return players.get(Rnd.get(players.size()));
	}
	
	private void applySoulSickness(final Npc npc) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				npc.getLifeStats().setCurrentHpPercent(50);
				GameEngineServices.skillEngine().getSkill(npc, 19594, 4, npc).useNoAnimationSkill();
			}
		}, 1000);
	}

	@Override
	public void handleDespawned() {
		despawnNpcs();
		super.handleDespawned();
	}

	@Override
	public void handleDied() {
		despawnNpcs();
		super.handleDied();
	}

	private void despawnNpcs() {
		despawnNpc(getPosition().getWorldMapInstance().getNpc(282366)); //Boreas.
		despawnNpc(getPosition().getWorldMapInstance().getNpc(282367)); //Jumentis.
		despawnNpc(getPosition().getWorldMapInstance().getNpc(282368)); //Charna.
	}
	
	@Override
	protected void handleBackHome() {
		is75EventStarted.set(false);
		is25EventStarted.set(false);
		super.handleDied();
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
}
