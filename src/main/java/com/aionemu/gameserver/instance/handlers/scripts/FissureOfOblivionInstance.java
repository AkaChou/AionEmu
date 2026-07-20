package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.FissureOfOblivionReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.FissureOfOblivionPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(302100000)
public class FissureOfOblivionInstance extends GeneralInstanceHandler {
	private static final long SETTLEMENT_DELAY = 3_000L;
	private FissureOfOblivionReward instanceReward;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		instanceReward = new FissureOfOblivionReward(mapId, instanceId);
		restoreScore();
		instanceReward.setInstanceScoreType(scoreType());
		restoreDeadlines();
	}

	@Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
	}

	@Override
	public void onEnterInstance(Player player) {
		getOrCreatePlayerReward(player);
		if (runtimeState().getBoolean("fissure.completed", false)) {
			doReward(player);
			sendScore(player, 0, 0);
			return;
		}
		startPrepareTimer();
		applyEntryEffect(player);
	}

	@Override
	public void onOpenDoor(Player player, int doorId) {
		if (doorId != 34 || runtimeState().getBoolean("fissure.completed", false)) {
			return;
		}
		setDoorState(doorId, true);
		RetailConditionSpawnEngine.setVariable(instance, "door_open", 1, 0);
		if (runtimeState().getLong("fissure.start_at", 0) == 0) {
			startMainTimer(System.currentTimeMillis());
		}
	}

	@Override
	public void onDie(Npc npc) {
		if (npc.getNpcId() == 245411) {
			startSettlement(System.currentTimeMillis());
			return;
		}
		var score = DataManager.RETAIL_AI_DATA == null ? null : DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId());
		if (score == null || npc.getWorldId() != mapId || !instanceReward.getInstanceScoreType().isStartProgress()) {
			return;
		}
		String key = "fissure.kill." + npc.getObjectId();
		if (runtimeState().get(key) != null) {
			return;
		}
		runtimeState().put(key, score.value());
		instanceReward.addPoints(score.value());
		instanceReward.addNpcKill();
		sendScore(null, npc.getObjectTemplate().getNameId(), score.value());
	}

	@Override
	public void doReward(Player player) {
		if (!runtimeState().getBoolean("fissure.completed", false)) {
			return;
		}
		FissureOfOblivionPlayerReward reward = getOrCreatePlayerReward(player);
		if (reward.isRewarded()) {
			return;
		}
		int rank = instanceReward.getRank();
		reward.setFrozenMarbleOfMemory(Math.toIntExact(InstanceSettlementService.timeAttackPlan(mapId, rank)
			.itemCount(186000448)));
		InstanceSettlementService.settleTimeAttack(instance, player, rank);
		reward.setRewarded();
		runtimeState().put(playerKey(player), true);
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeEffects(player);
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeEffects(player);
		if (runtimeState().getBoolean("fissure.completed", false)) {
			doReward(player);
		}
	}

	@Override
	public void onInstanceDestroy() {
		if (instanceReward != null) {
			instanceReward.clear();
		}
	}

	private InstanceScoreType scoreType() {
		return runtimeState().getLong("fissure.start_at", 0) > 0 ? InstanceScoreType.START_PROGRESS
			: InstanceScoreType.PREPARING;
	}

	private void startPrepareTimer() {
		if (runtimeState().getLong("fissure.start_at", 0) > 0) {
			return;
		}
		long deadline = runtimeState().getLong("fissure.prepare_deadline", 0);
		if (deadline == 0) {
			deadline = System.currentTimeMillis() + InstanceSettlementService.timeAttackWaitSeconds(mapId) * 1000L;
			runtimeState().put("fissure.prepare_deadline", deadline);
		}
		long prepareDeadline = deadline;
		scheduleDeadline("prepare", deadline, () -> startMainTimer(prepareDeadline));
		for (Player player : instance.getPlayersInside()) {
			sendScore(player, 0, 0);
		}
	}

	private synchronized void startMainTimer(long startAt) {
		if (runtimeState().getLong("fissure.start_at", 0) > 0
			|| runtimeState().getBoolean("fissure.completed", false)) {
			return;
		}
		cancelDeadline("prepare");
		long deadline = startAt + InstanceSettlementService.timeAttackLimitSeconds(mapId) * 1000L;
		runtimeState().put("fissure.start_at", startAt);
		runtimeState().put("fissure.expire_deadline", deadline);
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		scheduleDeadline("expire", deadline, this::completeInstance);
		for (Player player : instance.getPlayersInside()) {
			sendScore(player, 0, 0);
		}
	}

	private void startSettlement(long finishAt) {
		if (runtimeState().getBoolean("fissure.completed", false)) {
			return;
		}
		long deadline = runtimeState().getLong("fissure.settle_deadline", 0);
		if (deadline == 0) {
			deadline = finishAt + SETTLEMENT_DELAY;
			runtimeState().put("fissure.finish_at", finishAt);
			runtimeState().put("fissure.settle_deadline", deadline);
		}
		long settlementDeadline = deadline;
		scheduleDeadline("settle", settlementDeadline, this::completeInstance);
	}

	private synchronized void completeInstance() {
		if (runtimeState().getBoolean("fissure.completed", false)) {
			return;
		}
		long startAt = runtimeState().getLong("fissure.start_at", 0);
		long finishAt = runtimeState().getLong("fissure.finish_at", 0);
		if (finishAt == 0) {
			finishAt = runtimeState().getLong("fissure.expire_deadline", System.currentTimeMillis());
		}
		int rank = checkRank(instanceReward.getPoints(), startAt, finishAt);
		instanceReward.setRank(rank);
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		runtimeState().put("fissure.rank", rank);
		runtimeState().put("fissure.completed", true);
		cancelDeadline("prepare");
		cancelDeadline("expire");
		cancelDeadline("settle");
		for (Player player : instance.getPlayersInside()) {
			doReward(player);
			sendScore(player, 0, 0);
		}
	}

	private void restoreDeadlines() {
		if (runtimeState().getBoolean("fissure.completed", false)) {
			return;
		}
		long settle = runtimeState().getLong("fissure.settle_deadline", 0);
		if (settle > 0) {
			scheduleDeadline("settle", settle, this::completeInstance);
			return;
		}
		long expire = runtimeState().getLong("fissure.expire_deadline", 0);
		if (expire > 0) {
			scheduleDeadline("expire", expire, this::completeInstance);
			return;
		}
		long prepare = runtimeState().getLong("fissure.prepare_deadline", 0);
		if (prepare > 0) {
			scheduleDeadline("prepare", prepare, () -> startMainTimer(prepare));
		}
	}

	private int checkRank(int totalPoints, long startAt, long finishAt) {
		return InstanceSettlementService.timeAttackRank(mapId, totalPoints,
			Math.max(0, finishAt - startAt) / 1000L);
	}

	private void restoreScore() {
		for (String value : runtimeState().snapshot("fissure.kill.").values()) {
			instanceReward.addPoints(Integer.parseInt(value));
			instanceReward.addNpcKill();
		}
		instanceReward.setRank(runtimeState().getInt("fissure.rank", 7));
	}

	private FissureOfOblivionPlayerReward getOrCreatePlayerReward(Player player) {
		FissureOfOblivionPlayerReward reward = (FissureOfOblivionPlayerReward) instanceReward
			.getPlayerReward(player.getObjectId());
		if (reward == null) {
			reward = new FissureOfOblivionPlayerReward(player.getObjectId());
			if (runtimeState().getBoolean(playerKey(player), false)) {
				reward.setRewarded();
			}
			instanceReward.addPlayerReward(reward);
		}
		return reward;
	}

	private String playerKey(Player player) {
		return "fissure.reward." + player.getObjectId();
	}

	private void sendScore(Player only, int nameId, int points) {
		if (only != null) {
			sendScorePacket(only, nameId, points);
			return;
		}
		for (Player player : instance.getPlayersInside()) {
			sendScorePacket(player, nameId, points);
		}
	}

	private void sendScorePacket(Player player, int nameId, int points) {
		if (nameId != 0) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(nameId * 2 + 1), points));
		}
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(remainingSeconds(), instanceReward, null));
	}

	private int remainingSeconds() {
		long startAt = runtimeState().getLong("fissure.start_at", 0);
		if (startAt == 0) {
			return InstanceSettlementService.timeAttackWaitSeconds(mapId);
		}
		return Math.max(0, InstanceSettlementService.timeAttackLimitSeconds(mapId)
			- (int) ((System.currentTimeMillis() - startAt) / 1000L));
	}

	private void applyEntryEffect(Player player) {
		com.aionemu.gameserver.lifecycle.GameEngineServices.skillEngine().applyEffectDirectly(4831, player, player,
			InstanceSettlementService.timeAttackLimitSeconds(mapId) * 1000);
	}

	private void removeEffects(Player player) {
		PlayerEffectController effects = player.getEffectController();
		effects.removeEffect(4831);
	}
}
