package com.aionemu.gameserver.instance.handlers.scripts.idgelDome;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableInt;

import com.aionemu.gameserver.configs.main.GroupConfig;
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
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.LandMarkReward;
import com.aionemu.gameserver.model.instance.playerreward.LandMarkPlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
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

@InstanceID(301680000)
public class IdgelDomeLandmarkInstance extends GeneralInstanceHandler {
	private static final long PREPARATION_MILLIS = 120_000;
	private static final long BATTLE_MILLIS = 1_200_000;
	private static final long SUPPLY_MILLIS = 300_000;
	private static final long EXIT_MILLIS = 60_000;
	private static final String PHASE_PREPARING = "PREPARING";
	private static final String PHASE_BATTLE = "BATTLE";
	private static final String PHASE_FINISHED = "FINISHED";

	private final Set<Integer> participants = new LinkedHashSet<>();
	private LandMarkReward reward;
	private long preparationStartedAt;
	private long battleStartedAt;
	private boolean elyosTargetCompleted;
	private boolean asmodianTargetCompleted;
	private volatile boolean destroyed;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		reward = new LandMarkReward(mapId, instanceId, instance);
		restoreReward();
		String phase = runtimeState().get("landmark.phase", PHASE_PREPARING);
		preparationStartedAt = runtimeState().getLong("landmark.preparation.started", 0);
		battleStartedAt = runtimeState().getLong("landmark.battle.started", 0);
		if (PHASE_FINISHED.equals(phase)) {
			reward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
			restoreWinner();
			scheduleDeadline("exit", runtimeState().getLong("landmark.exit.deadline", 0), this::exitPlayers);
			return;
		}
		if (PHASE_BATTLE.equals(phase)) {
			reward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
			setDoorState(180, true);
			setDoorState(181, true);
			scheduleDeadline("battle", runtimeState().getLong("landmark.battle.deadline", 0), this::finishBattle);
		} else {
			reward.setInstanceScoreType(InstanceScoreType.PREPARING);
			startPreparation();
		}
		scheduleSupply();
	}

	private synchronized void startPreparation() {
		if (preparationStartedAt == 0) {
			preparationStartedAt = System.currentTimeMillis();
			runtimeState().put("landmark.phase", PHASE_PREPARING);
			runtimeState().put("landmark.preparation.started", preparationStartedAt);
			runtimeState().put("landmark.preparation.deadline", preparationStartedAt + PREPARATION_MILLIS);
		}
		scheduleDeadline("preparation", runtimeState().getLong("landmark.preparation.deadline", 0), this::startBattle);
	}

	private synchronized void startBattle() {
		if (destroyed || reward.isRewarded()) {
			return;
		}
		battleStartedAt = runtimeState().getLong("landmark.battle.started", 0);
		if (battleStartedAt == 0) {
			battleStartedAt = runtimeState().getLong("landmark.preparation.deadline", System.currentTimeMillis());
			runtimeState().put("landmark.battle.started", battleStartedAt);
			runtimeState().put("landmark.battle.deadline", battleStartedAt + BATTLE_MILLIS);
		}
		runtimeState().put("landmark.phase", PHASE_BATTLE);
		reward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		setDoorState(180, true);
		setDoorState(181, true);
		spawn(833898, 264.65891f, 259.27396f, 88.502739f, (byte) 0, 60);
		sendMsgByRace(1401181, Race.PC_ALL);
		sendMsgByRace(1403564, Race.PC_ALL);
		startInstancePacket();
		sendPacket(false);
		scheduleDeadline("battle", runtimeState().getLong("landmark.battle.deadline", 0), this::finishBattle);
	}

	private void scheduleSupply() {
		long deadline = runtimeState().getLong("landmark.supply.deadline", 0);
		if (deadline == 0) {
			deadline = preparationStartedAt + SUPPLY_MILLIS;
			runtimeState().put("landmark.supply.deadline", deadline);
		}
		scheduleDeadline("supply", deadline, this::spawnSupplies);
	}

	private void spawnSupplies() {
		if (destroyed || reward.isRewarded()) {
			return;
		}
		sendPacket(false);
		sendMsgByRace(1403625, Race.ELYOS);
		sendMsgByRace(1403626, Race.ASMODIANS);
		spawn(834168, 252.9754f, 246.21234f, 92.94253f, (byte) 15);
		spawn(834169, 276.4865f, 271.9778f, 92.94253f, (byte) 75);
	}

	private void finishBattle() {
		stopInstance(reward.getWinnerRaceByScore());
	}

	protected synchronized void stopInstance(Race winner) {
		if (destroyed || runtimeState().getBoolean("landmark.settled", false)) {
			return;
		}
		reward.setWinnerRace(winner);
		reward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		long endedAt = runtimeState().getLong("landmark.battle.ended", 0);
		if (endedAt == 0) {
			endedAt = System.currentTimeMillis();
			runtimeState().put("landmark.battle.ended", endedAt);
		}
		int elyosPoints = getPointsByRace(Race.ELYOS).intValue();
		int asmodianPoints = getPointsByRace(Race.ASMODIANS).intValue();
		int minimumTeamSize = (int) Math.min(
			reward.getInstanceRewards().stream().filter(player -> player.getRace() == Race.ELYOS).count(),
			reward.getInstanceRewards().stream().filter(player -> player.getRace() == Race.ASMODIANS).count());
		for (LandMarkPlayerReward playerReward : List.copyOf(reward.getInstanceRewards())) {
			int teamScore = playerReward.getRace() == Race.ELYOS ? elyosPoints : asmodianPoints;
			int opposingScore = playerReward.getRace() == Race.ELYOS ? asmodianPoints : elyosPoints;
			BattleResult result = InstanceSettlementService.battlegroundResult(teamScore, opposingScore);
			double bonusRate = InstanceSettlementService.battlegroundBonusRate(
				playerReward.calculateParticipation(battleStartedAt, endedAt), teamScore, opposingScore);
			int calculateMask = playerReward.getRace() == Race.ELYOS && elyosTargetCompleted ? 1
				: playerReward.getRace() == Race.ASMODIANS && asmodianTargetCompleted ? 2 : 0;
			RewardPlan base = InstanceSettlementService.battlegroundPlan(instance, result, 0, teamScore,
				calculateMask, minimumTeamSize);
			RewardPlan total = InstanceSettlementService.battlegroundPlan(instance, result, bonusRate, teamScore,
				calculateMask, minimumTeamSize);
			InstanceSettlementService.applyBattlegroundDisplay(playerReward, base, total);
			InstanceSettlementService.queueBattleground(instance, playerReward.getOwner(), result, total);
			Player player = instance.getPlayer(playerReward.getOwner());
			if (player != null) {
				if (PlayerActions.isAlreadyDead(player)) {
					PlayerReviveService.duelRevive(player);
				}
				InstanceSettlementService.settleBattleground(instance, player, result, total);
				PacketSendUtility.sendPacket(player,
					new SM_INSTANCE_SCORE(5, getTime(), reward, player.getObjectId()));
			}
		}
		runtimeState().put("landmark.winner", winner.getRaceId());
		runtimeState().put("landmark.phase", PHASE_FINISHED);
		runtimeState().put("landmark.settled", true);
		for (Npc npc : instance.getNpcs()) {
			npc.getController().onDelete();
		}
		long exitDeadline = endedAt + EXIT_MILLIS;
		runtimeState().put("landmark.exit.deadline", exitDeadline);
		scheduleDeadline("exit", exitDeadline, this::exitPlayers);
	}

	@Override
	public void onEnterInstance(Player player) {
		if (!reward.containPlayer(player.getObjectId())) {
			reward.regPlayerReward(player);
		}
		LandMarkPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		playerReward.updateBonusTime();
		persistPlayer(playerReward);
		sendEnterPacket(player);
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
		sendPacket(true);
		sendPacket(false);
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
		LandMarkPlayerReward ownerReward = reward.getPlayerReward(player.getObjectId());
		if (ownerReward != null) {
			ownerReward.endBoostMoraleEffect(player);
			ownerReward.applyBoostMoraleEffect(player);
		}
		if (lastAttacker instanceof Player attacker && attacker.getRace() != player.getRace()) {
			updateScore(attacker, player, 50, true);
		}
		return true;
	}

	protected void updateScore(Player player, Creature target, int points, boolean pvpKill) {
		if (points == 0 || reward.isRewarded()) {
			return;
		}
		reward.addPointsByRace(player.getRace(), points);
		List<Player> recipients = new ArrayList<>();
		if (target != null && player.isInGroup2()) {
			for (Player member : player.getPlayerGroup2().getOnlineMembers()) {
				if (!member.getLifeStats().isAlreadyDead()
						&& MathUtil.isIn3dRange(member, target, GroupConfig.GROUP_MAX_DISTANCE)) {
					recipients.add(member);
				}
			}
		} else {
			recipients.add(player);
		}
		for (Player recipient : recipients) {
			LandMarkPlayerReward playerReward = reward.getPlayerReward(recipient.getObjectId());
			if (playerReward == null) {
				continue;
			}
			playerReward.addPoints(points / recipients.size());
			persistPlayer(playerReward);
			if (target instanceof Npc npc) {
				PacketSendUtility.sendPacket(recipient, new SM_SYSTEM_MESSAGE(1400237,
					new DescriptionId(npc.getObjectTemplate().getNameId() * 2 + 1), points));
			} else if (target instanceof Player) {
				PacketSendUtility.sendPacket(recipient, new SM_SYSTEM_MESSAGE(1400237, target.getName(), points));
			}
		}
		if (pvpKill && points > 0) {
			reward.addPvpKillsByRace(player.getRace(), 1);
			LandMarkPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
			if (playerReward != null) {
				playerReward.addPvPKillToPlayer();
				persistPlayer(playerReward);
			}
		}
		persistTeamState();
		sendScorePacket(player.getObjectId());
	}

	@Override
	public void onDie(Npc npc) {
		if (npc.getNpcId() != 243965 && npc.getNpcId() != 243966) {
			return;
		}
		Player player = npc.getAggroList().getMostPlayerDamage();
		if (player != null) {
			npc.getController().onDelete();
			updateScore(player, npc, 50, false);
		}
	}

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		int points = switch (npc.getNpcId()) {
			case 833898 -> 1000;
			case 806343, 806375 -> 200;
			case 806344, 806376 -> 1000;
			case 806345, 806377 -> 500;
			case 806346, 806378 -> 50_000;
			default -> 0;
		};
		switch (npc.getNpcId()) {
			case 833898 -> npc.getController().onDelete();
			case 806343 -> completeDevice(npc, 1403428, false, false);
			case 806344 -> completeDevice(npc, 1403429, false, false);
			case 806345 -> completeDevice(npc, 1403430, false, false);
			case 806346 -> completeDevice(npc, 1403431, true, false);
			case 806375 -> completeDevice(npc, 1403435, false, false);
			case 806376 -> completeDevice(npc, 1403436, false, false);
			case 806377 -> completeDevice(npc, 1403437, false, false);
			case 806378 -> completeDevice(npc, 1403438, false, true);
			case 802192 -> {
				sendMsgByRace(1402368, Race.PC_ALL);
				spawnElyosTrap();
			}
			case 802193 -> {
				sendMsgByRace(1402369, Race.PC_ALL);
				spawnAsmodianTrap();
			}
		}
		updateScore(player, npc, points, false);
	}

	private void completeDevice(Npc npc, int messageId, boolean elyosComplete, boolean asmodianComplete) {
		npc.getController().onDelete();
		sendMsgByRace(messageId, Race.PC_ALL);
		if (elyosComplete) {
			elyosTargetCompleted = true;
			runtimeState().put("landmark.target.elyos", true);
			sendMsgByRace(1403434, Race.PC_ALL);
		}
		if (asmodianComplete) {
			asmodianTargetCompleted = true;
			runtimeState().put("landmark.target.asmodians", true);
			sendMsgByRace(1403441, Race.PC_ALL);
		}
	}

	private void spawnElyosTrap() {
		spawn(702404, 234.43842f, 194.1041f, 79.23065f, (byte) 105);
		spawn(702405, 234.13383f, 194.39594f, 79.23065f, (byte) 105);
		spawn(702405, 234.62419f, 193.95747f, 79.23065f, (byte) 45);
		spawn(702405, 234.42247f, 194.1363f, 79.23065f, (byte) 16);
		spawn(702405, 234.53394f, 194.27177f, 79.23065f, (byte) 75);
	}

	private void spawnAsmodianTrap() {
		spawn(702404, 294.57443f, 324.22205f, 79.23065f, (byte) 45);
		spawn(702405, 294.53418f, 324.0909f, 79.23065f, (byte) 105);
		spawn(702405, 294.66284f, 324.29172f, 79.23065f, (byte) 75);
		spawn(702405, 294.4634f, 323.84235f, 79.23065f, (byte) 15);
		spawn(702405, 294.70172f, 324.23065f, 79.23065f, (byte) 45);
	}

	private void sendMsgByRace(int messageId, Race race) {
		instance.doOnAllPlayers(player -> {
			if (race == Race.PC_ALL || player.getRace() == race) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(messageId));
			}
		});
	}

	private void sendScorePacket(int objectId) {
		instance.doOnAllPlayers(player -> PacketSendUtility.sendPacket(player,
			new SM_INSTANCE_SCORE(11, getTime(), reward, objectId)));
	}

	private int getTime() {
		long now = System.currentTimeMillis();
		if (reward.isPreparing()) {
			return (int) Math.max(0, PREPARATION_MILLIS - (now - preparationStartedAt));
		}
		if (reward.isStartProgress()) {
			return (int) Math.max(0, BATTLE_MILLIS - (now - battleStartedAt));
		}
		return 0;
	}

	private MutableInt getPointsByRace(Race race) {
		return reward.getPointsByRace(race);
	}

	private void removeItems(Player player) {
		Storage storage = player.getInventory();
		for (int itemId = 164000413; itemId <= 164000414; itemId++) {
			storage.decreaseByItemId(itemId, storage.getItemCountByItemId(itemId));
		}
	}

	@Override
	public void onLeaveInstance(Player player) {
		LandMarkPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.endBoostMoraleEffect(player);
			playerReward.updateLogOutTime();
			persistPlayer(playerReward);
		}
		removeItems(player);
	}

	@Override
	public void onPlayerLogOut(Player player) {
		LandMarkPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.updateLogOutTime();
			persistPlayer(playerReward);
		}
		removeItems(player);
	}

	@Override
	public void onPlayerLogin(Player player) {
		LandMarkPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.updateBonusTime();
			persistPlayer(playerReward);
			sendEnterPacket(player);
		}
	}

	@Override
	public void onExitInstance(Player player) {
		removeItems(player);
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}

	@Override
	public InstanceReward<?> getInstanceReward() {
		return reward;
	}

	@Override
	public void onInstanceDestroy() {
		destroyed = true;
		reward.clear();
	}

	private void exitPlayers() {
		if (destroyed) {
			return;
		}
		for (Player player : instance.getPlayersInside()) {
			onExitInstance(player);
		}
		GameCoreGameplayServices.autoGroupService().unRegisterInstance(instanceId);
	}

	private void persistPlayer(LandMarkPlayerReward playerReward) {
		int playerId = playerReward.getOwner();
		participants.add(playerId);
		runtimeState().put("landmark.player." + playerId + ".race", playerReward.getRace().getRaceId());
		runtimeState().put("landmark.player." + playerId + ".joined", playerReward.getJoinedAt());
		runtimeState().put("landmark.player." + playerId + ".points", playerReward.getPoints());
		runtimeState().put("landmark.player." + playerId + ".kills", playerReward.getPvPKills());
		runtimeState().put("landmark.player." + playerId + ".logout", playerReward.getLogoutAt());
		runtimeState().put("landmark.player." + playerId + ".offline", playerReward.getOfflineMillis());
		runtimeState().put("landmark.players", participants.stream().map(String::valueOf)
			.collect(java.util.stream.Collectors.joining(",")));
	}

	private void persistTeamState() {
		runtimeState().put("landmark.elyos.points", getPointsByRace(Race.ELYOS).intValue());
		runtimeState().put("landmark.asmodians.points", getPointsByRace(Race.ASMODIANS).intValue());
		runtimeState().put("landmark.elyos.kills", reward.getPvpKillsByRace(Race.ELYOS).intValue());
		runtimeState().put("landmark.asmodians.kills", reward.getPvpKillsByRace(Race.ASMODIANS).intValue());
	}

	private void restoreReward() {
		reward.addPointsByRace(Race.ELYOS, runtimeState().getInt("landmark.elyos.points", 0));
		reward.addPointsByRace(Race.ASMODIANS, runtimeState().getInt("landmark.asmodians.points", 0));
		reward.addPvpKillsByRace(Race.ELYOS, runtimeState().getInt("landmark.elyos.kills", 0));
		reward.addPvpKillsByRace(Race.ASMODIANS, runtimeState().getInt("landmark.asmodians.kills", 0));
		elyosTargetCompleted = runtimeState().getBoolean("landmark.target.elyos", false);
		asmodianTargetCompleted = runtimeState().getBoolean("landmark.target.asmodians", false);
		String players = runtimeState().get("landmark.players", "");
		if (players.isBlank()) {
			return;
		}
		for (String value : players.split(",")) {
			int playerId = Integer.parseInt(value);
			participants.add(playerId);
			LandMarkPlayerReward playerReward = new LandMarkPlayerReward(playerId, reward.getBuffId(),
				race(runtimeState().getInt("landmark.player." + playerId + ".race", Race.ELYOS.getRaceId())),
				runtimeState().getLong("landmark.player." + playerId + ".joined", 0));
			playerReward.addPoints(runtimeState().getInt("landmark.player." + playerId + ".points", 0));
			for (int kill = runtimeState().getInt("landmark.player." + playerId + ".kills", 0); kill > 0; kill--) {
				playerReward.addPvPKillToPlayer();
			}
			playerReward.restoreActivity(runtimeState().getLong("landmark.player." + playerId + ".logout", 0),
				runtimeState().getLong("landmark.player." + playerId + ".offline", 0));
			reward.addPlayerReward(playerReward);
		}
	}

	private void restoreWinner() {
		int winner = runtimeState().getInt("landmark.winner", -1);
		if (winner >= 0) {
			reward.setWinnerRace(race(winner));
		}
	}

	private Race race(int raceId) {
		return raceId == Race.ELYOS.getRaceId() ? Race.ELYOS : Race.ASMODIANS;
	}
}
