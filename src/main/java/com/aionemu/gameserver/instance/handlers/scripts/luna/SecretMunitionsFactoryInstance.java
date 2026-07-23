package com.aionemu.gameserver.instance.handlers.scripts.luna;

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
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.SecretMunitionsFactoryReward;
import com.aionemu.gameserver.model.instance.playerreward.SecretMunitionsFactoryPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(301640000)
public class SecretMunitionsFactoryInstance extends GeneralInstanceHandler {
	private static final String STATE = "secret_munitions_factory.";
	private static final int FINAL_BOSS = 244147;
	private static final long SETTLEMENT_DELAY = 5_000L;

	private SecretMunitionsFactoryReward instanceReward;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		instanceReward = new SecretMunitionsFactoryReward(mapId, instanceId);
		restoreScore();
		instanceReward.setInstanceScoreType(scoreType());
		restoreDeadlines();
		if (runtimeState().getBoolean(STATE + "completed", false)) {
			settlePlayers();
		}
	}

	@Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
	}

	@Override
	public void onEnterInstance(Player player) {
		getOrCreatePlayerReward(player);
		if (runtimeState().getBoolean(STATE + "completed", false)) {
			doReward(player);
			sendScore(player, 0, 0);
			return;
		}
		startPrepareTimer();
		applyEntryEffect(player);
		sendScore(player, 0, 0);
	}

	@Override
	public void onDie(Npc npc) {
		if (npc.getWorldId() != mapId || !instanceReward.getInstanceScoreType().isStartProgress()) {
			return;
		}
		var score = DataManager.RETAIL_AI_DATA == null ? null : DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId());
		if (score != null && score.scoreApplyType() == 3) {
			String key = STATE + "kill." + npc.getObjectId();
			if (runtimeState().get(key) == null) {
				runtimeState().put(key, score.value());
				instanceReward.addPoints(score.value());
				instanceReward.addNpcKill();
				sendScore(null, npc.getObjectTemplate().getNameId(), score.value());
			}
		}
		if (npc.getNpcId() == FINAL_BOSS) {
			startSettlement(System.currentTimeMillis());
		}
	}

	@Override
	public void doReward(Player player) {
		if (!runtimeState().getBoolean(STATE + "completed", false)) {
			return;
		}
		SecretMunitionsFactoryPlayerReward reward = getOrCreatePlayerReward(player);
		if (reward.isRewarded()) {
			return;
		}
		InstanceSettlementService.settleLuna(instance, player, instanceReward.getRank());
		reward.setRewarded();
		runtimeState().put(playerRewardKey(player.getObjectId()), true);
	}

	@Override
	public void onPlayerLogOut(Player player) {
		cleanupPlayer(player);
	}

	@Override
	public void onLeaveInstance(Player player) {
		cleanupPlayer(player);
		if (runtimeState().getBoolean(STATE + "completed", false)) {
			doReward(player);
		}
	}

	@Override
	public void onInstanceDestroy() {
		if (instanceReward != null) {
			instanceReward.clear();
		}
	}

	private void startPrepareTimer() {
		if (runtimeState().getLong(STATE + "start_at", 0) > 0) {
			return;
		}
		long deadline = runtimeState().getLong(STATE + "prepare_deadline", 0);
		if (deadline == 0) {
			deadline = System.currentTimeMillis() + InstanceSettlementService.timeAttackWaitSeconds(mapId) * 1000L;
			runtimeState().put(STATE + "prepare_deadline", deadline);
		}
		long prepareDeadline = deadline;
		scheduleDeadline("prepare", deadline, () -> startMainTimer(prepareDeadline));
	}

	private synchronized void startMainTimer(long startAt) {
		if (runtimeState().getLong(STATE + "start_at", 0) > 0
			|| runtimeState().getBoolean(STATE + "completed", false)) {
			return;
		}
		cancelDeadline("prepare");
		long deadline = startAt + InstanceSettlementService.timeAttackLimitSeconds(mapId) * 1000L;
		runtimeState().put(STATE + "start_at", startAt);
		runtimeState().put(STATE + "expire_deadline", deadline);
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		scheduleDeadline("expire", deadline, this::completeInstance);
		sendScore(null, 0, 0);
	}

	private void startSettlement(long finishAt) {
		if (runtimeState().getBoolean(STATE + "completed", false)) {
			return;
		}
		long deadline = runtimeState().getLong(STATE + "settle_deadline", 0);
		if (deadline == 0) {
			deadline = finishAt + SETTLEMENT_DELAY;
			runtimeState().put(STATE + "finish_at", finishAt);
			runtimeState().put(STATE + "settle_deadline", deadline);
		}
		scheduleDeadline("settle", deadline, this::completeInstance);
	}

	private synchronized void completeInstance() {
		if (runtimeState().getBoolean(STATE + "completed", false)) {
			return;
		}
		long startAt = runtimeState().getLong(STATE + "start_at", 0);
		long finishAt = runtimeState().getLong(STATE + "finish_at", 0);
		if (finishAt == 0) {
			finishAt = runtimeState().getLong(STATE + "expire_deadline", System.currentTimeMillis());
			runtimeState().put(STATE + "finish_at", finishAt);
		}
		int rank = InstanceSettlementService.timeAttackRank(mapId, instanceReward.getPoints(),
			Math.max(0, finishAt - startAt) / 1000L);
		instanceReward.setRank(rank);
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		runtimeState().put(STATE + "rank", rank);
		runtimeState().put(STATE + "completed", true);
		cancelDeadline("prepare");
		cancelDeadline("expire");
		cancelDeadline("settle");
		settlePlayers();
		sendScore(null, 0, 0);
	}

	private void restoreDeadlines() {
		if (runtimeState().getBoolean(STATE + "completed", false)) {
			return;
		}
		long settle = runtimeState().getLong(STATE + "settle_deadline", 0);
		if (settle > 0) {
			scheduleDeadline("settle", settle, this::completeInstance);
			return;
		}
		long expire = runtimeState().getLong(STATE + "expire_deadline", 0);
		if (expire > 0) {
			scheduleDeadline("expire", expire, this::completeInstance);
			return;
		}
		long prepare = runtimeState().getLong(STATE + "prepare_deadline", 0);
		if (prepare > 0) {
			scheduleDeadline("prepare", prepare, () -> startMainTimer(prepare));
		}
	}

	private void restoreScore() {
		for (String value : runtimeState().snapshot(STATE + "kill.").values()) {
			instanceReward.addPoints(Integer.parseInt(value));
			instanceReward.addNpcKill();
		}
		instanceReward.setRank(runtimeState().getInt(STATE + "rank", 7));
	}

	private InstanceScoreType scoreType() {
		if (runtimeState().getBoolean(STATE + "completed", false)) {
			return InstanceScoreType.END_PROGRESS;
		}
		return runtimeState().getLong(STATE + "start_at", 0) > 0 ? InstanceScoreType.START_PROGRESS
			: InstanceScoreType.PREPARING;
	}

	private SecretMunitionsFactoryPlayerReward getOrCreatePlayerReward(Player player) {
		return getOrCreatePlayerReward(player.getObjectId());
	}

	private SecretMunitionsFactoryPlayerReward getOrCreatePlayerReward(int playerId) {
		runtimeState().put(playerKey(playerId), true);
		SecretMunitionsFactoryPlayerReward reward =
			(SecretMunitionsFactoryPlayerReward) instanceReward.getPlayerReward(playerId);
		if (reward == null) {
			reward = new SecretMunitionsFactoryPlayerReward(playerId);
			if (runtimeState().getBoolean(playerRewardKey(playerId), false)) {
				reward.setRewarded();
			}
			instanceReward.addPlayerReward(reward);
		}
		return reward;
	}

	private String playerRewardKey(int playerId) {
		return STATE + "reward." + playerId;
	}

	private String playerKey(int playerId) {
		return STATE + "player." + playerId;
	}

	private void settlePlayers() {
		var plan = InstanceSettlementService.lunaPlan(mapId, instanceReward.getRank());
		for (String key : runtimeState().snapshot(STATE + "player.").keySet()) {
			int playerId = Integer.parseInt(key.substring((STATE + "player.").length()));
			InstanceSettlementService.queue(instance, playerId, "luna", plan);
		}
		for (Player player : instance.getPlayersInside()) {
			doReward(player);
		}
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
			PacketSendUtility.sendPacket(player,
				new SM_SYSTEM_MESSAGE(1400237, new DescriptionId(nameId * 2 + 1), points));
		}
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(remainingSeconds(), instanceReward, null));
	}

	private int remainingSeconds() {
		long startAt = runtimeState().getLong(STATE + "start_at", 0);
		if (startAt == 0) {
			return InstanceSettlementService.timeAttackWaitSeconds(mapId);
		}
		return Math.max(0, InstanceSettlementService.timeAttackLimitSeconds(mapId)
			- (int) ((System.currentTimeMillis() - startAt) / 1000L));
	}

	private void applyEntryEffect(Player player) {
		int skillId = player.getRace() == Race.ASMODIANS ? 21348 : 21347;
		GameEngineServices.skillEngine().applyEffectDirectly(skillId, player, player,
			InstanceSettlementService.timeAttackLimitSeconds(mapId) * 1000);
	}

	private void cleanupPlayer(Player player) {
		PlayerEffectController effects = player.getEffectController();
		effects.removeEffect(21347);
		effects.removeEffect(21348);
	}
}
