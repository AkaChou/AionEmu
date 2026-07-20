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
import com.aionemu.gameserver.model.instance.instancereward.IdgelDomeReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.IdgelDomePlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.BattleResult;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(301310000)
public class IdgelDomeInstance extends GeneralInstanceHandler {
	private static final long PREPARATION_MILLIS = 120_000;
	private static final long BATTLE_MILLIS = 1_200_000;
	private static final long SUPPLY_MILLIS = 300_000;
	private static final long BOSS_MILLIS = 600_000;
	private static final long EXIT_MILLIS = 60_000;
	private static final String PHASE_PREPARING = "PREPARING";
	private static final String PHASE_BATTLE = "BATTLE";
	private static final String PHASE_FINISHED = "FINISHED";

	private final Set<Integer> participants = new LinkedHashSet<>();
	private final Set<Integer> movies = new LinkedHashSet<>();
	private IdgelDomeReward reward;
	private Race raceKilledKunax;
	private long preparationStartedAt;
	private long battleStartedAt;
	private volatile boolean destroyed;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		reward = new IdgelDomeReward(mapId, instanceId, instance);
		restoreReward();
		String phase = runtimeState().get("idgel.phase", PHASE_PREPARING);
		preparationStartedAt = runtimeState().getLong("idgel.preparation.started", 0);
		battleStartedAt = runtimeState().getLong("idgel.battle.started", 0);
		if (PHASE_FINISHED.equals(phase)) {
			reward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
			restoreWinner();
			scheduleDeadline("exit", runtimeState().getLong("idgel.exit.deadline", 0), this::exitPlayers);
			return;
		}
		if (PHASE_BATTLE.equals(phase)) {
			reward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
			setDoorState(1, true);
			setDoorState(99, true);
			scheduleDeadline("battle", runtimeState().getLong("idgel.battle.deadline", 0), this::finishBattle);
		} else {
			reward.setInstanceScoreType(InstanceScoreType.PREPARING);
			startPreparation();
		}
		scheduleLegacyEvents();
	}

	private synchronized void startPreparation() {
		if (preparationStartedAt == 0) {
			preparationStartedAt = System.currentTimeMillis();
			runtimeState().put("idgel.phase", PHASE_PREPARING);
			runtimeState().put("idgel.preparation.started", preparationStartedAt);
			runtimeState().put("idgel.preparation.deadline", preparationStartedAt + PREPARATION_MILLIS);
		}
		scheduleDeadline("preparation", runtimeState().getLong("idgel.preparation.deadline", 0), this::startBattle);
	}

	private synchronized void startBattle() {
		if (destroyed || reward.isRewarded()) {
			return;
		}
		battleStartedAt = runtimeState().getLong("idgel.battle.started", 0);
		if (battleStartedAt == 0) {
			battleStartedAt = runtimeState().getLong("idgel.preparation.deadline", System.currentTimeMillis());
			runtimeState().put("idgel.battle.started", battleStartedAt);
			runtimeState().put("idgel.battle.deadline", battleStartedAt + BATTLE_MILLIS);
		}
		runtimeState().put("idgel.phase", PHASE_BATTLE);
		reward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		setDoorState(1, true);
		setDoorState(99, true);
		sendMsgByRace(1401181, Race.PC_ALL);
		startInstancePacket();
		sendPacket(false);
		scheduleDeadline("battle", runtimeState().getLong("idgel.battle.deadline", 0), this::finishBattle);
	}

	private void scheduleLegacyEvents() {
		long supplyDeadline = runtimeState().getLong("idgel.supply.deadline", 0);
		if (supplyDeadline == 0) {
			supplyDeadline = preparationStartedAt + SUPPLY_MILLIS;
			runtimeState().put("idgel.supply.deadline", supplyDeadline);
		}
		long bossDeadline = runtimeState().getLong("idgel.boss.deadline", 0);
		if (bossDeadline == 0) {
			bossDeadline = preparationStartedAt + BOSS_MILLIS;
			runtimeState().put("idgel.boss.deadline", bossDeadline);
		}
		scheduleDeadline("supply", supplyDeadline, this::spawnSupplies);
		scheduleDeadline("boss", bossDeadline, this::spawnBoss);
	}

	private void spawnSupplies() {
		if (destroyed || reward.isRewarded()) {
			return;
		}
		sendPacket(false);
		sendMsgByRace(1402086, Race.PC_ALL);
		spawn(702581, 312.9132f, 311.31152f, 79.86219f, (byte) 104);
		spawn(702582, 216.0075f, 209.24077f, 79.86219f, (byte) 44);
		spawn(702583, 252.9754f, 246.21234f, 92.94253f, (byte) 15);
		spawn(702583, 276.4865f, 271.9778f, 92.94253f, (byte) 75);
	}

	private void spawnBoss() {
		if (destroyed || reward.isRewarded()) {
			return;
		}
		sendPacket(false);
		sendMsgByRace(1402598, Race.PC_ALL);
		sendMsgByRace(1402367, Race.PC_ALL);
		spawn(234190, 266.579f, 257.436f, 85.81963f, (byte) 46);
		spawn(234751, 250.67055f, 257.33798f, 85.81963f, (byte) 62);
		spawn(234752, 265.60724f, 272.637f, 85.81963f, (byte) 36);
		spawn(234753, 263.66858f, 245.04124f, 85.81963f, (byte) 101);
		spawn(234754, 278.0694f, 262.5485f, 85.81963f, (byte) 2);
	}

	private void finishBattle() {
		stopInstance(reward.getWinnerRaceByScore());
	}

	protected synchronized void stopInstance(Race winner) {
		if (destroyed || runtimeState().getBoolean("idgel.settled", false)) {
			return;
		}
		reward.setWinnerRace(winner);
		reward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		long endedAt = runtimeState().getLong("idgel.battle.ended", 0);
		if (endedAt == 0) {
			endedAt = System.currentTimeMillis();
			runtimeState().put("idgel.battle.ended", endedAt);
		}
		int elyosPoints = getPointsByRace(Race.ELYOS).intValue();
		int asmodianPoints = getPointsByRace(Race.ASMODIANS).intValue();
		int minimumTeamSize = (int) Math.min(
			reward.getInstanceRewards().stream().filter(player -> player.getRace() == Race.ELYOS).count(),
			reward.getInstanceRewards().stream().filter(player -> player.getRace() == Race.ASMODIANS).count());
		for (IdgelDomePlayerReward playerReward : List.copyOf(reward.getInstanceRewards())) {
			int teamScore = playerReward.getRace() == Race.ELYOS ? elyosPoints : asmodianPoints;
			int opposingScore = playerReward.getRace() == Race.ELYOS ? asmodianPoints : elyosPoints;
			BattleResult result = InstanceSettlementService.battlegroundResult(teamScore, opposingScore);
			double bonusRate = InstanceSettlementService.battlegroundBonusRate(
				playerReward.calculateParticipation(battleStartedAt, endedAt), teamScore, opposingScore);
			int calculateMask = raceKilledKunax == playerReward.getRace() ? 1 : 0;
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
		runtimeState().put("idgel.winner", winner.getRaceId());
		runtimeState().put("idgel.phase", PHASE_FINISHED);
		runtimeState().put("idgel.settled", true);
		for (Npc npc : instance.getNpcs()) {
			npc.getController().onDelete();
		}
		long exitDeadline = endedAt + EXIT_MILLIS;
		runtimeState().put("idgel.exit.deadline", exitDeadline);
		scheduleDeadline("exit", exitDeadline, this::exitPlayers);
	}

	@Override
	public void onEnterInstance(Player player) {
		if (!reward.containPlayer(player.getObjectId())) {
			reward.regPlayerReward(player);
		}
		IdgelDomePlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		playerReward.updateBonusTime();
		persistPlayer(playerReward);
		sendMovie(player, player.getRace() == Race.ELYOS ? 901 : 902);
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
		IdgelDomePlayerReward ownerReward = reward.getPlayerReward(player.getObjectId());
		if (ownerReward != null) {
			ownerReward.endBoostMoraleEffect(player);
			ownerReward.applyBoostMoraleEffect(player);
		}
		if (lastAttacker instanceof Player attacker && attacker.getRace() != player.getRace()) {
			updateScore(attacker, player, 200, true);
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
			IdgelDomePlayerReward playerReward = reward.getPlayerReward(recipient.getObjectId());
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
			IdgelDomePlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
			if (playerReward != null) {
				playerReward.addPvPKillToPlayer();
				persistPlayer(playerReward);
			}
		}
		persistTeamState();
		sendScorePacket(player.getObjectId());
		if (reward.hasCapPoints()) {
			stopInstance(reward.getWinnerRaceByScore());
		}
	}

	@Override
	public void onDie(Npc npc) {
		Player player = npc.getAggroList().getMostPlayerDamage();
		if (player == null) {
			return;
		}
		int points = switch (npc.getNpcId()) {
			case 234186, 234187, 234188, 234189 -> 120;
			case 234751, 234752, 234753, 234754 -> 200;
			case 234190 -> 6000;
			default -> 0;
		};
		if (points == 0) {
			return;
		}
		if (npc.getNpcId() == 234190) {
			raceKilledKunax = player.getRace();
			runtimeState().put("idgel.kunax.race", raceKilledKunax.getRaceId());
			long deadline = System.currentTimeMillis() + 30_000;
			runtimeState().put("idgel.kunax.deadline", deadline);
			scheduleDeadline("kunax", deadline, this::finishBattle);
		} else {
			npc.getController().onDelete();
		}
		updateScore(player, npc, points, false);
	}

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 802192 -> {
				sendMsgByRace(1402368, Race.PC_ALL);
				spawnTrap(true);
			}
			case 802193 -> {
				sendMsgByRace(1402369, Race.PC_ALL);
				spawnTrap(false);
			}
		}
	}

	private void spawnTrap(boolean elyosSide) {
		if (elyosSide) {
			spawn(702404, 234.43842f, 194.1041f, 79.23065f, (byte) 105);
			spawn(702405, 234.13383f, 194.39594f, 79.23065f, (byte) 105);
			spawn(702405, 234.62419f, 193.95747f, 79.23065f, (byte) 45);
			spawn(702405, 234.42247f, 194.1363f, 79.23065f, (byte) 16);
			spawn(702405, 234.53394f, 194.27177f, 79.23065f, (byte) 75);
		} else {
			spawn(702404, 294.57443f, 324.22205f, 79.23065f, (byte) 45);
			spawn(702405, 294.53418f, 324.0909f, 79.23065f, (byte) 105);
			spawn(702405, 294.66284f, 324.29172f, 79.23065f, (byte) 75);
			spawn(702405, 294.4634f, 323.84235f, 79.23065f, (byte) 15);
			spawn(702405, 294.70172f, 324.23065f, 79.23065f, (byte) 45);
		}
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
		for (int itemId = 164000314; itemId <= 164000316; itemId++) {
			storage.decreaseByItemId(itemId, storage.getItemCountByItemId(itemId));
		}
	}

	@Override
	public void onLeaveInstance(Player player) {
		IdgelDomePlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.endBoostMoraleEffect(player);
			playerReward.updateLogOutTime();
			persistPlayer(playerReward);
		}
		removeItems(player);
	}

	@Override
	public void onPlayerLogOut(Player player) {
		IdgelDomePlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.updateLogOutTime();
			persistPlayer(playerReward);
		}
		removeItems(player);
	}

	@Override
	public void onPlayerLogin(Player player) {
		IdgelDomePlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
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
		GameCoreGameplayServices.autoGroupService().unRegisterInstance(instance);
	}

	private void sendMovie(Player player, int movieId) {
		if (movies.add(movieId)) {
			PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movieId));
		}
	}

	private void persistPlayer(IdgelDomePlayerReward playerReward) {
		int playerId = playerReward.getOwner();
		participants.add(playerId);
		runtimeState().put("idgel.player." + playerId + ".race", playerReward.getRace().getRaceId());
		runtimeState().put("idgel.player." + playerId + ".joined", playerReward.getJoinedAt());
		runtimeState().put("idgel.player." + playerId + ".points", playerReward.getPoints());
		runtimeState().put("idgel.player." + playerId + ".kills", playerReward.getPvPKills());
		runtimeState().put("idgel.player." + playerId + ".logout", playerReward.getLogoutAt());
		runtimeState().put("idgel.player." + playerId + ".offline", playerReward.getOfflineMillis());
		runtimeState().put("idgel.players", participants.stream().map(String::valueOf)
			.collect(java.util.stream.Collectors.joining(",")));
	}

	private void persistTeamState() {
		runtimeState().put("idgel.elyos.points", getPointsByRace(Race.ELYOS).intValue());
		runtimeState().put("idgel.asmodians.points", getPointsByRace(Race.ASMODIANS).intValue());
		runtimeState().put("idgel.elyos.kills", reward.getPvpKillsByRace(Race.ELYOS).intValue());
		runtimeState().put("idgel.asmodians.kills", reward.getPvpKillsByRace(Race.ASMODIANS).intValue());
	}

	private void restoreReward() {
		reward.addPointsByRace(Race.ELYOS, runtimeState().getInt("idgel.elyos.points", 1000) - 1000);
		reward.addPointsByRace(Race.ASMODIANS, runtimeState().getInt("idgel.asmodians.points", 1000) - 1000);
		reward.addPvpKillsByRace(Race.ELYOS, runtimeState().getInt("idgel.elyos.kills", 0));
		reward.addPvpKillsByRace(Race.ASMODIANS, runtimeState().getInt("idgel.asmodians.kills", 0));
		int kunaxRace = runtimeState().getInt("idgel.kunax.race", -1);
		if (kunaxRace >= 0) {
			raceKilledKunax = race(kunaxRace);
		}
		String players = runtimeState().get("idgel.players", "");
		if (players.isBlank()) {
			return;
		}
		for (String value : players.split(",")) {
			int playerId = Integer.parseInt(value);
			participants.add(playerId);
			IdgelDomePlayerReward playerReward = new IdgelDomePlayerReward(playerId, reward.getBuffId(),
				race(runtimeState().getInt("idgel.player." + playerId + ".race", Race.ELYOS.getRaceId())),
				runtimeState().getLong("idgel.player." + playerId + ".joined", 0));
			playerReward.addPoints(runtimeState().getInt("idgel.player." + playerId + ".points", 0));
			for (int kill = runtimeState().getInt("idgel.player." + playerId + ".kills", 0); kill > 0; kill--) {
				playerReward.addPvPKillToPlayer();
			}
			playerReward.restoreActivity(runtimeState().getLong("idgel.player." + playerId + ".logout", 0),
				runtimeState().getLong("idgel.player." + playerId + ".offline", 0));
			reward.addPlayerReward(playerReward);
		}
	}

	private void restoreWinner() {
		int winner = runtimeState().getInt("idgel.winner", -1);
		if (winner >= 0) {
			reward.setWinnerRace(race(winner));
		}
	}

	private Race race(int raceId) {
		return raceId == Race.ELYOS.getRaceId() ? Race.ELYOS : Race.ASMODIANS;
	}
}
