package com.aionemu.gameserver.instance.handlers.scripts.pvparenas;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.aionemu.gameserver.controllers.attack.AggroInfo;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.DescriptionId;
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
	private static final String STATE = "arena.";
	private static final String PREPARING = "PREPARING";
	private static final String BATTLE = "BATTLE";
	private static final String FINISHED = "FINISHED";
	private static final long EXIT_DELAY = 60_000;
	private final Set<Integer> consumedScoreNpcs = new HashSet<>();

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
		restoreGroup(group);
		boolean known = instanceReward.containPlayer(object) || runtimeState().get(playerKey(object, "points")) != null;
		PvPArenaPlayerReward playerReward = restorePlayer(object);
		if (!known) {
			playerReward.setRewardRate(player.getRates().getHarmonyRewardRate());
			playerReward.applyBoostMoraleEffect(player, instanceReward.getRebirthBuffDuration(0));
			instanceReward.setRndPosition(object);
		} else {
			playerReward.endAbsence();
			playerReward.setRewardRate(player.getRates().getHarmonyRewardRate());
			instanceReward.portToPosition(player);
		}
		persistPlayer(playerReward);
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
			restoreGroup(victimGroup);
			rank = instanceReward.getRank(victimGroup.getPoints());
			victimGroup.addPoints(-instanceReward.getDeathScore(rank));
			persistGroup(victimGroup);
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
					restoreGroup(attackerGroup);
					int rewardPoints = bonus * damager.getDamage() / victim.getAggroList().getTotalDamage();
					attackerGroup.addPoints(rewardPoints);
					persistGroup(attackerGroup);
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
		var score = DataManager.RETAIL_AI_DATA == null ? null : DataManager.RETAIL_AI_DATA.getNpcScore(npcId);
		return score == null || score.scoreApplyType() != 0 || score.equalizingScore() != 0 ? 0 : score.value();
	}

	@Override
	public boolean supportsRetailNpcScore(int npcId, int scoreApplyType) {
		return npcId == 207101 && scoreApplyType == 0;
	}

	@Override
	public synchronized boolean onRetailNpcScore(Player player, Npc npc, int scoreApplyType, int points) {
		HarmonyGroupReward group = instanceReward.getHarmonyGroupReward(player.getObjectId());
		if (!instanceReward.isStartProgress() || group == null
				|| !supportsRetailNpcScore(npc.getNpcId(), scoreApplyType)
				|| !consumedScoreNpcs.add(npc.getObjectId())) {
			return false;
		}
		restoreGroup(group);
		group.addPoints(points);
		persistGroup(group);
		sendSystemMsg(player, npc, points);
		instanceReward.sendPacket(10, player.getObjectId());
		if (instanceReward.hasCapPoints()) {
			finishBattle(true);
		}
		return true;
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
		PvPArenaPlayerReward playerReward = restorePlayer(player.getObjectId());
		if (playerReward != null) {
			playerReward.endAbsence();
			playerReward.setRewardRate(player.getRates().getHarmonyRewardRate());
			persistPlayer(playerReward);
			applyStageBuff(player);
		}
		sendEnterPacket(player);
	}

	@Override
	public void onPlayerLogOut(Player player) {
		PvPArenaPlayerReward playerReward = instanceReward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.beginAbsence();
			persistPlayer(playerReward);
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
		instanceReward.sendPacket(10, null);
		instanceReward.sendPacket(2, null);
		scheduleRound();
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
		runtimeState().put(STATE + "round", instanceReward.getRound());
		runtimeState().put(STATE + "zone." + instanceReward.getRound(), instanceReward.getZone());
		applyStageBuffs();
		instanceReward.sendPacket(10, null);
		instanceReward.sendPacket(2, null);
		scheduleZoneChange();
		if (instanceReward.getRound() == instanceReward.getScoreModifierStartStage()) {
			sendMsg(1401491, 0, false, 25, 2000);
		}
		scheduleRound();
	}
	private boolean canStart() {
		if (instanceReward.getParticipatingGroups().size() < 2) {
			// 独自一人时无法使用。 / Unavailable to use when you're alone.
			sendMsg(1403045);
			finishBattle(false);
			return false;
		}
		return true;
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
			persistPlayer(restorePlayer(player.getObjectId()));
			instanceReward.sendPacket(4, player.getObjectId());
		}
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
		cancelDeadline("prepare");
		cancelDeadline("round");
		cancelDeadline("zone");
		restoreGroups();
		removeStageBuffs();
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		long endedAt = System.currentTimeMillis();
		for (PvPArenaPlayerReward playerReward : instanceReward.getInstanceRewards()) {
			playerReward.finalizePlaytimeBonus(instanceReward.getTotalPlayMillis(), endedAt);
			runtimeState().put(playerKey(playerReward.getOwner(), "time_bonus"), playerReward.getTimeBonus());
			runtimeState().put(playerKey(playerReward.getOwner(), "participation"),
					playerReward.getParticipationPercent());
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
		for (HarmonyGroupReward group : groups) {
			runtimeState().put(groupKey(group, "final_points"), group.getPoints());
		}
		runtimeState().put(STATE + "phase", FINISHED);
		runtimeState().put(STATE + "ended_at", endedAt);
		instanceReward.sendPacket(5, null);
		for (Npc npc : instance.getNpcs()) {
			npc.getController().onDelete();
		}
		scheduleExit(endedAt + EXIT_DELAY);
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
					restoreGroup(winnerGroup);
					winnerGroup.addPvPKillToPlayer();
					persistGroup(winnerGroup);
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
		restoreGroup(group);
		int rewardetPoints = getNpcBonus(npc.getNpcId());
		int skill = instanceReward.getNpcBonusSkill(npc.getNpcId());
		if (skill != 0) {
			useSkill(npc, player, skill >> 8, skill & 0xFF);
		}
		group.addPoints(rewardetPoints);
		persistGroup(group);
		sendSystemMsg(player, npc, rewardetPoints);
		instanceReward.sendPacket(10, object);
		if (instanceReward.hasCapPoints()) {
			finishBattle(true);
		}
	}

	private void scheduleRound() {
		long deadline = System.currentTimeMillis() + instanceReward.getStageTimeSeconds() * 1000L;
		runtimeState().put(STATE + "round_deadline", deadline);
		scheduleDeadline("round", deadline, this::finishRound);
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

	private void restoreGroups() {
		instanceReward.getGroups().forEach(this::restoreGroup);
	}

	private void restoreGroup(HarmonyGroupReward group) {
		String phase = runtimeState().get(STATE + "phase", PREPARING);
		String points = runtimeState().get(groupKey(group, FINISHED.equals(phase) ? "final_points" : "points"));
		if (points != null) {
			group.restore(Integer.parseInt(points), runtimeState().getInt(groupKey(group, "pvp_kills"), 0), 0);
		}
	}

	private void persistGroup(HarmonyGroupReward group) {
		runtimeState().put(groupKey(group, "points"), group.getPoints());
		runtimeState().put(groupKey(group, "pvp_kills"), group.getPvPKills());
	}

	private static String playerKey(int playerId, String field) {
		return STATE + "player." + playerId + '.' + field;
	}

	private static String groupKey(HarmonyGroupReward group, String field) {
		return STATE + "group." + group.getOwner() + '.' + field;
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
