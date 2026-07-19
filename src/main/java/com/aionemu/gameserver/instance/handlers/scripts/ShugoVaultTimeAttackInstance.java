package com.aionemu.gameserver.instance.handlers.scripts;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.ShugoEmperorVaultReward;
import com.aionemu.gameserver.model.instance.playerreward.ShugoEmperorVaultPlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceDeadlineScheduler;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

/** Shared retail time-attack lifecycle for IDSweep and IDSweep_02. */
abstract class ShugoVaultTimeAttackInstance extends GeneralInstanceHandler {
	private static final long SETTLEMENT_DELAY = 3_000L;
	private static final int VAULT_MAP = 301400000;
	private static final int SAFE_MAP = 301590000;
	private static final int VAULT_KEY = 185000222;
	private static final int SAFE_KEY = 185000268;
	private static final String REWARD_VARIABLE = "IDSweep_Reward";
	private static final String S_REWARD_VARIABLE = "IDSweep_Reward_S";
	private static final int[] VAULT_CONSUMABLES = { 162002031, 162002032, 162002033, 162002034, 162002035,
			162002036 };
	private static final int[] SAFE_CONSUMABLES = { 162002079, 162002080, 162002081, 162002082, 162002083,
			162002084 };
	private static final int[] TRANSFORMATION_EFFECTS = { 21829, 21830, 21831, 21832, 21833, 21834 };

	private ShugoEmperorVaultReward instanceReward;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		instanceReward = new ShugoEmperorVaultReward(mapId, instanceId);
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
	public void onOpenDoor(Player player, int doorId) {
		if (doorId != 430 || runtimeState().getBoolean(state("completed"), false)) {
			return;
		}
		setDoorState(doorId, true);
		if (runtimeState().getLong(state("start_at"), 0) == 0) {
			startMainTimer(System.currentTimeMillis());
		}
	}

	@Override
	public void onDie(Npc npc) {
		var score = DataManager.RETAIL_AI_DATA == null ? null : DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId());
		if (score == null) {
			return;
		}
		if (score.scoreApplyType() != 0 || score.equalizingScore() != 0) {
			throw new IllegalStateException("Unsupported IDSweep NPC score for " + npc.getNpcId());
		}
		KillEvent kill = recordKill(npc, score.value());
		delete(npc);
		if (kill.newlyCounted()) {
			sendScore(npc.getObjectTemplate().getNameId(), score.value());
		}
		if (isFinalBoss(npc.getNpcId())) {
			startSettlement(kill.killedAt());
		}
	}

	@Override
	public void doReward(Player player) {
		if (!runtimeState().getBoolean(state("completed"), false)) {
			return;
		}
		ShugoEmperorVaultPlayerReward reward = getOrCreatePlayerReward(player.getObjectId());
		if (reward.isRewarded()) {
			return;
		}
		int rank = instanceReward.getRank();
		RewardPlan plan = InstanceSettlementService.timeAttackPlan(mapId, rank);
		reward.setRustedVaultKey(Math.toIntExact(plan.itemCount(rewardItemId())));
		InstanceSettlementService.settleTimeAttack(instance, player, rank);
		reward.setRewarded();
		runtimeState().put(playerRewardKey(player.getObjectId()), true);
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeItems(player);
		removeEffects(player);
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeItems(player);
		removeEffects(player);
	}

	@Override
	public void onInstanceDestroy() {
		InstanceDeadlineScheduler.clearTransient(instance);
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
			deadline = System.currentTimeMillis() + InstanceSettlementService.timeAttackWaitSeconds(mapId) * 1000L;
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
		long deadline = startAt + InstanceSettlementService.timeAttackLimitSeconds(mapId) * 1000L;
		runtimeState().put(state("start_at"), startAt);
		runtimeState().put(state("expire_deadline"), deadline);
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		sendScore(0, 0);
		scheduleDeadline("expire", deadline, this::completeInstance);
	}

	private void startSettlement(long finishAt) {
		if (runtimeState().getBoolean(state("completed"), false)
				|| runtimeState().getLong(state("settle_deadline"), 0) > 0) {
			return;
		}
		runtimeState().put(state("finish_at"), finishAt);
		long deadline = finishAt + SETTLEMENT_DELAY;
		runtimeState().put(state("settle_deadline"), deadline);
		scheduleDeadline("settle", deadline, this::completeInstance);
	}

	private synchronized void completeInstance() {
		if (runtimeState().getBoolean(state("completed"), false)) {
			return;
		}
		long startAt = runtimeState().getLong(state("start_at"), 0);
		long finishAt = runtimeState().getLong(state("finish_at"), 0);
		if (finishAt == 0) {
			finishAt = runtimeState().getLong(state("expire_deadline"), System.currentTimeMillis());
			runtimeState().put(state("finish_at"), finishAt);
		}
		if (startAt == 0) {
			startAt = finishAt;
		}
		int rank = InstanceSettlementService.timeAttackRank(mapId, instanceReward.getPoints(),
				Math.max(0, finishAt - startAt) / 1000);
		instanceReward.setRank(rank);
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		runtimeState().put(state("rank"), rank);
		runtimeState().put(state("completed"), true);
		setRewardCondition(rank);
		cancelDeadline("prepare");
		cancelDeadline("expire");
		cancelDeadline("settle");
		despawnScoredNpcs();
		for (Player player : instance.getPlayersInside()) {
			doReward(player);
		}
		sendScore(0, 0);
	}

	private void restoreDeadline() {
		if (runtimeState().getBoolean(state("completed"), false)) {
			return;
		}
		long settlement = runtimeState().getLong(state("settle_deadline"), 0);
		if (settlement > 0) {
			scheduleDeadline("settle", settlement, this::completeInstance);
			return;
		}
		long finishAt = finalBossKilledAt();
		if (finishAt > 0) {
			startSettlement(finishAt);
			return;
		}
		long expire = runtimeState().getLong(state("expire_deadline"), 0);
		if (runtimeState().getLong(state("start_at"), 0) > 0 && expire > 0) {
			scheduleDeadline("expire", expire, this::completeInstance);
			return;
		}
		long prepare = runtimeState().getLong(state("prepare_deadline"), 0);
		if (prepare > 0) {
			scheduleDeadline("prepare", prepare, () -> startMainTimer(prepare));
		}
	}

	private void setRewardCondition(int rank) {
		String variable = mapId == SAFE_MAP && rank == 1 ? S_REWARD_VARIABLE : REWARD_VARIABLE;
		if (!RetailConditionSpawnEngine.setVariable(instance, variable, 1, 0)) {
			throw new IllegalStateException("Missing IDSweep reward condition variable " + variable + " for world " + mapId);
		}
	}

	private KillEvent recordKill(Npc npc, int retailScore) {
		String key = state("kill." + npc.getObjectId());
		String existing = runtimeState().get(key);
		if (existing != null) {
			return KillEvent.decode(existing).duplicate();
		}
		boolean counted = instanceReward.getInstanceScoreType().isStartProgress();
		KillEvent event = new KillEvent(counted ? retailScore : 0, counted, System.currentTimeMillis(), npc.getNpcId(),
				true);
		runtimeState().put(key, event.encode());
		if (counted) {
			instanceReward.addPoints(retailScore);
			instanceReward.addNpcKill();
		}
		return event;
	}

	private void restoreScore() {
		int points = 0;
		int kills = 0;
		for (String value : runtimeState().snapshot(state("kill.")).values()) {
			KillEvent event = KillEvent.decode(value);
			points = Math.addExact(points, event.score());
			if (event.wasCounted()) {
				kills++;
			}
		}
		instanceReward.restore(points, kills, runtimeState().getInt(state("rank"), 7));
	}

	private long finalBossKilledAt() {
		long result = 0;
		for (String value : runtimeState().snapshot(state("kill.")).values()) {
			KillEvent event = KillEvent.decode(value);
			if (isFinalBoss(event.npcId())) {
				result = Math.max(result, event.killedAt());
			}
		}
		return result;
	}

	private void despawnScoredNpcs() {
		for (Npc npc : npcs()) {
			if (DataManager.RETAIL_AI_DATA != null && DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId()) != null) {
				delete(npc);
			}
		}
	}

	private List<Npc> npcs() {
		List<Npc> result = new ArrayList<>();
		for (var iterator = instance.objectIterator(); iterator.hasNext();) {
			VisibleObject object = iterator.next();
			if (object instanceof Npc npc) {
				result.add(npc);
			}
		}
		return result;
	}

	private ShugoEmperorVaultPlayerReward getOrCreatePlayerReward(int playerId) {
		ShugoEmperorVaultPlayerReward reward = (ShugoEmperorVaultPlayerReward) instanceReward.getPlayerReward(playerId);
		if (reward == null) {
			reward = new ShugoEmperorVaultPlayerReward(playerId);
			if (runtimeState().getBoolean(playerRewardKey(playerId), false)) {
				reward.setRewarded();
				reward.setRustedVaultKey(Math.toIntExact(InstanceSettlementService
						.timeAttackPlan(mapId, instanceReward.getRank()).itemCount(rewardItemId())));
			}
			instanceReward.addPlayerReward(reward);
		}
		return reward;
	}

	private InstanceScoreType scoreType() {
		if (runtimeState().getBoolean(state("completed"), false)) {
			return InstanceScoreType.END_PROGRESS;
		}
		return runtimeState().getLong(state("start_at"), 0) > 0 ? InstanceScoreType.START_PROGRESS
				: InstanceScoreType.PREPARING;
	}

	private int getTime() {
		if (runtimeState().getBoolean(state("completed"), false)) {
			return 0;
		}
		long deadline = runtimeState().getLong(state("start_at"), 0) > 0
				? runtimeState().getLong(state("expire_deadline"), 0)
				: runtimeState().getLong(state("prepare_deadline"), 0);
		return (int) Math.max(0, deadline - System.currentTimeMillis());
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

	private void removeItems(Player player) {
		Storage inventory = player.getInventory();
		int[] consumables = mapId == SAFE_MAP ? SAFE_CONSUMABLES : VAULT_CONSUMABLES;
		int key = rewardItemId();
		inventory.decreaseByItemId(key, inventory.getItemCountByItemId(key));
		for (int itemId : consumables) {
			inventory.decreaseByItemId(itemId, inventory.getItemCountByItemId(itemId));
		}
	}

	private static void removeEffects(Player player) {
		PlayerEffectController effects = player.getEffectController();
		for (int skillId : TRANSFORMATION_EFFECTS) {
			effects.removeEffect(skillId);
		}
	}

	private int rewardItemId() {
		return mapId == SAFE_MAP ? SAFE_KEY : VAULT_KEY;
	}

	private boolean isFinalBoss(int npcId) {
		return mapId == VAULT_MAP ? npcId == 235647 : npcId == 246773 || npcId == 244061;
	}

	private String state(String suffix) {
		return (mapId == SAFE_MAP ? "idsweep02." : "idsweep.") + suffix;
	}

	private String playerRewardKey(int playerId) {
		return state("player." + playerId + ".rewarded");
	}

	private static void delete(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}

	private record KillEvent(int score, boolean wasCounted, long killedAt, int npcId, boolean newlyCounted) {
		private String encode() {
			return score + ":" + wasCounted + ":" + killedAt + ":" + npcId;
		}

		private KillEvent duplicate() {
			return new KillEvent(score, wasCounted, killedAt, npcId, false);
		}

		private static KillEvent decode(String value) {
			String[] parts = value.split(":", -1);
			if (parts.length != 4) {
				throw new IllegalStateException("Invalid IDSweep kill event");
			}
			return new KillEvent(Integer.parseInt(parts[0]), Boolean.parseBoolean(parts[1]), Long.parseLong(parts[2]),
					Integer.parseInt(parts[3]), false);
		}
	}
}
