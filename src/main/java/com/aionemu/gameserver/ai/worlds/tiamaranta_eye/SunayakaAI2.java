package com.aionemu.gameserver.ai.worlds.tiamaranta_eye;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.network.aion.serverpackets.*;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Future;

/**
 * Tiamaranta eye 区域 NPC AI：Sunayaka（@AIName "sunayaka"），继承 AggressiveNpcAI2。
 * Tiamaranta eye zone NPC AI: Sunayaka (@AIName "sunayaka"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("sunayaka")
public class SunayakaAI2 extends AggressiveNpcAI2
{
	private boolean canThink = true;
	private Future<?> sunayakaRageTask;
	private AtomicBoolean isAggred = new AtomicBoolean(false);

	private void simmeringRage() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 20651, 1, getOwner()).useNoAnimationSkill(); //Simmering Rage.
		getOwner().getEffectController().removeEffect(8763);
	}

	private void rageOfTheDragonLords() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 8763, 1, getOwner()).useNoAnimationSkill(); //Rage Of The Dragon Lords
	}

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			switch (getNpcId()) {
				case 218553: //Governor Sunayaka.
				case 219311: //Berserker Sunayaka.
				//////////////////////////////////
				case 249144: //Governor Sunayaka.
				case 249145: //Berserker Sunayaka.
					// 狂战士苏纳亚卡在开战 15 分钟后狂暴。 / Berserker Sunayaka goes berserk 15 minutes after the battle starts.
					GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1401459, 0);
					getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
						@Override
						public void visit(Player player) {
							if (player.isOnline()) {
								PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 900)); //15 Minutes.
							}
						}
					});
					sunayakaRageTask = GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
						@Override
						public void run() {
							// 狂战士苏纳亚卡已狂暴。 / Berserker Sunayaka has gone berserk.
							GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1401460, 0);
							getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
								@Override
								public void visit(Player player) {
									simmeringRage();
									if (player.isOnline()) {
										PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 0));
									}
								}
							});
						}
					}, 900000); //15Min...
				break;
			}
		}
	}

	@Override
	public boolean canThink() {
		return canThink;
	}

	private void cancelSunayakaRageTask() {
		if (sunayakaRageTask != null && !sunayakaRageTask.isDone()) {
			sunayakaRageTask.cancel(true);
		}
	}

	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		rageOfTheDragonLords();
	}

	@Override
	protected void handleDespawned() {
		cancelSunayakaRageTask();
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 0));
				}
			}
		});
		super.handleDespawned();
	}

	@Override
	protected void handleBackHome() {
		canThink = true;
		isAggred.set(false);
		cancelSunayakaRageTask();
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 0));
				}
			}
		});
		super.handleBackHome();
	}

	@Override
	protected void handleDied() {
		cancelSunayakaRageTask();
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 0));
				}
			}
		});
		super.handleDied();
	}
}
