package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.StonespearReachReward;
import com.aionemu.gameserver.model.instance.playerreward.StonespearReachPlayerReward;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;
import com.aionemu.gameserver.services.player.PlayerReviveService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(301500000)
public class StonespearReachInstance extends GeneralInstanceHandler {
	private static final String STATE_PREFIX = "stonespear.";
	private static final int FINAL_BOSS = 855843;

	private StonespearReachReward instanceReward;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		instanceReward = new StonespearReachReward(mapId, instanceId);
		restoreScore();
		instanceReward.setInstanceScoreType(scoreType());
		restoreDeadline();
	}

	@Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
	}

	@Override
	public void onEnterInstance(Player player) {
		getOrCreatePlayerReward(player.getObjectId());
		if (runtimeState().getBoolean(state("completed"), false)) {
			doReward(player);
			sendScore(0, 0);
			return;
		}
		startPrepareTimer();
	}

	@Override
	public boolean supportsRetailNpcScore(int npcId, int scoreApplyType) {
		if (DataManager.RETAIL_AI_DATA == null) {
			return false;
		}
		var score = DataManager.RETAIL_AI_DATA.getNpcScore(npcId);
		return score != null && score.scoreApplyType() == scoreApplyType
			&& scoreApplyType == 0 && score.equalizingScore() == 0;
	}

	@Override
	public boolean onRetailNpcScore(Player player, Npc npc, int scoreApplyType, int points) {
		return supportsRetailNpcScore(npc.getNpcId(), scoreApplyType) && consumeNpcScore(npc, points);
	}

	@Override
	public void onDie(Npc npc) {
		RetailConditionSpawnEngine.consumeConditionSpawnDeath(instance, npc);
		var score = DataManager.RETAIL_AI_DATA == null ? null : DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId());
		if (score != null && supportsRetailNpcScore(npc.getNpcId(), score.scoreApplyType())) {
			consumeNpcScore(npc, score.value());
		}
		if (npc.getNpcId() == FINAL_BOSS) {
			completeInstance(System.currentTimeMillis());
		}
	}

	@Override
	public void doReward(Player player) {
		if (!runtimeState().getBoolean(state("completed"), false)) {
			return;
		}
		StonespearReachPlayerReward playerReward = getOrCreatePlayerReward(player.getObjectId());
		if (playerReward.isRewarded()) {
			return;
		}
		RewardPlan plan = InstanceSettlementService.timeAttackPlan(mapId, instanceReward.getRank());
		playerReward.setScoreAP(plan.ap());
		playerReward.setCeramium(Math.toIntExact(plan.itemCount(186000469)));
		InstanceSettlementService.settleTimeAttack(instance, player, instanceReward.getRank());
		playerReward.setRewarded();
		runtimeState().put(playerRewardKey(player.getObjectId()), true);
	}

	@Override
	public void onExitInstance(Player player) {
		if (player.isInGroup2()) {
			PlayerGroupService.removePlayer(player);
		}
		TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
	}

	@Override
	public boolean onReviveEvent(Player player) {
		player.getGameStats().updateStatsAndSpeedVisually();
		PlayerReviveService.revive(player, 100, 100, false, 0);
		PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_REBIRTH_MASSAGE_ME);
		PacketSendUtility.sendPacket(player,
			new SM_QUESTION_WINDOW(SM_QUESTION_WINDOW.STR_INSTANT_DUNGEON_RESURRECT, 0, 0));
		for (Player member : instance.getPlayersInside()) {
			PacketSendUtility.sendPacket(member, member == player
				? new SM_SYSTEM_MESSAGE(1402910)
				: new SM_SYSTEM_MESSAGE(1402911, player.getName()));
		}
		return TeleportService2.teleportTo(player, mapId, instanceId,
			196.80058f, 264.41388f, 97.461075f, (byte) 0);
	}

	@Override
	public void onInstanceDestroy() {
		cancelDeadline("prepare");
		cancelDeadline("expire");
		if (instanceReward != null) {
			instanceReward.clear();
		}
	}

	private void startPrepareTimer() {
		if (runtimeState().getLong(state("start_at"), 0) > 0) {
			sendScore(0, 0);
			return;
		}
		long deadline = runtimeState().getLong(state("prepare_deadline"), 0);
		if (deadline == 0) {
			deadline = System.currentTimeMillis()
				+ InstanceSettlementService.timeAttackWaitSeconds(mapId) * 1_000L;
			runtimeState().put(state("prepare_deadline"), deadline);
		}
		long prepareDeadline = deadline;
		scheduleDeadline("prepare", deadline, () -> startMainTimer(prepareDeadline));
		sendScore(0, 0);
	}

	private synchronized void startMainTimer(long startAt) {
		if (runtimeState().getLong(state("start_at"), 0) > 0
			|| runtimeState().getBoolean(state("completed"), false)) {
			return;
		}
		cancelDeadline("prepare");
		long deadline = startAt + InstanceSettlementService.timeAttackLimitSeconds(mapId) * 1_000L;
		runtimeState().put(state("start_at"), startAt);
		runtimeState().put(state("expire_deadline"), deadline);
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		sendScore(0, 0);
		scheduleDeadline("expire", deadline, () -> completeInstance(deadline));
	}

	private synchronized void completeInstance(long finishAt) {
		if (runtimeState().getBoolean(state("completed"), false)) {
			return;
		}
		long startAt = runtimeState().getLong(state("start_at"), finishAt);
		int rank = checkRank(instanceReward.getPoints(), startAt, finishAt);
		instanceReward.setRank(rank);
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		runtimeState().put(state("finish_at"), finishAt);
		runtimeState().put(state("rank"), rank);
		runtimeState().put(state("completed"), true);
		cancelDeadline("prepare");
		cancelDeadline("expire");
		for (Player player : instance.getPlayersInside()) {
			doReward(player);
		}
		sendScore(0, 0);
	}

	private void restoreDeadline() {
		if (runtimeState().getBoolean(state("completed"), false)) {
			return;
		}
		long expire = runtimeState().getLong(state("expire_deadline"), 0);
		if (runtimeState().getLong(state("start_at"), 0) > 0 && expire > 0) {
			scheduleDeadline("expire", expire, () -> completeInstance(expire));
			return;
		}
		long prepare = runtimeState().getLong(state("prepare_deadline"), 0);
		if (prepare > 0) {
			scheduleDeadline("prepare", prepare, () -> startMainTimer(prepare));
		}
	}

	private synchronized boolean consumeNpcScore(Npc npc, int points) {
		if (!instanceReward.isStartProgress() || points == 0) {
			return false;
		}
		String stableKey = npc.getSpawn() == null ? null : npc.getSpawn().getStableKey();
		String eventKey = scoreEventKey(stableKey, npc.getObjectId());
		if (runtimeState().getBoolean(eventKey, false)) {
			return true;
		}
		runtimeState().put(eventKey, true);
		instanceReward.addPoints(points);
		instanceReward.addNpcKill();
		runtimeState().put(state("points"), instanceReward.getPoints());
		runtimeState().put(state("kills"), instanceReward.getNpcKills());
		sendScore(npc.getObjectTemplate().getNameId(), points);
		return true;
	}

	private int checkRank(int totalPoints, long startAt, long finishAt) {
		return InstanceSettlementService.timeAttackRank(mapId, totalPoints,
			Math.max(0, finishAt - startAt) / 1_000L);
	}

	static String scoreEventKey(String stableKey, int objectId) {
		return STATE_PREFIX + "score.event."
			+ (stableKey == null || stableKey.isBlank() ? "object." + objectId : stableKey);
	}

	private void restoreScore() {
		int baseScore = DataManager.RETAIL_INSTANCE_DATA.rewards("world_timeattack").stream()
			.filter(row -> row.requiredInt("world_id") == mapId)
			.findFirst().map(row -> row.intValue("base_score", 0)).orElse(0);
		instanceReward.restore(runtimeState().getInt(state("points"), baseScore),
			runtimeState().getInt(state("kills"), 0), runtimeState().getInt(state("rank"), 7));
	}

	private InstanceScoreType scoreType() {
		if (runtimeState().getBoolean(state("completed"), false)) {
			return InstanceScoreType.END_PROGRESS;
		}
		return runtimeState().getLong(state("start_at"), 0) > 0
			? InstanceScoreType.START_PROGRESS : InstanceScoreType.PREPARING;
	}

	private int getTime() {
		if (runtimeState().getBoolean(state("completed"), false)) {
			return 0;
		}
		long now = System.currentTimeMillis();
		long deadline = runtimeState().getLong(state("start_at"), 0) > 0
			? runtimeState().getLong(state("expire_deadline"), now)
			: runtimeState().getLong(state("prepare_deadline"), now);
		return (int) Math.min(Integer.MAX_VALUE, Math.max(0, deadline - now));
	}

	private void sendScore(int nameId, int points) {
		for (Player player : instance.getPlayersInside()) {
			if (nameId != 0) {
				PacketSendUtility.sendPacket(player,
					new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(nameId * 2 + 1), points));
			}
			PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(getTime(), instanceReward, null));
		}
	}

	private StonespearReachPlayerReward getOrCreatePlayerReward(int playerId) {
		StonespearReachPlayerReward playerReward =
			(StonespearReachPlayerReward) instanceReward.getPlayerReward(playerId);
		if (playerReward == null) {
			playerReward = new StonespearReachPlayerReward(playerId);
			if (runtimeState().getBoolean(playerRewardKey(playerId), false)) {
				playerReward.setRewarded();
			}
			instanceReward.addPlayerReward(playerReward);
		}
		return playerReward;
	}

	private static String state(String suffix) {
		return STATE_PREFIX + suffix;
	}

	private static String playerRewardKey(int playerId) {
		return state("player." + playerId + ".rewarded");
	}
}
