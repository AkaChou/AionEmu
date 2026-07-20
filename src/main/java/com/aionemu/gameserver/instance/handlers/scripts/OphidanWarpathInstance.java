package com.aionemu.gameserver.instance.handlers.scripts;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;

import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.EngulfedOphidanBridgeReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.EngulfedOphidanBridgePlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.BattleResult;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(301670000)
public class OphidanWarpathInstance extends GeneralInstanceHandler {
	private static final long EXIT_MILLIS = 60_000;
	private static final String PHASE_PREPARING = "PREPARING";
	private static final String PHASE_BATTLE = "BATTLE";
	private static final String PHASE_FINISHED = "FINISHED";
	private static final String STATE_PREFIX = "ophidan_warpath.";

	private final Set<Integer> participants = new LinkedHashSet<>();
	private EngulfedOphidanBridgeReward reward;
	private long preparationMillis;
	private long battleMillis;
	private long preparationStartedAt;
	private long battleStartedAt;
	private int playerKillScore;
	private int playerDeathScore;
	private int scoreLimitMaximum;
	private int scoreLimitGap;
	private volatile boolean destroyed;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		Row battleground = battleground();
		preparationMillis = battleground.requiredInt("wait_time") * 1_000L;
		battleMillis = battleground.requiredInt("limit_time") * 1_000L;
		playerKillScore = battleground.requiredInt("pc_kill_score");
		playerDeathScore = battleground.requiredInt("pc_die_score");
		scoreLimitMaximum = battleground.requiredInt("score_limit_max");
		scoreLimitGap = battleground.requiredInt("score_limit_gap");
		reward = new EngulfedOphidanBridgeReward(mapId, instanceId, instance);
		restoreReward(battleground.requiredInt("base_score"));
		preparationStartedAt = runtimeState().getLong(STATE_PREFIX + "preparation.started", 0);
		battleStartedAt = runtimeState().getLong(STATE_PREFIX + "battle.started", 0);
		restorePhase();
	}

	private Row battleground() {
		return DataManager.RETAIL_INSTANCE_DATA.rewards("instant_dungeon_battleground").stream()
			.filter(row -> row.requiredInt("world_id") == mapId).findFirst()
			.orElseThrow(() -> new IllegalStateException("Missing Ophidan Warpath battleground data"));
	}

	private void restorePhase() {
		String phase = runtimeState().get(STATE_PREFIX + "phase", PHASE_PREPARING);
		if (PHASE_FINISHED.equals(phase)) {
			reward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
			long deadline = deadline(STATE_PREFIX + "exit.deadline", System.currentTimeMillis() + EXIT_MILLIS);
			scheduleDeadline("exit", deadline, this::exitPlayers);
		} else if (PHASE_BATTLE.equals(phase)) {
			reward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
			openFirstDoors();
			long deadline = deadline(STATE_PREFIX + "battle.deadline",
				Math.max(System.currentTimeMillis(), battleStartedAt + battleMillis));
			scheduleDeadline("battle", deadline, this::finishBattle);
		} else {
			reward.setInstanceScoreType(InstanceScoreType.PREPARING);
			if (preparationStartedAt > 0) {
				long deadline = deadline(STATE_PREFIX + "preparation.deadline",
					Math.max(System.currentTimeMillis(), preparationStartedAt + preparationMillis));
				scheduleDeadline("preparation", deadline, this::startBattle);
			}
		}
	}

	private long deadline(String key, long fallback) {
		long deadline = runtimeState().getLong(key, 0);
		if (deadline == 0) {
			deadline = fallback;
			runtimeState().put(key, deadline);
		}
		return deadline;
	}

	@Override
	public void onEnterInstance(Player player) {
		EngulfedOphidanBridgePlayerReward playerReward = registerPlayer(player);
		playerReward.updateBonusTime();
		persistPlayer(playerReward);
		startPreparation();
		sendEnterPacket(player);
	}

	private synchronized void startPreparation() {
		if (preparationStartedAt != 0 || destroyed || reward.isRewarded()) {
			return;
		}
		preparationStartedAt = System.currentTimeMillis();
		long deadline = preparationStartedAt + preparationMillis;
		runtimeState().put(STATE_PREFIX + "preparation.started", preparationStartedAt);
		runtimeState().put(STATE_PREFIX + "preparation.deadline", deadline);
		runtimeState().put(STATE_PREFIX + "phase", PHASE_PREPARING);
		scheduleDeadline("preparation", deadline, this::startBattle);
	}

	private synchronized void startBattle() {
		if (battleStartedAt != 0 || destroyed || reward.isRewarded()) {
			return;
		}
		battleStartedAt = System.currentTimeMillis();
		long deadline = battleStartedAt + battleMillis;
		reward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		runtimeState().put(STATE_PREFIX + "battle.started", battleStartedAt);
		runtimeState().put(STATE_PREFIX + "battle.deadline", deadline);
		runtimeState().put(STATE_PREFIX + "phase", PHASE_BATTLE);
		openFirstDoors();
		startInstancePacket();
		broadcastTables();
		scheduleDeadline("battle", deadline, this::finishBattle);
	}

	private synchronized void finishBattle() {
		if (destroyed || reward.isRewarded()) {
			return;
		}
		long endedAt = runtimeState().getLong(STATE_PREFIX + "battle.ended", 0);
		if (endedAt == 0) {
			endedAt = System.currentTimeMillis();
			runtimeState().put(STATE_PREFIX + "battle.ended", endedAt);
		}
		reward.setWinnerRace(winnerRace());
		reward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		int elyosPoints = points(Race.ELYOS).intValue();
		int asmodianPoints = points(Race.ASMODIANS).intValue();
		int minimumTeamSize = (int) Math.min(
			reward.getInstanceRewards().stream().filter(player -> player.getRace() == Race.ELYOS).count(),
			reward.getInstanceRewards().stream().filter(player -> player.getRace() == Race.ASMODIANS).count());
		for (EngulfedOphidanBridgePlayerReward playerReward : List.copyOf(reward.getInstanceRewards())) {
			int teamScore = playerReward.getRace() == Race.ELYOS ? elyosPoints : asmodianPoints;
			int opposingScore = playerReward.getRace() == Race.ELYOS ? asmodianPoints : elyosPoints;
			BattleResult result = InstanceSettlementService.battlegroundResult(teamScore, opposingScore);
			double bonusRate = InstanceSettlementService.battlegroundBonusRate(
				participation(playerReward.getOwner(), endedAt), teamScore, opposingScore);
			RewardPlan base = InstanceSettlementService.battlegroundPlan(instance, result, 0, teamScore, 0,
				minimumTeamSize);
			RewardPlan total = InstanceSettlementService.battlegroundPlan(instance, result, bonusRate, teamScore, 0,
				minimumTeamSize);
			InstanceSettlementService.applyBattlegroundDisplay(playerReward, base, total);
			Player player = instance.getPlayer(playerReward.getOwner());
			if (player == null) {
				InstanceSettlementService.queueBattleground(instance, playerReward.getOwner(), result, total);
				continue;
			}
			if (PlayerActions.isAlreadyDead(player)) {
				PlayerReviveService.duelRevive(player);
			}
			InstanceSettlementService.settleBattleground(instance, player, result, total);
			PacketSendUtility.sendPacket(player,
				new SM_INSTANCE_SCORE(5, getTime(), reward, player.getObjectId()));
		}
		broadcastTables();
		long exitDeadline = deadline(STATE_PREFIX + "exit.deadline", endedAt + EXIT_MILLIS);
		runtimeState().put(STATE_PREFIX + "phase", PHASE_FINISHED);
		scheduleDeadline("exit", exitDeadline, this::exitPlayers);
	}

	private double participation(int playerId, long endedAt) {
		long duration = Math.max(1, endedAt - battleStartedAt);
		long joinedAt = runtimeState().getLong(playerKey(playerId, "joined"), battleStartedAt);
		long offlineMillis = runtimeState().getLong(playerKey(playerId, "offline"), 0);
		long logoutAt = runtimeState().getLong(playerKey(playerId, "logout"), 0);
		long inactive = Math.max(0, Math.max(joinedAt, battleStartedAt) - battleStartedAt) + offlineMillis;
		if (logoutAt > 0) {
			inactive += Math.max(0, endedAt - logoutAt);
		}
		return Math.max(0, Math.min(1, (double) (duration - Math.min(duration, inactive)) / duration));
	}

	private Race winnerRace() {
		int comparison = points(Race.ELYOS).compareTo(points(Race.ASMODIANS));
		return comparison > 0 ? Race.ELYOS : comparison < 0 ? Race.ASMODIANS : Race.PC_ALL;
	}

	private void exitPlayers() {
		if (destroyed) {
			return;
		}
		for (Player player : instance.getPlayersInside()) {
			onExitInstance(player);
		}
		GameCoreGameplayServices.autoGroupService().unRegisterInstance(instance);
	}

	private void sendEnterPacket(Player player) {
		instance.doOnAllPlayers(opponent -> {
			if (player.getRace() != opponent.getRace()) {
				PacketSendUtility.sendPacket(opponent, new SM_INSTANCE_SCORE(11, getTime(), reward, player.getObjectId()));
				PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(11, getTime(), reward, opponent.getObjectId()));
			} else if (player.getObjectId() != opponent.getObjectId()) {
				PacketSendUtility.sendPacket(opponent,
					new SM_INSTANCE_SCORE(3, getTime(), reward, player.getObjectId(), 20, 0));
			}
		});
		broadcastTables();
		PacketSendUtility.sendPacket(player,
			new SM_INSTANCE_SCORE(4, getTime(), reward, player.getObjectId(), 20, 0));
	}

	private void startInstancePacket() {
		instance.doOnAllPlayers(player -> {
			PacketSendUtility.sendPacket(player,
				new SM_INSTANCE_SCORE(7, getTime(), reward, instance.getPlayersInside(), true));
			PacketSendUtility.sendPacket(player,
				new SM_INSTANCE_SCORE(3, getTime(), reward, player.getObjectId(), 0, 0));
		});
	}

	private void broadcastTables() {
		instance.doOnAllPlayers(player -> {
			PacketSendUtility.sendPacket(player,
				new SM_INSTANCE_SCORE(6, getTime(), reward, instance.getPlayersInside(), true));
			PacketSendUtility.sendPacket(player,
				new SM_INSTANCE_SCORE(7, getTime(), reward, instance.getPlayersInside(), true));
		});
	}

	private void broadcastScore(int objectId) {
		instance.doOnAllPlayers(player -> PacketSendUtility.sendPacket(player,
			new SM_INSTANCE_SCORE(11, getTime(), reward, objectId)));
	}

	private int getTime() {
		long now = System.currentTimeMillis();
		if (reward.isPreparing()) {
			return (int) Math.max(0, runtimeState().getLong(STATE_PREFIX + "preparation.deadline", now) - now);
		}
		if (reward.isStartProgress()) {
			return (int) Math.max(0, runtimeState().getLong(STATE_PREFIX + "battle.deadline", now) - now);
		}
		return 0;
	}

	@Override
	public boolean onReviveEvent(Player player) {
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		PlayerReviveService.revive(player, 100, 100, false, 0);
		player.getGameStats().updateStatsAndSpeedVisually();
		reward.portToPosition(player);
		return true;
	}

	@Override
	public boolean onDie(Player player, Creature lastAttacker) {
		EngulfedOphidanBridgePlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.endBoostMoraleEffect(player);
			playerReward.applyBoostMoraleEffect(player);
		}
		if (reward.isStartProgress() && lastAttacker instanceof Player attacker
				&& attacker.getRace() != player.getRace()) {
			updateScore(attacker, player, playerKillScore, true);
			addTeamScore(player.getRace(), -playerDeathScore);
			broadcastScore(player.getObjectId());
			finishIfScoreLimitReached();
		}
		return true;
	}

	private MutableInt points(Race race) {
		return reward.getPointsByRace(race);
	}

	private void addTeamScore(Race race, int value) {
		reward.addPointsByRace(race, value);
		runtimeState().put(STATE_PREFIX + "score." + race.name(), points(race).intValue());
	}

	private void updateScore(Player player, Creature target, int value, boolean pvpKill) {
		if (!reward.isStartProgress() || value == 0) {
			return;
		}
		addTeamScore(player.getRace(), value);
		List<Player> recipients = new ArrayList<>();
		if (target != null && player.isInGroup2()) {
			for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
				if (!member.getLifeStats().isAlreadyDead()
						&& MathUtil.isIn3dRange(member, target, GroupConfig.GROUP_MAX_DISTANCE)) {
					recipients.add(member);
				}
			}
		}
		if (recipients.isEmpty()) {
			recipients.add(player);
		}
		for (Player recipient : recipients) {
			EngulfedOphidanBridgePlayerReward recipientReward = registerPlayer(recipient);
			recipientReward.addPoints(value / recipients.size());
			persistPlayer(recipientReward);
			if (target instanceof Npc npc) {
				PacketSendUtility.sendPacket(recipient, new SM_SYSTEM_MESSAGE(1400237,
					new DescriptionId(npc.getObjectTemplate().getNameId() * 2 + 1), value));
			} else if (target instanceof Player) {
				PacketSendUtility.sendPacket(recipient, new SM_SYSTEM_MESSAGE(1400237, target.getName(), value));
			}
		}
		if (pvpKill && value > 0) {
			reward.addPvpKillsByRace(player.getRace(), 1);
			EngulfedOphidanBridgePlayerReward killerReward = registerPlayer(player);
			killerReward.addPvPKillToPlayer();
			persistPlayer(killerReward);
			runtimeState().put(STATE_PREFIX + "pvp." + player.getRace().name(),
				reward.getPvpKillsByRace(player.getRace()).intValue());
		}
		broadcastScore(player.getObjectId());
	}

	private void finishIfScoreLimitReached() {
		int elyos = points(Race.ELYOS).intValue();
		int asmodians = points(Race.ASMODIANS).intValue();
		if (scoreLimitMaximum > 0 && Math.max(elyos, asmodians) >= scoreLimitMaximum
				|| scoreLimitGap > 0 && Math.abs(elyos - asmodians) >= scoreLimitGap) {
			finishBattle();
		}
	}

	@Override
	public boolean supportsRetailNpcScore(int npcId, int scoreApplyType) {
		return scoreApplyType >= 0 && scoreApplyType <= 2
			&& (npcId == 833935 || npcId == 833936 || npcId == 833961);
	}

	@Override
	public boolean onRetailNpcScore(Player player, Npc npc, int scoreApplyType, int value) {
		if (!supportsRetailNpcScore(npc.getNpcId(), scoreApplyType)) {
			return false;
		}
		applyNpcScore(player, npc, scoreApplyType, value);
		finishIfScoreLimitReached();
		return true;
	}

	@Override
	public void onDie(Npc npc) {
		Player player = npc.getAggroList().getMostPlayerDamage();
		var score = DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId());
		if (player == null || score == null || score.equalizingScore() != 0) {
			return;
		}
		applyNpcScore(player, npc, score.scoreApplyType(), score.value());
	}

	private void applyNpcScore(Player player, Npc npc, int applyType, int value) {
		switch (applyType) {
			case 0 -> updateScore(player, npc, value, false);
			case 1 -> {
				addTeamScore(Race.ELYOS, value);
				broadcastScore(player.getObjectId());
			}
			case 2 -> {
				addTeamScore(Race.ASMODIANS, value);
				broadcastScore(player.getObjectId());
			}
		}
		finishIfScoreLimitReached();
	}

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 701947, 701949 -> GameEngineServices.skillEngine().getSkill(npc, 21065, 1, player)
				.useNoAnimationSkill();
			case 701948, 701950 -> GameEngineServices.skillEngine().getSkill(npc, 21066, 1, player)
				.useNoAnimationSkill();
		}
		var score = DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId());
		if (score != null && !supportsRetailNpcScore(npc.getNpcId(), score.scoreApplyType())
				&& score.equalizingScore() == 0) {
			applyNpcScore(player, npc, score.scoreApplyType(), score.value());
		}
	}

	private EngulfedOphidanBridgePlayerReward registerPlayer(Player player) {
		EngulfedOphidanBridgePlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward == null) {
			playerReward = new EngulfedOphidanBridgePlayerReward(player.getObjectId(), reward.getBuffId(), player.getRace());
			reward.addPlayerReward(playerReward);
			runtimeState().put(playerKey(player.getObjectId(), "joined"), System.currentTimeMillis());
		}
		participants.add(player.getObjectId());
		persistParticipants();
		return playerReward;
	}

	private void restoreReward(int baseScore) {
		restoreTeamScore(Race.ELYOS, runtimeState().getInt(STATE_PREFIX + "score.ELYOS", baseScore));
		restoreTeamScore(Race.ASMODIANS, runtimeState().getInt(STATE_PREFIX + "score.ASMODIANS", baseScore));
		reward.addPvpKillsByRace(Race.ELYOS, runtimeState().getInt(STATE_PREFIX + "pvp.ELYOS", 0));
		reward.addPvpKillsByRace(Race.ASMODIANS, runtimeState().getInt(STATE_PREFIX + "pvp.ASMODIANS", 0));
		String players = runtimeState().get(STATE_PREFIX + "players", "");
		if (players.isBlank()) {
			return;
		}
		for (String value : players.split(",")) {
			int playerId = Integer.parseInt(value);
			participants.add(playerId);
			Race race = runtimeState().getInt(playerKey(playerId, "race"), Race.ELYOS.getRaceId())
				== Race.ELYOS.getRaceId() ? Race.ELYOS : Race.ASMODIANS;
			EngulfedOphidanBridgePlayerReward playerReward = new EngulfedOphidanBridgePlayerReward(playerId,
				reward.getBuffId(), race, runtimeState().getLong(playerKey(playerId, "joined"), 0));
			playerReward.addPoints(runtimeState().getInt(playerKey(playerId, "points"), 0));
			for (int kills = runtimeState().getInt(playerKey(playerId, "kills"), 0); kills > 0; kills--) {
				playerReward.addPvPKillToPlayer();
			}
			playerReward.restoreActivity(runtimeState().getLong(playerKey(playerId, "logout"), 0),
				runtimeState().getLong(playerKey(playerId, "offline"), 0));
			reward.addPlayerReward(playerReward);
		}
	}

	private void restoreTeamScore(Race race, int value) {
		reward.addPointsByRace(race, value - reward.getPointsByRace(race).intValue());
	}

	private void persistPlayer(EngulfedOphidanBridgePlayerReward playerReward) {
		int playerId = playerReward.getOwner();
		participants.add(playerId);
		runtimeState().put(playerKey(playerId, "race"), playerReward.getRace().getRaceId());
		runtimeState().put(playerKey(playerId, "joined"),
			runtimeState().getLong(playerKey(playerId, "joined"), playerReward.getJoinedAt()));
		runtimeState().put(playerKey(playerId, "points"), playerReward.getPoints());
		runtimeState().put(playerKey(playerId, "kills"), playerReward.getPvPKills());
		runtimeState().put(playerKey(playerId, "logout"), playerReward.getLogoutAt());
		runtimeState().put(playerKey(playerId, "offline"), playerReward.getOfflineMillis());
		persistParticipants();
	}

	private void persistParticipants() {
		runtimeState().put(STATE_PREFIX + "players", participants.stream().map(String::valueOf)
			.collect(java.util.stream.Collectors.joining(",")));
	}

	private String playerKey(int playerId, String field) {
		return STATE_PREFIX + "player." + playerId + '.' + field;
	}

	@Override
	public void onLeaveInstance(Player player) {
		EngulfedOphidanBridgePlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.updateLogOutTime();
			playerReward.endBoostMoraleEffect(player);
			persistPlayer(playerReward);
		}
	}

	@Override
	public void onPlayerLogOut(Player player) {
		EngulfedOphidanBridgePlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.updateLogOutTime();
			persistPlayer(playerReward);
		}
	}

	@Override
	public void onPlayerLogin(Player player) {
		EngulfedOphidanBridgePlayerReward playerReward = registerPlayer(player);
		playerReward.updateBonusTime();
		persistPlayer(playerReward);
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(10, getTime(), reward, player.getObjectId()));
	}

	private void openFirstDoors() {
		setDoorState(176, true);
		setDoorState(177, true);
	}

	@Override
	public void onInstanceDestroy() {
		destroyed = true;
		cancelDeadline("preparation");
		cancelDeadline("battle");
		cancelDeadline("exit");
		reward.clear();
	}

	@Override
	public InstanceReward<?> getInstanceReward() {
		return reward;
	}

	@Override
	public void onExitInstance(Player player) {
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}
}
