package com.aionemu.gameserver.instance.handlers.scripts;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.lifecycle.GameCoreGameplayServices;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.actions.PlayerActions;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.TreasureIslandReward;
import com.aionemu.gameserver.model.instance.playerreward.BattlegroundPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DIE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.BattleResult;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(301700000)
public class TreasureIslandOfCourageInstance extends GeneralInstanceHandler {
	private static final long PREPARATION_MILLIS = 60_000;
	private static final long BATTLE_MILLIS = 780_000;
	private static final long EXIT_MILLIS = 60_000;
	private static final String PHASE_PREPARING = "PREPARING";
	private static final String PHASE_BATTLE = "BATTLE";
	private static final String PHASE_FINISHED = "FINISHED";

	private TreasureIslandReward reward;
	private final Set<Integer> participants = new LinkedHashSet<>();
	private long preparationStartedAt;
	private long battleStartedAt;
	private volatile boolean destroyed;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		reward = new TreasureIslandReward(mapId, instanceId);
		restoreReward();
		String phase = runtimeState().get("idrun.phase", PHASE_PREPARING);
		preparationStartedAt = runtimeState().getLong("idrun.preparation.started", 0);
		battleStartedAt = runtimeState().getLong("idrun.battle.started", 0);
		if (PHASE_BATTLE.equals(phase)) {
			reward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
			setDoorState(8, true);
			setDoorState(93, true);
			scheduleDeadline("battle", runtimeState().getLong("idrun.battle.deadline", 0), this::finishBattle);
		} else if (PHASE_FINISHED.equals(phase)) {
			reward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
			scheduleDeadline("exit", runtimeState().getLong("idrun.exit.deadline", 0), this::exitPlayers);
		} else if (preparationStartedAt > 0) {
			scheduleDeadline("preparation", runtimeState().getLong("idrun.preparation.deadline", 0), this::startBattle);
		}
	}

	@Override
	public void onEnterInstance(Player player) {
		BattlegroundPlayerReward playerReward = reward.registerPlayer(player.getObjectId(), player.getRace());
		participants.add(player.getObjectId());
		playerReward.updateBonusTime();
		persistPlayer(playerReward);
		startPreparation();
		sendWorldInfo(player);
	}

	private synchronized void startPreparation() {
		if (preparationStartedAt != 0 || destroyed || reward.isRewarded()) {
			return;
		}
		preparationStartedAt = System.currentTimeMillis();
		long deadline = preparationStartedAt + PREPARATION_MILLIS;
		runtimeState().put("idrun.phase", PHASE_PREPARING);
		runtimeState().put("idrun.preparation.started", preparationStartedAt);
		runtimeState().put("idrun.preparation.deadline", deadline);
		persistParticipants();
		scheduleDeadline("preparation", deadline, this::startBattle);
	}

	private synchronized void startBattle() {
		if (battleStartedAt != 0 || destroyed || reward.isRewarded()) {
			return;
		}
		battleStartedAt = System.currentTimeMillis();
		reward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		long deadline = battleStartedAt + BATTLE_MILLIS;
		runtimeState().put("idrun.phase", PHASE_BATTLE);
		runtimeState().put("idrun.battle.started", battleStartedAt);
		runtimeState().put("idrun.battle.deadline", deadline);
		setDoorState(8, true);
		setDoorState(93, true);
		broadcastWorldInfo(6);
		broadcastTeamTables();
		scheduleDeadline("battle", deadline, this::finishBattle);
	}

	public void onStageSensor(Player player, int npcId) {
		if (!reward.isStartProgress()) {
			return;
		}
		int stage = npcId - 836198;
		if (stage < 1 || stage > 5) {
			return;
		}
		int points = reward.registerStage(player.getObjectId(), stage);
		if (points < 0) {
			return;
		}
		String side = player.getRace() == Race.ELYOS ? "Light" : "Dark";
		RetailConditionSpawnEngine.setVariable(instance, "Stage_" + stage + "_" + side + "_Condition_1", 1, 0);
		BattlegroundPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		persistPlayer(playerReward);
		runtimeState().put("idrun.stage." + stage + ".arrivals", reward.getStageArrivals(stage));
		broadcastScore(player.getObjectId());
	}

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 835544, 835592 -> giveHeroItem(player, 11277);
			case 835545, 835593 -> giveHeroItem(player, 11278);
			case 835546, 835594 -> giveHeroItem(player, 11279);
			case 835547, 835794 -> giveHeroItem(player, 11280);
			case 836347 -> openTreasure(player, 185000320, 188058577, false);
			case 836348 -> openTreasure(player, 185000319, 188058576, true);
		}
	}

	private void giveHeroItem(Player player, int skillId) {
		GameEngineServices.skillEngine().applyEffectDirectly(skillId, player, player, 4_000);
		ItemService.addItem(player, 190100295, 1);
		ItemService.addItem(player, 169300017, 1);
	}

	private void openTreasure(Player player, int keyId, int rewardId, boolean magnificent) {
		if (!player.getInventory().decreaseByItemId(keyId, 1)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1111300));
			return;
		}
		ItemService.addItem(player, rewardId, 1);
		if (magnificent) {
			RetailConditionSpawnEngine.setVariable(instance, "idrun_treasure_despawn", 0, 1);
		}
	}

	private synchronized void finishBattle() {
		if (destroyed || reward.isRewarded()) {
			return;
		}
		reward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		long endedAt = runtimeState().getLong("idrun.battle.ended", 0);
		if (endedAt == 0) {
			endedAt = System.currentTimeMillis();
			runtimeState().put("idrun.battle.ended", endedAt);
		}
		int elyosPoints = reward.getPointsByRace(Race.ELYOS);
		int asmodianPoints = reward.getPointsByRace(Race.ASMODIANS);
		int minimumTeamSize = (int) Math.min(
				reward.getInstanceRewards().stream().filter(r -> r.getRace() == Race.ELYOS).count(),
				reward.getInstanceRewards().stream().filter(r -> r.getRace() == Race.ASMODIANS).count());

		for (BattlegroundPlayerReward playerReward : List.copyOf(reward.getInstanceRewards())) {
			int teamScore = playerReward.getRace() == Race.ELYOS ? elyosPoints : asmodianPoints;
			int opposingScore = playerReward.getRace() == Race.ELYOS ? asmodianPoints : elyosPoints;
			BattleResult result = InstanceSettlementService.battlegroundResult(teamScore, opposingScore);
			double bonusRate = InstanceSettlementService.battlegroundBonusRate(
					playerReward.calculateParticipation(battleStartedAt, endedAt), teamScore, opposingScore);
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
			PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(5, getTime(), reward, player.getObjectId()));
		}
		broadcastWorldInfo(6);
		broadcastTeamTables();
		long deadline = runtimeState().getLong("idrun.exit.deadline", 0);
		if (deadline == 0) {
			deadline = endedAt + EXIT_MILLIS;
			runtimeState().put("idrun.exit.deadline", deadline);
		}
		runtimeState().put("idrun.phase", PHASE_FINISHED);
		scheduleDeadline("exit", deadline, this::exitPlayers);
	}

	private void sendWorldInfo(Player player) {
		List<Player> players = instance.getPlayersInside();
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(6, getTime(), reward, players, true));
		for (Race race : List.of(Race.ELYOS, Race.ASMODIANS)) {
			Integer representative = representative(race);
			if (representative != null) {
				PacketSendUtility.sendPacket(player,
						new SM_INSTANCE_SCORE(7, getTime(), reward, representative, players));
			}
		}
		PacketSendUtility.sendPacket(player,
				new SM_INSTANCE_SCORE(4, getTime(), reward, player.getObjectId(), 0, player.getRace().getRaceId()));
	}

	private void broadcastWorldInfo(int type) {
		List<Player> players = instance.getPlayersInside();
		instance.doOnAllPlayers(player -> PacketSendUtility.sendPacket(player,
				new SM_INSTANCE_SCORE(type, getTime(), reward, players, true)));
	}

	private void broadcastTeamTables() {
		List<Player> players = instance.getPlayersInside();
		for (Race race : List.of(Race.ELYOS, Race.ASMODIANS)) {
			Integer representative = representative(race);
			if (representative != null) {
				instance.doOnAllPlayers(player -> PacketSendUtility.sendPacket(player,
						new SM_INSTANCE_SCORE(7, getTime(), reward, representative, players)));
			}
		}
	}

	private void broadcastScore(int objectId) {
		instance.doOnAllPlayers(player -> PacketSendUtility.sendPacket(player,
				new SM_INSTANCE_SCORE(11, getTime(), reward, objectId)));
	}

	private Integer representative(Race race) {
		return reward.getInstanceRewards().stream().filter(player -> player.getRace() == race)
				.map(BattlegroundPlayerReward::getOwner).findFirst().orElse(null);
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

	private void removeInstanceItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(190100295, storage.getItemCountByItemId(190100295));
		storage.decreaseByItemId(169300017, storage.getItemCountByItemId(169300017));
		for (int skillId = 11277; skillId <= 11280; skillId++) {
			player.getEffectController().removeEffect(skillId);
		}
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeInstanceItems(player);
		BattlegroundPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.updateLogOutTime();
			playerReward.endBoostMoraleEffect(player);
			persistPlayer(playerReward);
		}
	}

	@Override
	public void onExitInstance(Player player) {
		removeInstanceItems(player);
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
	}

	@Override
	public void onPlayerLogOut(Player player) {
		BattlegroundPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.updateLogOutTime();
			persistPlayer(playerReward);
		}
	}

	@Override
	public void onPlayerLogin(Player player) {
		BattlegroundPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.updateBonusTime();
			persistPlayer(playerReward);
			sendWorldInfo(player);
		}
	}

	@Override
	public boolean onDie(Player player, Creature lastAttacker) {
		BattlegroundPlayerReward playerReward = reward.getPlayerReward(player.getObjectId());
		if (playerReward != null) {
			playerReward.endBoostMoraleEffect(player);
			playerReward.applyBoostMoraleEffect(player);
		}
		int attackerId = lastAttacker == null || player.equals(lastAttacker) ? 0 : lastAttacker.getObjectId();
		PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.DIE, 0, attackerId), true);
		PacketSendUtility.sendPacket(player, new SM_DIE(player.haveSelfRezEffect(), player.haveSelfRezItem(), 0, 8));
		return true;
	}

	@Override
	public InstanceReward<?> getInstanceReward() {
		return reward;
	}

	@Override
	public void onInstanceDestroy() {
		destroyed = true;
		cancelDeadline("preparation");
		cancelDeadline("battle");
		cancelDeadline("exit");
		reward.clear();
	}

	private void restoreReward() {
		String players = runtimeState().get("idrun.players", "");
		if (!players.isBlank()) {
			for (String value : players.split(",")) {
				int playerId = Integer.parseInt(value);
				participants.add(playerId);
				int raceId = runtimeState().getInt("idrun.player." + playerId + ".race", Race.ELYOS.getRaceId());
				Race race = raceId == Race.ELYOS.getRaceId() ? Race.ELYOS : Race.ASMODIANS;
				reward.restorePlayer(playerId, race,
						runtimeState().getLong("idrun.player." + playerId + ".joined", creationTime),
						runtimeState().getInt("idrun.player." + playerId + ".points", 0),
						runtimeState().getInt("idrun.player." + playerId + ".stages", 0),
						runtimeState().getLong("idrun.player." + playerId + ".logout", 0),
						runtimeState().getLong("idrun.player." + playerId + ".offline", 0));
			}
		}
		for (int stage = 1; stage <= 5; stage++) {
			reward.restoreStageArrivals(stage, runtimeState().getInt("idrun.stage." + stage + ".arrivals", 0));
		}
	}

	private void persistPlayer(BattlegroundPlayerReward playerReward) {
		int playerId = playerReward.getOwner();
		participants.add(playerId);
		runtimeState().put("idrun.player." + playerId + ".race", playerReward.getRace().getRaceId());
		runtimeState().put("idrun.player." + playerId + ".joined", playerReward.getJoinedAt());
		runtimeState().put("idrun.player." + playerId + ".points", playerReward.getPoints());
		runtimeState().put("idrun.player." + playerId + ".stages", reward.getStageMask(playerId));
		runtimeState().put("idrun.player." + playerId + ".logout", playerReward.getLogoutAt());
		runtimeState().put("idrun.player." + playerId + ".offline", playerReward.getOfflineMillis());
		persistParticipants();
	}

	private void persistParticipants() {
		runtimeState().put("idrun.players", participants.stream().map(String::valueOf)
				.collect(java.util.stream.Collectors.joining(",")));
		}
}
