package com.aionemu.gameserver.instance.handlers.scripts;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.configs.main.GroupConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailInstanceData.Row;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.EvergaleCanyonReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.EvergaleCanyonPlayerReward;
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

@InstanceID(302350000)
public class EvergaleCanyonInstance extends GeneralInstanceHandler {
	private static final long EXIT_MILLIS = 60_000;
	private static final String PHASE_PREPARING = "PREPARING";
	private static final String PHASE_BATTLE = "BATTLE";
	private static final String PHASE_NO_ENEMY = "NO_ENEMY";
	private static final String PHASE_FINISHED = "FINISHED";
	private static final String STATE_PREFIX = "evergale.";

	private final Set<Integer> participants = new LinkedHashSet<>();
	private final Set<Integer> activeMembers = new LinkedHashSet<>();
	private final int[] populationThresholds = new int[5];
	private EvergaleCanyonReward reward;
	private long preparationMillis;
	private long battleMillis;
	private long noEnemyMillis;
	private long preparationStartedAt;
	private long battleStartedAt;
	private int playerKillScore;
	private int playerDeathScore;
	private int scoreLimitMaximum;
	private int scoreLimitGap;
	private int populationLevel;
	private volatile boolean destroyed;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		Row battleground = battleground();
		preparationMillis = battleground.requiredInt("wait_time") * 1_000L;
		battleMillis = battleground.requiredInt("limit_time") * 1_000L;
		noEnemyMillis = battleground.requiredInt("wait_time_after_noenemy") * 1_000L;
		playerKillScore = battleground.requiredInt("pc_kill_score");
		playerDeathScore = battleground.requiredInt("pc_die_score");
		scoreLimitMaximum = battleground.requiredInt("score_limit_max");
		scoreLimitGap = battleground.requiredInt("score_limit_gap");
		for (int i = 0; i < populationThresholds.length; i++) {
			populationThresholds[i] = battleground.requiredInt("condition_reward_cond_0" + (i + 1));
		}
		reward = new EvergaleCanyonReward(mapId, instanceId);
		restoreReward(battleground.requiredInt("base_score"));
		restoreActiveMembers();
		populationLevel = runtimeState().getInt(STATE_PREFIX + "population.level", 0);
		setPopulationVariable(populationLevel);
		preparationStartedAt = runtimeState().getLong(STATE_PREFIX + "preparation.started", 0);
		battleStartedAt = runtimeState().getLong(STATE_PREFIX + "battle.started", 0);
		restorePhase();
	}

	private Row battleground() {
		int spawnPage = instance.getDynamicInstance() == null ? 0 : instance.getDynamicInstance().getSpawnPage();
		if (spawnPage != 1 && spawnPage != 2) {
			throw new IllegalStateException("Missing Evergale battleground spawn page 1 or 2: " + spawnPage);
		}
		return DataManager.RETAIL_INSTANCE_DATA.rewards("instant_dungeon_battleground").stream()
			.filter(row -> row.requiredInt("world_id") == mapId)
			.filter(row -> row.requiredInt("spawn_page") == spawnPage)
			.findFirst().orElseThrow(() -> new IllegalStateException(
				"Missing Evergale battleground data for spawn page " + spawnPage));
	}

	private void restorePhase() {
		String phase = phase();
		switch (phase) {
			case PHASE_FINISHED -> {
				reward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
				reward.setWinnerRace(winnerRace());
				long deadline = runtimeState().getLong(STATE_PREFIX + "exit.deadline", 0);
				if (deadline == 0) {
					deadline = System.currentTimeMillis() + EXIT_MILLIS;
					runtimeState().put(STATE_PREFIX + "exit.deadline", deadline);
				}
				scheduleDeadline("exit", deadline, this::exitPlayers);
			}
			case PHASE_NO_ENEMY -> {
				reward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
				openFirstDoors();
				long deadline = runtimeState().getLong(STATE_PREFIX + "noEnemy.deadline", 0);
				if (deadline == 0) {
					deadline = System.currentTimeMillis() + noEnemyMillis;
					runtimeState().put(STATE_PREFIX + "noEnemy.deadline", deadline);
				}
				scheduleDeadline("noEnemy", deadline, this::finishBattle);
			}
			case PHASE_BATTLE -> {
				reward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
				openFirstDoors();
				scheduleDeadline("battle", restoreDeadline("battle", battleStartedAt, battleMillis), this::finishBattle);
			}
			case PHASE_PREPARING -> {
				reward.setInstanceScoreType(InstanceScoreType.PREPARING);
				if (preparationStartedAt > 0) {
					scheduleDeadline("preparation",
						restoreDeadline("preparation", preparationStartedAt, preparationMillis), this::startBattle);
				}
			}
			default -> throw new IllegalStateException("Unknown Evergale phase " + phase);
		}
	}

	private long restoreDeadline(String phase, long startedAt, long duration) {
		String key = STATE_PREFIX + phase + ".deadline";
		long deadline = runtimeState().getLong(key, 0);
		if (deadline == 0) {
			deadline = (startedAt > 0 ? startedAt : System.currentTimeMillis()) + duration;
			runtimeState().put(key, deadline);
		}
		return deadline;
	}

	@Override
	public synchronized void onEnterInstance(Player player) {
		boolean knownParticipant = reward.containPlayer(player.getObjectId());
		if ((PHASE_NO_ENEMY.equals(phase()) || PHASE_FINISHED.equals(phase())) && !knownParticipant) {
			onExitInstance(player);
			return;
		}
		EvergaleCanyonPlayerReward playerReward = registerPlayer(player);
		activeMembers.add(player.getObjectId());
		persistActiveMembers();
		updatePopulationLevel();
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
		battleStartedAt = Math.min(System.currentTimeMillis(),
			runtimeState().getLong(STATE_PREFIX + "preparation.deadline", System.currentTimeMillis()));
		long deadline = battleStartedAt + battleMillis;
		reward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		runtimeState().put(STATE_PREFIX + "battle.started", battleStartedAt);
		runtimeState().put(STATE_PREFIX + "battle.deadline", deadline);
		runtimeState().put(STATE_PREFIX + "phase", PHASE_BATTLE);
		openFirstDoors();
		sendMsgByRace(1401181, Race.PC_ALL);
		startInstancePacket();
		broadcastScoreTables();
		scheduleDeadline("battle", deadline, this::finishBattle);
		checkNoEnemy();
	}

	private synchronized void finishBattle() {
		if (destroyed || reward.isRewarded()) {
			return;
		}
		long endedAt = runtimeState().getLong(STATE_PREFIX + "battle.ended", 0);
		if (endedAt == 0) {
			long now = System.currentTimeMillis();
			String deadlineKey = PHASE_NO_ENEMY.equals(phase()) ? "noEnemy.deadline" : "battle.deadline";
			long deadline = runtimeState().getLong(STATE_PREFIX + deadlineKey, 0);
			endedAt = deadline > 0 && deadline <= now ? deadline : now;
			runtimeState().put(STATE_PREFIX + "battle.ended", endedAt);
		}
		Race winner = winnerRace();
		reward.setWinnerRace(winner);
		reward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		runtimeState().put(STATE_PREFIX + "winner", winner.getRaceId());
		settlePlayers(endedAt, winner);
		broadcastScoreTables();
		long exitDeadline = runtimeState().getLong(STATE_PREFIX + "exit.deadline", 0);
		if (exitDeadline == 0) {
			exitDeadline = endedAt + EXIT_MILLIS;
			runtimeState().put(STATE_PREFIX + "exit.deadline", exitDeadline);
		}
		runtimeState().put(STATE_PREFIX + "phase", PHASE_FINISHED);
		scheduleDeadline("exit", exitDeadline, this::exitPlayers);
	}

	private void settlePlayers(long endedAt, Race winner) {
		int elyosPoints = getPointsByRace(Race.ELYOS).intValue();
		int asmodianPoints = getPointsByRace(Race.ASMODIANS).intValue();
		int populationThreshold = populationThresholdForLevel(populationLevel, populationThresholds);
		for (EvergaleCanyonPlayerReward playerReward : List.copyOf(reward.getInstanceRewards())) {
			int teamScore = playerReward.getRace() == Race.ELYOS ? elyosPoints : asmodianPoints;
			int opposingScore = playerReward.getRace() == Race.ELYOS ? asmodianPoints : elyosPoints;
			BattleResult result = winner == Race.PC_ALL ? BattleResult.DRAW
				: playerReward.getRace() == winner ? BattleResult.WIN : BattleResult.LOSE;
			double bonusRate = InstanceSettlementService.battlegroundBonusRate(
				playerReward.calculateParticipation(battleStartedAt, endedAt), teamScore, opposingScore);
			RewardPlan base = InstanceSettlementService.battlegroundPlan(instance, result, 0, teamScore, 0,
				populationThreshold);
			RewardPlan total = InstanceSettlementService.battlegroundPlan(instance, result, bonusRate, teamScore, 0,
				populationThreshold);
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
	}

	private Race winnerRace() {
		int storedWinner = runtimeState().getInt(STATE_PREFIX + "winner", -1);
		if (storedWinner == Race.ELYOS.getRaceId()) {
			return Race.ELYOS;
		}
		if (storedWinner == Race.ASMODIANS.getRaceId()) {
			return Race.ASMODIANS;
		}
		int comparison = getPointsByRace(Race.ELYOS).compareTo(getPointsByRace(Race.ASMODIANS));
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
				PacketSendUtility.sendPacket(opponent,
					new SM_INSTANCE_SCORE(11, getTime(), reward, player.getObjectId()));
				PacketSendUtility.sendPacket(player,
					new SM_INSTANCE_SCORE(11, getTime(), reward, opponent.getObjectId()));
				PacketSendUtility.sendPacket(opponent,
					new SM_INSTANCE_SCORE(3, getTime(), reward, player.getObjectId()));
			} else {
				PacketSendUtility.sendPacket(opponent,
					new SM_INSTANCE_SCORE(11, getTime(), reward, opponent.getObjectId()));
				if (player.getObjectId() != opponent.getObjectId()) {
					PacketSendUtility.sendPacket(opponent,
						new SM_INSTANCE_SCORE(3, getTime(), reward, player.getObjectId(), 20, 0));
				}
			}
		});
		broadcastScoreTables();
		PacketSendUtility.sendPacket(player,
			new SM_INSTANCE_SCORE(4, getTime(), reward, player.getObjectId(), 20, 0));
	}

	private void startInstancePacket() {
		instance.doOnAllPlayers(player -> {
			PacketSendUtility.sendPacket(player,
				new SM_INSTANCE_SCORE(7, getTime(), reward, instance.getPlayersInside(), true));
			PacketSendUtility.sendPacket(player,
				new SM_INSTANCE_SCORE(3, getTime(), reward, player.getObjectId(), 0, 0));
			PacketSendUtility.sendPacket(player,
				new SM_INSTANCE_SCORE(11, getTime(), reward, player.getObjectId()));
		});
	}

	private void sendPacket(boolean objects) {
		int type = objects ? 6 : 7;
		instance.doOnAllPlayers(player -> PacketSendUtility.sendPacket(player,
			new SM_INSTANCE_SCORE(type, getTime(), reward, instance.getPlayersInside(), true)));
	}

	private void broadcastScoreTables() {
		sendPacket(true);
		sendPacket(false);
	}

	private void broadcastScore(int objectId) {
		instance.doOnAllPlayers(player -> PacketSendUtility.sendPacket(player,
			new SM_INSTANCE_SCORE(11, getTime(), reward, objectId)));
	}

	private int getTime() {
		long now = System.currentTimeMillis();
		String deadline = switch (phase()) {
			case PHASE_PREPARING -> "preparation.deadline";
			case PHASE_BATTLE -> "battle.deadline";
			case PHASE_NO_ENEMY -> "noEnemy.deadline";
			default -> null;
		};
		return deadline == null ? 0 : (int) Math.max(0, runtimeState().getLong(STATE_PREFIX + deadline, now) - now);
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
		EvergaleCanyonPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.endBoostMoraleEffect(player);
			playerReward.applyBoostMoraleEffect(player);
		}
		if (isBattlePhase() && lastAttacker instanceof Player attacker && attacker.getRace() != player.getRace()) {
			updateScore(attacker, player, playerKillScore, true);
			addTeamScore(player.getRace(), -playerDeathScore);
			broadcastScore(player.getObjectId());
			if (scoreLimitReached()) {
				finishBattle();
			}
		}
		return true;
	}

	private MutableInt getPointsByRace(Race race) {
		return reward.getPointsByRace(race);
	}

	private void addTeamScore(Race race, int points) {
		reward.addPointsByRace(race, points);
		runtimeState().put(STATE_PREFIX + "score." + race.name(), getPointsByRace(race).intValue());
	}

	private void updateScore(Player player, Creature target, int points, boolean pvpKill) {
		if (!isBattlePhase() || points == 0) {
			return;
		}
		addTeamScore(player.getRace(), points);
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
			EvergaleCanyonPlayerReward recipientReward = registerPlayer(recipient);
			recipientReward.addPoints(points / recipients.size());
			persistPlayer(recipientReward);
			if (target instanceof Npc npc) {
				PacketSendUtility.sendPacket(recipient, new SM_SYSTEM_MESSAGE(1400237,
					new DescriptionId(npc.getObjectTemplate().getNameId() * 2 + 1), points));
			} else if (target instanceof Player) {
				PacketSendUtility.sendPacket(recipient, new SM_SYSTEM_MESSAGE(1400237, target.getName(), points));
			}
		}
		if (pvpKill && points > 0) {
			reward.addPvpKillsByRace(player.getRace(), 1);
			EvergaleCanyonPlayerReward killerReward = registerPlayer(player);
			killerReward.addPvPKillToPlayer();
			persistPlayer(killerReward);
			runtimeState().put(STATE_PREFIX + "pvp." + player.getRace().name(),
				reward.getPvpKillsByRace(player.getRace()).intValue());
		}
		broadcastScore(player.getObjectId());
	}

	private boolean scoreLimitReached() {
		int elyos = getPointsByRace(Race.ELYOS).intValue();
		int asmodians = getPointsByRace(Race.ASMODIANS).intValue();
		return scoreLimitMaximum > 0 && Math.max(elyos, asmodians) >= scoreLimitMaximum
			|| scoreLimitGap > 0 && Math.abs(elyos - asmodians) >= scoreLimitGap;
	}

	@Override
	public boolean supportsRetailNpcScore(int npcId, int scoreApplyType) {
		var score = DataManager.RETAIL_AI_DATA.getNpcScore(npcId);
		return score != null && score.scoreApplyType() == scoreApplyType
			&& (scoreApplyType == 1 || scoreApplyType == 2) && score.equalizingScore() == 0;
	}

	@Override
	public boolean onRetailNpcScore(Player player, Npc npc, int scoreApplyType, int points) {
		if (!supportsRetailNpcScore(npc.getNpcId(), scoreApplyType)) {
			return false;
		}
		consumeNpcScore(npc, scoreApplyType, points);
		return true;
	}

	@Override
	public void onDie(Npc npc) {
		var score = DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId());
		if (score != null && supportsRetailNpcScore(npc.getNpcId(), score.scoreApplyType())) {
			consumeNpcScore(npc, score.scoreApplyType(), score.value());
		}
	}

	private synchronized boolean consumeNpcScore(Npc npc, int scoreApplyType, int points) {
		if (!isBattlePhase() || points == 0) {
			return false;
		}
		String stableKey = npc.getSpawn() == null ? null : npc.getSpawn().getStableKey();
		String eventKey = scoreEventKey(stableKey, npc.getObjectId());
		if (runtimeState().getBoolean(eventKey, false)) {
			return true;
		}
		runtimeState().put(eventKey, true);
		addTeamScore(scoreApplyType == 1 ? Race.ELYOS : Race.ASMODIANS, points);
		broadcastScoreTables();
		if (scoreLimitReached()) {
			finishBattle();
		}
		return true;
	}

	static String scoreEventKey(String stableKey, int objectId) {
		return STATE_PREFIX + "score.event."
			+ (stableKey == null || stableKey.isBlank() ? "object." + objectId : stableKey);
	}

	private synchronized void checkNoEnemy() {
		if (!isBattlePhase()) {
			return;
		}
		int elyos = activeMemberCount(Race.ELYOS);
		int asmodians = activeMemberCount(Race.ASMODIANS);
		Race winner = noEnemyWinner(elyos, asmodians);
		if (winner == Race.PC_ALL) {
			return;
		}
		Race emptyRace = winner == Race.ELYOS ? Race.ASMODIANS : Race.ELYOS;
		restoreTeamScore(emptyRace, 0);
		MutableInt emptyKills = reward.getPvpKillsByRace(emptyRace);
		emptyKills.add(-emptyKills.intValue());
		runtimeState().put(STATE_PREFIX + "pvp." + emptyRace.name(), 0);
		long deadline = System.currentTimeMillis() + noEnemyMillis;
		runtimeState().put(STATE_PREFIX + "winner", winner.getRaceId());
		runtimeState().put(STATE_PREFIX + "phase", PHASE_NO_ENEMY);
		runtimeState().put(STATE_PREFIX + "noEnemy.deadline", deadline);
		cancelDeadline("battle");
		broadcastScoreTables();
		scheduleDeadline("noEnemy", deadline, this::finishBattle);
	}

	static Race noEnemyWinner(int elyosMembers, int asmodianMembers) {
		if (elyosMembers == 0 && asmodianMembers > 0) {
			return Race.ASMODIANS;
		}
		if (asmodianMembers == 0 && elyosMembers > 0) {
			return Race.ELYOS;
		}
		return Race.PC_ALL;
	}

	private void updatePopulationLevel() {
		int memberCount = Math.max(activeMemberCount(Race.ELYOS), activeMemberCount(Race.ASMODIANS));
		int nextLevel = populationLevelForCount(memberCount, populationThresholds);
		if (nextLevel <= populationLevel) {
			return;
		}
		populationLevel = nextLevel;
		runtimeState().put(STATE_PREFIX + "population.level", populationLevel);
		setPopulationVariable(populationLevel);
	}

	static int populationLevelForCount(int memberCount, int... thresholds) {
		int level = 0;
		while (level < thresholds.length && memberCount >= thresholds[level]) {
			level++;
		}
		return level;
	}

	static int populationThresholdForLevel(int level, int... thresholds) {
		return level <= 0 ? 0 : thresholds[Math.min(level, thresholds.length) - 1];
	}

	private int activeMemberCount(Race race) {
		int count = 0;
		for (int playerId : activeMembers) {
			EvergaleCanyonPlayerReward playerReward = reward.getPlayerReward(playerId);
			if (playerReward != null && playerReward.getRace() == race) {
				count++;
			}
		}
		return count;
	}

	private void setPopulationVariable(int level) {
		if (!RetailConditionSpawnEngine.setVariable(instance, "people_expand_con", level, 0)) {
			throw new IllegalStateException("Missing Evergale condition variable people_expand_con");
		}
	}

	private EvergaleCanyonPlayerReward registerPlayer(Player player) {
		EvergaleCanyonPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward == null) {
			playerReward = new EvergaleCanyonPlayerReward(player.getObjectId(), reward.getBuffId(), player.getRace());
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
			EvergaleCanyonPlayerReward playerReward = new EvergaleCanyonPlayerReward(playerId, reward.getBuffId(), race,
				runtimeState().getLong(playerKey(playerId, "joined"), 0));
			playerReward.addPoints(runtimeState().getInt(playerKey(playerId, "points"), 0));
			for (int kills = runtimeState().getInt(playerKey(playerId, "kills"), 0); kills > 0; kills--) {
				playerReward.addPvPKillToPlayer();
			}
			playerReward.restoreActivity(runtimeState().getLong(playerKey(playerId, "logout"), 0),
				runtimeState().getLong(playerKey(playerId, "offline"), 0));
			reward.addPlayerReward(playerReward);
		}
	}

	private void restoreActiveMembers() {
		String members = runtimeState().get(STATE_PREFIX + "members", "");
		if (!members.isBlank()) {
			for (String value : members.split(",")) {
				activeMembers.add(Integer.parseInt(value));
			}
		}
	}

	private void restoreTeamScore(Race race, int points) {
		reward.addPointsByRace(race, points - reward.getPointsByRace(race).intValue());
		runtimeState().put(STATE_PREFIX + "score." + race.name(), points);
	}

	private void persistPlayer(EvergaleCanyonPlayerReward playerReward) {
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

	private void persistActiveMembers() {
		runtimeState().put(STATE_PREFIX + "members", activeMembers.stream().map(String::valueOf)
			.collect(java.util.stream.Collectors.joining(",")));
	}

	private String playerKey(int playerId, String field) {
		return STATE_PREFIX + "player." + playerId + '.' + field;
	}

	private String phase() {
		return runtimeState().get(STATE_PREFIX + "phase", PHASE_PREPARING);
	}

	private boolean isBattlePhase() {
		return PHASE_BATTLE.equals(phase()) && reward.isStartProgress();
	}

	@Override
	public synchronized void onLeaveInstance(Player player) {
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
		EvergaleCanyonPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.updateLogOutTime();
			playerReward.endBoostMoraleEffect(player);
			persistPlayer(playerReward);
		}
		activeMembers.remove(player.getObjectId());
		persistActiveMembers();
		checkNoEnemy();
	}

	@Override
	public void onPlayerLogOut(Player player) {
		EvergaleCanyonPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.updateLogOutTime();
			persistPlayer(playerReward);
		}
	}

	@Override
	public void onPlayerLogin(Player player) {
		EvergaleCanyonPlayerReward playerReward = registerPlayer(player);
		playerReward.updateBonusTime();
		persistPlayer(playerReward);
		PacketSendUtility.sendPacket(player,
			new SM_INSTANCE_SCORE(10, getTime(), reward, player.getObjectId()));
	}

	private void sendMsgByRace(int messageId, Race race) {
		instance.doOnAllPlayers(player -> {
			if (race == Race.PC_ALL || player.getRace() == race) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(messageId));
			}
		});
	}

	private void openFirstDoors() {
		setDoorState(352, true);
		setDoorState(507, true);
	}

	@Override
	public void onInstanceDestroy() {
		destroyed = true;
		cancelDeadline("preparation");
		cancelDeadline("battle");
		cancelDeadline("noEnemy");
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
