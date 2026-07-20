package com.aionemu.gameserver.instance.handlers.scripts.pvparenas;

import java.util.HashSet;
import java.util.Set;
import java.util.function.IntConsumer;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.attack.AggroInfo;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.StaticDoor;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.PvPArenaReward;
import com.aionemu.gameserver.model.instance.playerreward.PvPArenaPlayerReward;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
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
 * PvP 竞技场副本事件处理器。
 * Instance event handler for PvP Arena.
 *
 * @author Encom
 */

public class PvPArenaInstance extends GeneralInstanceHandler
{
	private static final String STATE = "arena.";
	private static final String PREPARING = "PREPARING";
	private static final String BATTLE = "BATTLE";
	private static final String FINISHED = "FINISHED";
	private static final long EXIT_DELAY = 60_000;

	/** 副本是否已销毁 / whether the instance is destroyed */
	private boolean isInstanceDestroyed;
	/** 副本奖励对象 / instance reward object */
	protected PvPArenaReward instanceReward;
	
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
		PvPArenaPlayerReward ownerReward = getPlayerReward(player.getObjectId());
		ownerReward.endBoostMoraleEffect(player);
		ownerReward.applyBoostMoraleEffect(player,
				instanceReward.getRebirthBuffDuration(instanceReward.getRank(ownerReward.getPoints())));
		sendPacket();
		if (lastAttacker != null && lastAttacker != player) {
				if (lastAttacker instanceof Player) {
					Player winner = (Player) lastAttacker;
					PvPArenaPlayerReward reward = getPlayerReward(winner.getObjectId());
					reward.addPvPKillToPlayer();
					persistPlayer(reward);
				int worldId = winner.getWorldId();
				GameEngineServices.questEngine().onKillInWorld(new QuestEnv(player, winner, 0, 0), worldId);
			}
		}
		updatePoints(player);
		return true;
	}
	
	private void updatePoints(Creature victim) {
		if (!instanceReward.isStartProgress()) {
			return;
		}
		int bonus = 0;
		int rank = 0;
		if (victim instanceof Player) {
			PvPArenaPlayerReward victimFine = getPlayerReward(victim.getObjectId());
			rank = instanceReward.getRank(victimFine.getPoints());
			victimFine.addPoints(-instanceReward.getDeathScore(rank));
			persistPlayer(victimFine);
			bonus = instanceReward.getKillScore() * instanceReward.getScoreModifier(rank) / 100;
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
					int rewardPoints = bonus * damager.getDamage() / victim.getAggroList().getTotalDamage();
					PvPArenaPlayerReward reward = getPlayerReward(attaker.getObjectId());
					reward.addPoints(rewardPoints);
					persistPlayer(reward);
				sendSystemMsg(attaker, victim, rewardPoints);
			}
		} if (instanceReward.hasCapPoints()) {
			reward();
		}
		sendPacket();
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
		final int npcId = npc.getNpcId();
		if (npcId == 701173 || //Blessed Relics.
			npcId == 701187) { //Blessed Relics.
			spawnBlessedRelics(30000);
		} if (npcId == 701174 || //Cursed Relics.
			npcId == 701188) { //Cursed Relics.
			spawnCursedRelics(30000);
		}
	}
	
	/**
	 * 玩家进入副本时处理。
	 * Handle a player entering the instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onEnterInstance(Player player) {
		Integer object = player.getObjectId();
		boolean known = containPlayer(object) || runtimeState().get(playerKey(object, "points")) != null;
		PvPArenaPlayerReward playerReward = restorePlayer(object);
		if (!known) {
			playerReward.setRewardRate(rewardRate(player));
			playerReward.applyBoostMoraleEffect(player, instanceReward.getRebirthBuffDuration(0));
			instanceReward.setRndPosition(object);
		} else {
			playerReward.endAbsence();
			playerReward.setRewardRate(rewardRate(player));
			instanceReward.portToPosition(player);
		}
		persistPlayer(playerReward);
		applyStageBuff(player);
		sendPacket();
	}
	
	private void sendPacket(final AionServerPacket packet) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendPacket(player, packet);
			}
		});
	}
	
	private void spawnBlessedRelics(int delay) {
		long deadline = System.currentTimeMillis() + delay;
		runtimeState().put(STATE + "blessed.deadline", deadline);
		scheduleDeadline("blessed", deadline, () -> {
			runtimeState().remove(STATE + "blessed.deadline");
			if (!isInstanceDestroyed && !instanceReward.isRewarded()) {
				spawn(Rnd.get(1, 2) == 1 ? 701173 : 701187, 1841.951f, 1733.968f, 300.242f, (byte) 0);
			}
		});
	}

	private void spawnCursedRelics(int delay) {
		long deadline = System.currentTimeMillis() + delay;
		runtimeState().put(STATE + "cursed.deadline", deadline);
		scheduleDeadline("cursed", deadline, () -> {
			runtimeState().remove(STATE + "cursed.deadline");
			if (!isInstanceDestroyed && !instanceReward.isRewarded()) {
				spawn(Rnd.get(1, 2) == 1 ? 701174 : 701188, 674.517f, 1778.428f, 204.693f, (byte) 0);
			}
		});
	}

	private int getNpcBonus(int npcId) {
		var score = DataManager.RETAIL_AI_DATA == null ? null : DataManager.RETAIL_AI_DATA.getNpcScore(npcId);
		return score == null || score.scoreApplyType() != 0 || score.equalizingScore() != 0 ? 0 : score.value();
	}
	
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
	 * 玩家从该副本登出时处理。
	 * Handle a player logging out from this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogOut(Player player) {
		PvPArenaPlayerReward reward = getPlayerReward(player.getObjectId());
		reward.beginAbsence();
		persistPlayer(reward);
	}
	
	/**
	 * 玩家登录到该副本时处理。
	 * Handle a player logging into this instance.
	 *
	 * @param player 玩家 / player
	 */
	@Override
	public void onPlayerLogin(Player player) {
		PvPArenaPlayerReward playerReward = restorePlayer(player.getObjectId());
		playerReward.endAbsence();
		playerReward.setRewardRate(rewardRate(player));
		persistPlayer(playerReward);
		applyStageBuff(player);
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
		instanceReward = new PvPArenaReward(mapId, instanceId, instance);
		spawnRings();
		restoreLifecycle();
	}

	private void restoreLifecycle() {
		long startedAt = runtimeState().getLong(STATE + "started_at", 0);
		if (startedAt == 0) {
			startedAt = System.currentTimeMillis();
			runtimeState().put(STATE + "started_at", startedAt);
			runtimeState().put(STATE + "phase", PREPARING);
			runtimeState().put(STATE + "round", 1);
			runtimeState().put(STATE + "zone.1", instanceReward.getZone());
		}
		instanceReward.setInstanceStartTime(startedAt);
		int round = runtimeState().getInt(STATE + "round", 1);
		int[] zones = new int[round];
		for (int i = 0; i < round; i++) {
			zones[i] = runtimeState().getInt(STATE + "zone." + (i + 1), 0);
		}
		instanceReward.restoreProgress(round, zones);
		String phase = runtimeState().get(STATE + "phase", PREPARING);
		instanceReward.setInstanceScoreType(switch (phase) {
			case FINISHED -> InstanceScoreType.END_PROGRESS;
			case BATTLE -> InstanceScoreType.START_PROGRESS;
			default -> InstanceScoreType.PREPARING;
		});
		restorePlayers();
		switch (phase) {
		case FINISHED -> scheduleExit(runtimeState().getLong(STATE + "exit_deadline", System.currentTimeMillis()));
		case BATTLE -> {
			openDoors();
				scheduleDeadline("round", runtimeState().getLong(STATE + "round_deadline", System.currentTimeMillis()),
						this::finishRound);
				long zoneDeadline = runtimeState().getLong(STATE + "zone_deadline", 0);
				if (zoneDeadline > 0) {
					scheduleDeadline("zone", zoneDeadline, this::changeZone);
				}
		}
		default -> {
			long deadline = runtimeState().getLong(STATE + "prepare_deadline", 0);
				if (deadline == 0) {
					deadline = startedAt + instanceReward.getWaitTimeSeconds() * 1000L;
					runtimeState().put(STATE + "prepare_deadline", deadline);
				}
				scheduleDeadline("prepare", deadline, this::startBattle);
			}
		}
		if (!instanceReward.isSoloArena() && !FINISHED.equals(phase)) {
			restoreRelic("blessed", this::spawnBlessedRelics);
			restoreRelic("cursed", this::spawnCursedRelics);
		}
	}

	private void startBattle() {
		if (isInstanceDestroyed || instanceReward.isRewarded() || !canStart()) {
			return;
		}
		openDoors();
		sendMsg(1401181);
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		runtimeState().put(STATE + "phase", BATTLE);
		runtimeState().put(STATE + "doors_open", true);
		sendPacket();
		scheduleRound();
	}

	private void finishRound() {
		if (isInstanceDestroyed || instanceReward.isRewarded()) {
			return;
		}
		if (instanceReward.getRound() == 3) {
			finishBattle();
			return;
		}
		removeStageBuffs();
		instanceReward.setRound(instanceReward.getRound() + 1);
		instanceReward.setRndZone();
		runtimeState().put(STATE + "round", instanceReward.getRound());
		runtimeState().put(STATE + "zone." + instanceReward.getRound(), instanceReward.getZone());
		applyStageBuffs();
		scheduleZoneChange();
		if (instanceReward.getRound() == instanceReward.getScoreModifierStartStage()) {
			sendMsg(1401491, 0, false, 25, 2000);
		}
		sendPacket();
		scheduleRound();
	}
	
	private boolean canStart() {
		if (instanceReward.getInstanceRewards().isEmpty()) {
			// 独自一人时无法使用。 / Unavailable to use when you're alone.
			sendMsg(1403045);
			finishBattle();
			return false;
		}
		return true;
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
	
	private boolean containPlayer(Integer object) {
		return instanceReward.containPlayer(object);
	}
	/**
	 * 返回玩家奖励记录。
	 * Return the player's reward record.
	 *
	 * visible object
	 * result
	 */
	
	protected PvPArenaPlayerReward getPlayerReward(Integer object) {
		return restorePlayer(object);
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
			persistPlayer(playerReward);
			instanceReward.clearPosition(playerReward.getPosition(), Boolean.FALSE);
		}
	}
	/**
	 * 向副本内玩家发送数据包。
	 * Send a packet to players in the instance.
	 */
	
	protected void sendPacket() {
		instanceReward.sendPacket();
	}
	
	private void despawnNpc(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
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
	
	private void scheduleZoneChange() {
		long deadline = System.currentTimeMillis() + 1000;
		runtimeState().put(STATE + "zone_deadline", deadline);
		scheduleDeadline("zone", deadline, this::changeZone);
	}

	private void changeZone() {
		runtimeState().remove(STATE + "zone_deadline");
		for (Player player : instance.getPlayersInside()) {
			instanceReward.portToPosition(player);
			persistPlayer(getPlayerReward(player.getObjectId()));
		}
		sendPacket();
	}
	/**
	 * 处理 reward。
	 * Handle reward.
	 */
	
	protected synchronized void reward() {
		finishBattle();
	}

	private synchronized void finishBattle() {
		if (isInstanceDestroyed || instanceReward.isRewarded()) {
			return;
		}
		cancelDeadline("prepare");
		cancelDeadline("round");
		cancelDeadline("zone");
		removeStageBuffs();
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		long endedAt = System.currentTimeMillis();
		for (PvPArenaPlayerReward playerReward : instanceReward.getInstanceRewards()) {
			playerReward.finalizePlaytimeBonus(instanceReward.getTotalPlayMillis(), endedAt);
			runtimeState().put(playerKey(playerReward.getOwner(), "time_bonus"), playerReward.getTimeBonus());
			runtimeState().put(playerKey(playerReward.getOwner(), "participation"),
					playerReward.getParticipationPercent());
		}
		int playerCount = instanceReward.getInstanceRewards().size();
		int totalScore = instanceReward.getTotalPoints();
		for (PvPArenaPlayerReward playerReward : instanceReward.getInstanceRewards()) {
			int rank = instanceReward.getRank(playerReward.getScorePoints());
			ArenaReward arenaReward = InstanceSettlementService.arenaReward(instanceReward.getArenaRow(), rank,
					playerCount, playerReward.getScorePoints(), totalScore, playerReward.getRewardRate());
			playerReward.applyArenaReward(arenaReward);
			Player player = instance.getPlayer(playerReward.getOwner());
			if (player == null) {
				InstanceSettlementService.queue(instance, playerReward.getOwner(), "arena", arenaReward.plan());
				continue;
			}
				if (PlayerActions.isAlreadyDead(player)) {
					PlayerReviveService.duelRevive(player);
				}
				InstanceSettlementService.settle(instance.getDynamicInstance().getInstanceUid(), player, "arena",
						arenaReward.plan());
			}
		runtimeState().put(STATE + "phase", FINISHED);
		runtimeState().put(STATE + "ended_at", endedAt);
		sendPacket();
		for (Npc npc : instance.getNpcs()) {
			npc.getController().onDelete();
		}
		scheduleExit(endedAt + EXIT_DELAY);
	}

	private double rewardRate(Player player) {
		if (instanceReward.isSoloArena()) {
			return player.getRates().getDisciplineRewardRate();
		}
		if (instanceReward.isGlory()) {
			return player.getRates().getGloryRewardRate();
		}
		return player.getRates().getChaosRewardRate();
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
		if (buffId > 0 && playerReward != null
				&& instanceReward.getRank(playerReward.getPoints()) + 1 >= targetRank) {
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
	 * 处理 spawnRings。
	 * Handle spawnRings.
	 */
	
	protected void spawnRings() {
	}
	/**
	 * 返回 npc。
	 * Return the npc.
	 *
	 * @param x X 坐标 / X
	 * @param y Y 坐标 / Y
	 * @param z Z 坐标 / Z
	 * result
	 */
	
	protected Npc getNpc(float x, float y, float z) {
		if (!isInstanceDestroyed) {
			for (Npc npc : instance.getNpcs()) {
				SpawnTemplate st = npc.getSpawn();
				if (st.getX() == x && st.getY() == y && st.getZ() == z) {
					return npc;
				}
			}
		}
		return null;
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
		switch(npc.getNpcId()) {
			case 701169: //Plaza Flame Thrower.
			    despawnNpc(npc);
				spawn(702405, 1798.8951f, 1727.2413f, 302.81836f, (byte) 62);
				spawn(702405, 1808.9938f, 1703.7997f, 302.73233f, (byte) 74);
			break;
			case 701170: //Plaza Flame Thrower.
			    despawnNpc(npc);
				spawn(702405, 1848.1892f, 1689.1056f, 302.74982f, (byte) 92);
				spawn(702405, 1871.4725f, 1699.5228f, 303.0393f, (byte) 104);
			break;
			case 701171: //Plaza Flame Thrower.
			    despawnNpc(npc);
				spawn(702405, 1886.8333f, 1738.3987f, 302.5374f, (byte) 3);
				spawn(702405, 1876.5596f, 1761.9902f, 302.6582f, (byte) 14);
			break;
			case 701172: //Plaza Flame Thrower.
			    despawnNpc(npc);
				spawn(702405, 1837.242f, 1776.3717f, 302.7615f, (byte) 32);
				spawn(702405, 1814.1249f, 1766.2068f, 302.61606f, (byte) 43);
			break;
			case 207102: //Recovery Relics.
				player.getLifeStats().increaseHp(SM_ATTACK_STATUS.TYPE.HP, 10000);
				player.getLifeStats().increaseMp(SM_ATTACK_STATUS.TYPE.MP, 10000);
			break;
			} if (!instanceReward.isStartProgress()) {
			return;
		}
		int rewardetPoints = getNpcBonus(npc.getNpcId());
		int skill = instanceReward.getNpcBonusSkill(npc.getNpcId());
		if (skill != 0) {
			useSkill(npc, player, skill >> 8, skill & 0xFF);
		}
		PvPArenaPlayerReward reward = getPlayerReward(player.getObjectId());
		reward.addPoints(rewardetPoints);
		persistPlayer(reward);
		sendSystemMsg(player, npc, rewardetPoints);
		sendPacket();
	}

	private void scheduleRound() {
		long deadline = System.currentTimeMillis() + instanceReward.getStageTimeSeconds() * 1000L;
		runtimeState().put(STATE + "round_deadline", deadline);
		scheduleDeadline("round", deadline, this::finishRound);
	}

	private void restoreRelic(String name, IntConsumer spawn) {
		long deadline = runtimeState().getLong(STATE + name + ".deadline", 0);
		spawn.accept((int) Math.max(0, deadline - System.currentTimeMillis()));
	}

	private void scheduleExit(long deadline) {
		runtimeState().put(STATE + "exit_deadline", deadline);
		scheduleDeadline("exit", deadline, () -> {
			if (isInstanceDestroyed) {
				return;
			}
			for (Player player : instance.getPlayersInside()) {
				if (PlayerActions.isAlreadyDead(player)) {
					PlayerReviveService.duelRevive(player);
				}
				onExitInstance(player);
			}
			GameCoreGameplayServices.autoGroupService().unRegisterInstance(instance);
		});
	}

	private void restorePlayers() {
		Set<Integer> players = new HashSet<>();
		for (String key : runtimeState().snapshot(STATE + "player.").keySet()) {
			String suffix = key.substring((STATE + "player.").length());
			int separator = suffix.indexOf('.');
			if (separator > 0) {
				players.add(Integer.parseInt(suffix.substring(0, separator)));
			}
		}
		players.forEach(this::restorePlayer);
	}

	private PvPArenaPlayerReward restorePlayer(int playerId) {
		PvPArenaPlayerReward reward = instanceReward.getPlayerReward(playerId);
		if (reward != null) {
			return reward;
		}
		instanceReward.regPlayerReward(playerId);
		reward = instanceReward.getPlayerReward(playerId);
		String points = runtimeState().get(playerKey(playerId, "points"));
		if (points != null) {
			reward.restore(Integer.parseInt(points), runtimeState().getInt(playerKey(playerId, "pvp_kills"), 0),
					runtimeState().getInt(playerKey(playerId, "monster_kills"), 0));
			long absenceStartedAt = runtimeState().getLong(playerKey(playerId, "absence_started_at"), 0);
			if (absenceStartedAt == 0 && !FINISHED.equals(runtimeState().get(STATE + "phase", PREPARING))) {
				absenceStartedAt = System.currentTimeMillis();
			}
			reward.restoreAbsence(runtimeState().getLong(playerKey(playerId, "absent_millis"), 0), absenceStartedAt);
			String rate = runtimeState().get(playerKey(playerId, "reward_rate"));
			if (rate != null) {
				reward.setRewardRate(Double.parseDouble(rate));
			}
			if (FINISHED.equals(runtimeState().get(STATE + "phase", PREPARING))) {
				reward.restoreFinalScore(runtimeState().getInt(playerKey(playerId, "time_bonus"), 0),
						runtimeState().getInt(playerKey(playerId, "participation"), 0));
			}
			persistPlayer(reward);
		}
		return reward;
	}

	private void persistPlayer(PvPArenaPlayerReward reward) {
		int playerId = reward.getOwner();
		runtimeState().put(playerKey(playerId, "points"), reward.getPoints());
		runtimeState().put(playerKey(playerId, "pvp_kills"), reward.getPvPKills());
		runtimeState().put(playerKey(playerId, "monster_kills"), reward.getMonsterKills());
		runtimeState().put(playerKey(playerId, "absent_millis"), reward.getAbsentMillis());
		runtimeState().put(playerKey(playerId, "absence_started_at"), reward.getAbsenceStartedAt());
		runtimeState().put(playerKey(playerId, "reward_rate"), reward.getRewardRate());
	}

	private static String playerKey(int playerId, String field) {
		return STATE + "player." + playerId + '.' + field;
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
}
