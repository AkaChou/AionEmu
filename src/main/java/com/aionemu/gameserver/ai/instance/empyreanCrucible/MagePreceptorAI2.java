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
import java.util.Collections;
import java.util.List;

/**
 * Empyrean Crucible 副本 NPC AI：Mage Preceptor（@AIName "mage_preceptor"），继承 AggressiveNpcAI2。
 * Empyrean Crucible instance NPC AI: Mage Preceptor (@AIName "mage_preceptor"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("mage_preceptor")
public class MagePreceptorAI2 extends AggressiveNpcAI2 {

	private List<Integer> percents = new ArrayList<Integer>();

	@Override
	public void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}

	private void startEvent(int percent) {
		if (percent == 50 || percent == 25) {
			GameEngineServices.skillEngine().getSkill(getOwner(), 19606, 46, getTarget()).useNoAnimationSkill();
		} switch (percent) {
			case 75:
				GameEngineServices.skillEngine().getSkill(getOwner(), 19605, 46, getTargetPlayer()).useNoAnimationSkill();
			break;
			case 50:
				GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
					@Override
					public void run() {
						if (!isAlreadyDead()) {
							GameEngineServices.skillEngine().getSkill(getOwner(), 19609, 46, getOwner()).useNoAnimationSkill();
							GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
								@Override
								public void run() {
									WorldPosition p = getPosition();
									switch (Rnd.get(1, 2)) {
										case 1:
										    spawn(282363, p.getX(), p.getY(), p.getZ(), p.getHeading()); //Summoned Tran Of Fire.
										break;
										case 2:
										    spawn(282364, p.getX(), p.getY(), p.getZ(), p.getHeading()); //Summoned Tran Of Wind.
										break;
									}
									scheduleSkill(2000);
								}
							}, 4500);
						}
					}
				}, 3000);
			break;
			case 25:
				scheduleSkill(3000);
				scheduleSkill(9000);
				scheduleSkill(15000);
			break;
		}
	}

	private void scheduleSkill(int delay) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				if (!isAlreadyDead()) {
					GameEngineServices.skillEngine().getSkill(getOwner(), 19605, 46, getTargetPlayer()).useNoAnimationSkill();
				}
			}
		}, delay);
	}

	private Player getTargetPlayer() {
		List<Player> players = new ArrayList<Player>();
		for (Player player : getKnownList().getKnownPlayers().values()) {
			if (!PlayerActions.isAlreadyDead(player) && MathUtil.isIn3dRange(player, getOwner(), 37)) {
				players.add(player);
			}
		}
		return players.get(Rnd.get(players.size()));
	}

	private void checkPercentage(int percentage) {
		for (Integer percent : percents) {
			if (percentage <= percent) {
				percents.remove(percent);
				startEvent(percent);
				break;
			}
		}
	}
	private void addPercents() {
		percents.clear();
		Collections.addAll(percents, new Integer[] {75, 50, 25});
	}

	@Override
	public void handleSpawned() {
		super.handleSpawned();
		addPercents();
	}

	@Override
	public void handleDespawned() {
		percents.clear();
		despawnNpcs();
		super.handleDespawned();
	}

	@Override
	public void handleDied() {
		despawnNpcs();
		super.handleDied();
	}

	@Override
	public void handleBackHome() {
		addPercents();
		despawnNpcs();
		super.handleBackHome();
	}

	private void despawnNpcs() {
		despawnNpc(getPosition().getWorldMapInstance().getNpc(282364));
		despawnNpc(getPosition().getWorldMapInstance().getNpc(282363));
	}

	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}
}
