package com.aionemu.gameserver.instance.handlers.scripts.pvparenas;

import java.util.List;

import com.aionemu.gameserver.controllers.attack.AggroInfo;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.autogroup.AGPlayer;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.HarmonyArenaReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.HarmonyGroupReward;
import com.aionemu.gameserver.model.instance.playerreward.PvPArenaPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.ArenaReward;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 合作竞技场副本事件处理器。
 * Instance event handler for Harmony Arena.
 *
 * @author Encom
 */

public class HarmonyArenaInstance extends GeneralInstanceHandler
{
	/** 副本奖励对象 / instance reward object */
	protected HarmonyArenaReward instanceReward;
	/** 副本是否已销毁 / whether the instance is destroyed */
	protected boolean isInstanceDestroyed;
	
	/**
	 * 返回本副本奖励对象。
	 * Return this instance's reward object.
	 *
	 * result
	 */
	@Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
	}
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(final Player player) {
		Integer object = player.getObjectId();
		HarmonyGroupReward group = instanceReward.getHarmonyGroupReward(object);
		if (group == null) {
			throw new IllegalStateException("Harmony player has no retail match side: " + object);
		}
		if (!instanceReward.containPlayer(object)) {
			instanceReward.regPlayerReward(object);
			PvPArenaPlayerReward playerReward = instanceReward.getPlayerReward(object);
			playerReward.setRewardRate(player.getRates().getHarmonyRewardRate());
			playerReward.applyBoostMoraleEffect(player, instanceReward.getRebirthBuffDuration(0));
			instanceReward.setRndPosition(object);
		} else {
			instanceReward.getPlayerReward(object).endAbsence();
			instanceReward.getPlayerReward(object).setRewardRate(player.getRates().getHarmonyRewardRate());
			instanceReward.portToPosition(player);
		}
		applyStageBuff(player);
		sendEnterPacket(player);
	}
	
	private void sendEnterPacket(final Player player) {
		final Integer object = player.getObjectId();
		final HarmonyGroupReward group = instanceReward.getHarmonyGroupReward(object);
		if (group == null) {
			return;
		}
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * opponent
			 */
			@Override
			public void visit(Player opponent) {
				if (!group.containPlayer(opponent.getObjectId())) {
					PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(10, getTime(), getInstanceReward(), object));
					PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(10, getTime(), getInstanceReward(), opponent.getObjectId()));
					PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(3, getTime(), getInstanceReward(), object));
				} else {
					PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(10, getTime(), getInstanceReward(), opponent.getObjectId()));
					if (!object.equals(opponent.getObjectId())) {
						PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(3, getTime(), getInstanceReward(), object));
					}
				}
			}
		});
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(6, getTime(), getInstanceReward(), null));
		instanceReward.sendPacket(4, object);
	}
	
	private void updatePoints(Creature victim) {
		if (!instanceReward.isStartProgress()) {
			return;
		}
		int bonus = 0;
		int rank = 0;
		if (victim instanceof Player) {
			final HarmonyGroupReward victimGroup = instanceReward.getHarmonyGroupReward(victim.getObjectId());
			if (victimGroup == null) {
				return;
			}
			rank = instanceReward.getRank(victimGroup.getPoints());
			victimGroup.addPoints(-instanceReward.getDeathScore(rank));
			bonus = instanceReward.getKillScore() * instanceReward.getScoreModifier(rank) / 100;
			instanceReward.sendPacket(10, victim.getObjectId());
		} else {
			bonus = getNpcBonus(((Npc) victim).getNpcId());
		} if (bonus == 0) {
			return;
		}
		for (AggroInfo damager : victim.getAggroList().getList()) {
			if (!(damager.getAttacker() instanceof Creature)) {
				continue;
			}
			Creature master = ((Creature) damager.getAttacker()).getMaster();
			if (master == null) {
				continue;
			} if (master instanceof Player) {
				Player attaker = (Player) master;
				HarmonyGroupReward attackerGroup = instanceReward.getHarmonyGroupReward(attaker.getObjectId());
				if (attackerGroup == null) {
					continue;
				}
				int rewardPoints = bonus * damager.getDamage() / victim.getAggroList().getTotalDamage();
				attackerGroup.addPoints(rewardPoints);
				sendSystemMsg(attaker, victim, rewardPoints);
				instanceReward.sendPacket(10, attaker.getObjectId());
			}
		} if (instanceReward.hasCapPoints()) {
			finishBattle(true);
		}
	}
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
	public void onDie(Npc npc) {
		if (npc.getAggroList().getMostPlayerDamage() == null) {
			return;
		}
		updatePoints(npc);
	}
	/**
	 * 处理 sendSystemMsg。
	 * Handle sendSystemMsg.
	 *
	 * 玩家 / player
	 * creature
	 * rewardPoints
	 */
	
	protected void sendSystemMsg(Player player, Creature creature, int rewardPoints) {
		int nameId = creature.getObjectTemplate().getNameId();
		DescriptionId name = new DescriptionId(nameId * 2 + 1);
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400237, nameId == 0 ? creature.getName() : name, rewardPoints));
	}
	
	private int getNpcBonus(int npcId) {
		switch (npcId) {
			case 207102:
			case 207116:
			case 243678: //Roaming Volcanic Petrahulk.
				return 400;
			case 207099:
				return 200;
			case 243679: //Heated Negotiator Grangvolkan.
				return 100;
			case 219328: //Plaza Wall.	
			case 243680: //Lurking Fangwing.
				return 50;
			default:
				return 0;
		}
	}
	
	private int getTime() {
		return instanceReward.getTime();
	}
	
	/**
	 * 玩家登录到该副本时处理。
	 * Handle a player logging into this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogin(Player player) {
		PvPArenaPlayerReward playerReward = instanceReward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.endAbsence();
			playerReward.setRewardRate(player.getRates().getHarmonyRewardRate());
			applyStageBuff(player);
		}
		sendEnterPacket(player);
	}

	@Override
	public void onPlayerLogOut(Player player) {
		PvPArenaPlayerReward playerReward = instanceReward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.beginAbsence();
		}
	}
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		instanceReward = new HarmonyArenaReward(mapId, instanceId, instance);
		instanceReward.setInstanceScoreType(InstanceScoreType.PREPARING);
		instanceReward.setInstanceStartTime();
		spawnRings();
		GameThreadPoolServices.threadPoolManager().schedule(this::startBattle,
				instanceReward.getWaitTimeSeconds() * 1000L);
	}

	private void startBattle() {
		if (isInstanceDestroyed || instanceReward.isRewarded() || !canStart()) {
			return;
		}
		openDoors();
		sendMsgByRace(1401181, Race.PC_ALL, 0);
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		instanceReward.sendPacket(10, null);
		instanceReward.sendPacket(2, null);
		GameThreadPoolServices.threadPoolManager().schedule(this::finishRound,
				instanceReward.getStageTimeSeconds() * 1000L);
	}

	private void finishRound() {
		if (isInstanceDestroyed || instanceReward.isRewarded()) {
			return;
		}
		if (instanceReward.getRound() == 3) {
			finishBattle(true);
			return;
		}
		removeStageBuffs();
		instanceReward.setRound(instanceReward.getRound() + 1);
		instanceReward.setRndZone();
		applyStageBuffs();
		instanceReward.sendPacket(10, null);
		instanceReward.sendPacket(2, null);
		changeZone();
		if (instanceReward.getRound() == instanceReward.getScoreModifierStartStage()) {
			sendMsgByRace(1401491, Race.PC_ALL, 2000);
		}
		GameThreadPoolServices.threadPoolManager().schedule(this::finishRound,
				instanceReward.getStageTimeSeconds() * 1000L);
	}
	/**
	 * 处理 spawnRings。
	 * Handle spawnRings.
	 */
	
	protected void spawnRings() {
	}
	
	private boolean canStart() {
		if (instanceReward.getParticipatingGroups().size() < 2) {
			// 独自一人时无法使用。 / Unavailable to use when you're alone.
			sendMsgByRace(1403045, Race.PC_ALL, 0);
			finishBattle(false);
			return false;
		}
		return true;
	}
	
	private void changeZone() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				for (Player player : instance.getPlayersInside()) {
					instanceReward.portToPosition(player);
					instanceReward.sendPacket(4, player.getObjectId());
				}
			}
		}, 1000);
	}
	
	/**
	 * 玩家离开副本时处理。
	 * Handle a player leaving the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onLeaveInstance(Player player) {
		PvPArenaPlayerReward playerReward = instanceReward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.endBoostMoraleEffect(player);
			playerReward.endStageBuff(player);
			playerReward.beginAbsence();
			instanceReward.clearPosition(playerReward.getPosition(), Boolean.FALSE);
		}
	}
	/**
	 * 处理 sendMsgByRace。
	 * Handle sendMsgByRace.
	 *
	 * message
	 * 阵营 / race
	 * time
	 */
	
	protected void sendMsgByRace(final int msg, final Race race, int time) {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				instance.doOnAllPlayers(new Visitor<Player>() {
					/**
					 * 处理 visit。
					 * Handle visit.
					 *
					 * @param player 玩家 / player
					 */
					@Override
					public void visit(Player player) {
						if (player.getRace().equals(race) || race.equals(Race.PC_ALL)) {
							PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msg));
						}
					}
				});
			}
		}, time);
	}
	
	/**
	 * 玩家请求退出副本时处理。
	 * Handle a player exit request.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onExitInstance(Player player) {
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}
	
	private void openDoors() {
		for (StaticDoor door : instance.getDoors().values()) {
			if (door != null) {
				door.setOpen(true);
			}
		}
	}
	/**
	 * 处理 reward。
	 * Handle reward.
	 */
	
	protected synchronized void reward() {
		finishBattle(true);
	}

	private synchronized void finishBattle(boolean settleRewards) {
		if (isInstanceDestroyed || instanceReward.isRewarded()) {
			return;
		}
		removeStageBuffs();
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		long endedAt = System.currentTimeMillis();
		for (PvPArenaPlayerReward playerReward : instanceReward.getInstanceRewards()) {
			playerReward.finalizePlaytimeBonus(instanceReward.getTotalPlayMillis(), endedAt);
			HarmonyGroupReward group = instanceReward.getHarmonyGroupReward(playerReward.getOwner());
			if (group != null) {
				group.addPoints(playerReward.getTimeBonus());
			}
		}
		List<HarmonyGroupReward> groups = instanceReward.getParticipatingGroups();
		if (settleRewards && instanceReward.canRewarded() && !groups.isEmpty()) {
			int groupCount = groups.size();
			int totalScore = instanceReward.getTotalPoints();
			for (HarmonyGroupReward group : groups) {
				int rank = instanceReward.getRank(group.getPoints());
				for (AGPlayer member : group.getAGPlayers()) {
					PvPArenaPlayerReward playerReward = instanceReward.getPlayerReward(member.getObjectId());
					if (playerReward == null) {
						continue;
					}
					ArenaReward arenaReward = InstanceSettlementService.arenaReward(instanceReward.getArenaRow(), rank,
							groupCount, group.getPoints(), totalScore, playerReward.getRewardRate());
					playerReward.applyArenaReward(arenaReward);
					Player player = instance.getPlayer(member.getObjectId());
					if (player == null) {
						InstanceSettlementService.queue(instance, member.getObjectId(), "arena", arenaReward.plan());
						continue;
					}
					if (PlayerActions.isAlreadyDead(player)) {
						PlayerReviveService.duelRevive(player);
					}
					InstanceSettlementService.settle(instance.getDynamicInstance().getInstanceUid(), player, "arena",
							arenaReward.plan());
				}
			}
		}
		instanceReward.sendPacket(5, null);
		for (Npc npc : instance.getNpcs()) {
			npc.getController().onDelete();
		}
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/**
			 * 处理 run。
			 * Handle run.
			 */
			@Override
			public void run() {
				if (!isInstanceDestroyed) {
					for (Player player : instance.getPlayersInside()) {
						if (PlayerActions.isAlreadyDead(player)) {
							PlayerReviveService.duelRevive(player);
						}
						onExitInstance(player);
					}
					GameCoreGameplayServices.autoGroupService().unRegisterInstance(instanceId);
				}
			}
		}, 60000);
	}

	private void applyStageBuffs() {
		for (Player player : instance.getPlayersInside()) {
			applyStageBuff(player);
		}
	}

	private void applyStageBuff(Player player) {
		if (!instanceReward.isStartProgress()) {
			return;
		}
		int buffId = instanceReward.getStageEndBuffId(instanceReward.getRound());
		int targetRank = instanceReward.getStageEndBuffTargetRank(instanceReward.getRound());
		PvPArenaPlayerReward playerReward = instanceReward.getPlayerReward(player.getObjectId());
		HarmonyGroupReward group = instanceReward.getHarmonyGroupReward(player.getObjectId());
		if (buffId > 0 && playerReward != null && group != null
				&& instanceReward.getRank(group.getPoints()) + 1 >= targetRank) {
			playerReward.applyStageBuff(player, buffId, instanceReward.getStageTimeSeconds() * 1000);
		}
	}

	private void removeStageBuffs() {
		for (Player player : instance.getPlayersInside()) {
			PvPArenaPlayerReward playerReward = instanceReward.getPlayerReward(player.getObjectId());
			if (playerReward != null) {
				playerReward.endStageBuff(player);
			}
		}
	}
	
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * 玩家 / player
	 * @param lastAttacker 最后攻击者 / last attacker
	 * result
	 */
	@Override
	public boolean onDie(Player player, Creature lastAttacker) {
		PvPArenaPlayerReward ownerReward = instanceReward.getPlayerReward(player.getObjectId());
		HarmonyGroupReward ownerGroup = instanceReward.getHarmonyGroupReward(player.getObjectId());
		if (ownerReward == null || ownerGroup == null) {
			return true;
		}
		ownerReward.endBoostMoraleEffect(player);
		ownerReward.applyBoostMoraleEffect(player,
				instanceReward.getRebirthBuffDuration(instanceReward.getRank(ownerGroup.getPoints())));
		instanceReward.sendPacket(4, player.getObjectId());
		if (lastAttacker != null && lastAttacker != player) {
			if (lastAttacker instanceof Player) {
				Player winner = (Player) lastAttacker;
				Integer winnerObj = winner.getObjectId();
				HarmonyGroupReward winnerGroup = instanceReward.getHarmonyGroupReward(winnerObj);
				if (winnerGroup != null) {
					winnerGroup.addPvPKillToPlayer();
				}
				int worldId = winner.getWorldId();
				GameEngineServices.questEngine().onKillInWorld(new QuestEnv(player, winner, 0, 0), worldId);
			}
		}
		updatePoints(player);
		return true;
	}
	
	/**
	 * 玩家对 NPC 使用物品完成时处理。
	 * Handle item-use finish on an NPC.
	 *
	 * 玩家 / player
	 * npc
	 */
	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		final Integer object = player.getObjectId();
		final HarmonyGroupReward group = instanceReward.getHarmonyGroupReward(object);
		if (!instanceReward.isStartProgress() || group == null) {
			return;
		}
		int rewardetPoints = getNpcBonus(npc.getNpcId());
		int skill = instanceReward.getNpcBonusSkill(npc.getNpcId());
		if (skill != 0) {
			useSkill(npc, player, skill >> 8, skill & 0xFF);
		}
		group.addPoints(rewardetPoints);
		sendSystemMsg(player, npc, rewardetPoints);
		instanceReward.sendPacket(10, object);
	}
	/**
	 * 处理 useSkill。
	 * Handle useSkill.
	 *
	 * npc
	 * 玩家 / player
	 * skill id
	 * level
	 */
	
	protected void useSkill(Npc npc, Player player, int skillId, int level) {
		GameEngineServices.skillEngine().getSkill(npc, skillId, level, player).useNoAnimationSkill();
	}
	
	/**
	 * 处理玩家复活事件。
	 * Handle a player revive event.
	 *
	 * 玩家 / player
	 * result
	 */
	@Override
	public boolean onReviveEvent(Player player) {
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		PlayerReviveService.revive(player, 100, 100, false, 0);
		player.getGameStats().updateStatsAndSpeedVisually();
		if (!isInstanceDestroyed) {
			instanceReward.portToPosition(player);
		}
		return true;
	}
	
	/**
	 * 副本销毁时清理资源。
	 * Clean up resources when the instance is destroyed.
	 */
	@Override
	public void onInstanceDestroy() {
		isInstanceDestroyed = true;
		instanceReward.clear();
	}
}
