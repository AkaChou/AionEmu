package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.EternalBastionReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.playerreward.EternalBastionPlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 永恒堡垒副本事件处理器。
 * Instance event handler for The Eternal Bastion.
 */
@InstanceID(300540000)
public class TheEternalBastionInstance extends GeneralInstanceHandler {
	private static final String STATE_PREFIX = "eternal.";
	private static final long EXIT_DELAY = 5_000L;
	private EternalBastionReward instanceReward;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		instanceReward = new EternalBastionReward(mapId, instanceId);
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
		EternalBastionPlayerReward playerReward = getOrCreatePlayerReward(player.getObjectId());
		if (runtimeState().getBoolean(STATE_PREFIX + "completed", false)) {
			if (!playerReward.isRewarded()) {
				doReward(player);
			}
			sendScore(0, 0);
			return;
		}
		startPrepareTimer();
		sendScore(0, 0);
	}

	@Override
	public void onDie(Npc npc) {
		var score = DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId());
		if (score != null && !supportsRetailNpcScore(npc.getNpcId(), score.scoreApplyType())
			&& score.equalizingScore() == 0 && instanceReward.isStartProgress()) {
			applyRetailScore(npc, score.value());
		}
		switch (npc.getNpcId()) {
			case 231130 -> finishInstance(true);
			case 209516, 209517 -> finishInstance(false);
			default -> {
			}
		}
	}

	@Override
	public boolean supportsRetailNpcScore(int npcId, int scoreApplyType) {
		return scoreApplyType == 3;
	}

	@Override
	public boolean onRetailNpcScore(Player player, Npc npc, int scoreApplyType, int value) {
		if (!supportsRetailNpcScore(npc.getNpcId(), scoreApplyType) || !instanceReward.isStartProgress()) {
			return false;
		}
		applyRetailScore(npc, value);
		return true;
	}

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 701625 -> {
				delete(npc);
				GameEngineServices.skillEngine().getSkill(npc, 21065, 60, player).useNoAnimationSkill();
			}
			case 701922 -> {
				delete(npc);
				GameEngineServices.skillEngine().getSkill(npc, 21066, 60, player).useNoAnimationSkill();
			}
			default -> {
			}
		}
	}

	@Override
	public void doReward(Player player) {
		if (!runtimeState().getBoolean(STATE_PREFIX + "completed", false)) {
			return;
		}
		EternalBastionPlayerReward playerReward = getOrCreatePlayerReward(player.getObjectId());
		if (playerReward.isRewarded()) {
			return;
		}
		RewardPlan plan = InstanceSettlementService.timeAttackPlan(mapId, instanceReward.getRank());
		playerReward.setScoreAP(plan.ap());
		playerReward.setHighestGradeMaterialBox(Math.toIntExact(plan.itemCount(188052594)));
		playerReward.setHighGradeMaterialBox(Math.toIntExact(plan.itemCount(188052595)));
		playerReward.setHighestGradeMaterialSupportBundle(Math.toIntExact(plan.itemCount(188052596)));
		playerReward.setHighGradeMaterialSupportBundle(Math.toIntExact(plan.itemCount(188052597)));
		playerReward.setLowGradeMaterialSupportBundle(Math.toIntExact(plan.itemCount(188052598)));
		InstanceSettlementService.settleTimeAttack(instance, player, instanceReward.getRank());
		playerReward.setRewarded();
		runtimeState().put(playerRewardKey(player.getObjectId()), true);
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
		removeEffects(player);
		if (player.isInGroup2()) {
			PlayerGroupService.removePlayer(player);
		}
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
		removeEffects(player);
	}

	@Override
	public void onInstanceDestroy() {
		cancelDeadline("prepare");
		cancelDeadline("expire");
		cancelDeadline("exit");
		if (instanceReward != null) {
			instanceReward.clear();
		}
	}

	private void startPrepareTimer() {
		if (runtimeState().getLong(STATE_PREFIX + "start_at", 0) > 0
			|| runtimeState().getBoolean(STATE_PREFIX + "completed", false)) {
			return;
		}
		long deadline = runtimeState().getLong(STATE_PREFIX + "prepare_deadline", 0);
		if (deadline == 0) {
			deadline = System.currentTimeMillis() + InstanceSettlementService.timeAttackWaitSeconds(mapId) * 1_000L;
			runtimeState().put(STATE_PREFIX + "prepare_deadline", deadline);
		}
		long prepareDeadline = deadline;
		scheduleDeadline("prepare", deadline, () -> startMainTimer(prepareDeadline));
	}

	private synchronized void startMainTimer(long startAt) {
		if (runtimeState().getLong(STATE_PREFIX + "start_at", 0) > 0
			|| runtimeState().getBoolean(STATE_PREFIX + "completed", false)) {
			return;
		}
		cancelDeadline("prepare");
		long deadline = startAt + InstanceSettlementService.timeAttackLimitSeconds(mapId) * 1_000L;
		runtimeState().put(STATE_PREFIX + "start_at", startAt);
		runtimeState().put(STATE_PREFIX + "expire_deadline", deadline);
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		sendScore(0, 0);
		scheduleDeadline("expire", deadline, () -> finishInstance(false));
	}

	private synchronized void finishInstance(boolean success) {
		if (runtimeState().getBoolean(STATE_PREFIX + "completed", false)) {
			return;
		}
		long startAt = runtimeState().getLong(STATE_PREFIX + "start_at", 0);
		long finishAt = System.currentTimeMillis();
		int rank = success && startAt > 0
			? checkRank(instanceReward.getPoints(), startAt, finishAt)
			: 7;
		instanceReward.setRank(rank);
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		runtimeState().put(STATE_PREFIX + "finish_at", finishAt);
		runtimeState().put(STATE_PREFIX + "rank", rank);
		runtimeState().put(STATE_PREFIX + "completed", true);
		cancelDeadline("prepare");
		cancelDeadline("expire");
		for (Player player : instance.getPlayersInside()) {
			doReward(player);
		}
		sendScore(0, 0);
		long exitDeadline = finishAt + EXIT_DELAY;
		runtimeState().put(STATE_PREFIX + "exit_deadline", exitDeadline);
		scheduleDeadline("exit", exitDeadline, this::exitPlayers);
	}

	private int checkRank(int totalPoints, long startAt, long finishAt) {
		return InstanceSettlementService.timeAttackRank(mapId, totalPoints,
			Math.max(0, finishAt - startAt) / 1_000L);
	}

	private void restoreDeadline() {
		if (runtimeState().getBoolean(STATE_PREFIX + "completed", false)) {
			long exit = runtimeState().getLong(STATE_PREFIX + "exit_deadline", 0);
			if (exit > 0) {
				scheduleDeadline("exit", exit, this::exitPlayers);
			}
			return;
		}
		long expire = runtimeState().getLong(STATE_PREFIX + "expire_deadline", 0);
		if (expire > 0) {
			scheduleDeadline("expire", expire, () -> finishInstance(false));
			return;
		}
		long prepare = runtimeState().getLong(STATE_PREFIX + "prepare_deadline", 0);
		if (prepare > 0) {
			scheduleDeadline("prepare", prepare, () -> startMainTimer(prepare));
		}
	}

	private void exitPlayers() {
		if (instance == null) {
			return;
		}
		for (Player player : instance.getPlayersInside()) {
			onLeaveInstance(player);
		}
	}

	private void applyRetailScore(Npc npc, int value) {
		instanceReward.addPoints(value);
		instanceReward.addNpcKill();
		runtimeState().put(STATE_PREFIX + "points", instanceReward.getPoints());
		runtimeState().put(STATE_PREFIX + "kills", instanceReward.getNpcKills());
		sendScore(npc.getObjectTemplate().getNameId(), value);
	}

	private void restoreScore() {
		int baseScore = DataManager.RETAIL_INSTANCE_DATA.rewards("world_timeattack").stream()
			.filter(row -> row.requiredInt("world_id") == mapId)
			.findFirst().map(row -> row.intValue("base_score", 0)).orElse(0);
		instanceReward.restore(runtimeState().getInt(STATE_PREFIX + "points", baseScore),
			runtimeState().getInt(STATE_PREFIX + "kills", 0), runtimeState().getInt(STATE_PREFIX + "rank", 7));
	}

	private InstanceScoreType scoreType() {
		if (runtimeState().getBoolean(STATE_PREFIX + "completed", false)) {
			return InstanceScoreType.END_PROGRESS;
		}
		return runtimeState().getLong(STATE_PREFIX + "start_at", 0) > 0
			? InstanceScoreType.START_PROGRESS : InstanceScoreType.PREPARING;
	}

	private int getTime() {
		if (runtimeState().getBoolean(STATE_PREFIX + "completed", false)) {
			return 0;
		}
		long now = System.currentTimeMillis();
		long deadline = runtimeState().getLong(STATE_PREFIX + "start_at", 0) > 0
			? runtimeState().getLong(STATE_PREFIX + "expire_deadline", now)
			: runtimeState().getLong(STATE_PREFIX + "prepare_deadline", now);
		return (int) Math.max(0, deadline - now);
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

	private EternalBastionPlayerReward getOrCreatePlayerReward(int playerId) {
		EternalBastionPlayerReward reward = (EternalBastionPlayerReward) instanceReward.getPlayerReward(playerId);
		if (reward == null) {
			reward = new EternalBastionPlayerReward(playerId);
			if (runtimeState().getBoolean(playerRewardKey(playerId), false)) {
				reward.setRewarded();
			}
			instanceReward.addPlayerReward(reward);
		}
		return reward;
	}

	private static String playerRewardKey(int playerId) {
		return STATE_PREFIX + "player." + playerId + ".rewarded";
	}

	private static void delete(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}

	private static void removeItems(Player player) {
		Storage storage = player.getInventory();
		storage.decreaseByItemId(185000137, storage.getItemCountByItemId(185000137));
		storage.decreaseByItemId(182006996, storage.getItemCountByItemId(182006996));
		storage.decreaseByItemId(182006997, storage.getItemCountByItemId(182006997));
	}

	private static void removeEffects(Player player) {
		PlayerEffectController effects = player.getEffectController();
		effects.removeEffect(21065);
		effects.removeEffect(21066);
		effects.removeEffect(21141);
	}
}
