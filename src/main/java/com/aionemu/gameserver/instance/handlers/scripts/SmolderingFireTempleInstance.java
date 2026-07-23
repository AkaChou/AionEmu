package com.aionemu.gameserver.instance.handlers.scripts;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.SmolderingReward;
import com.aionemu.gameserver.model.instance.playerreward.SmolderingPlayerReward;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(302000000)
public class SmolderingFireTempleInstance extends GeneralInstanceHandler {
	private static final long SETTLEMENT_DELAY = 3_000L;

	private SmolderingReward instanceReward;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		RetailConditionSpawnEngine.initialize(instance);
		instanceReward = new SmolderingReward(mapId, instanceId);
		restoreScore();
		instanceReward.setInstanceScoreType(scoreType());
		restoreKilledNpcs();
		reconcileProgress();
		if (runtimeState().getBoolean("smolder.completed", false)) {
			despawnScoredNpcs();
		}
		restoreDeadline();
	}

	@Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
	}

	@Override
	public void onDropRegistered(Npc npc) {
		Set<DropItem> drops = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		if (npcId == 244435) {
			int index = drops.size() + 1;
			for (Player player : instance.getPlayersInside()) {
				if (!player.isOnline()) {
					continue;
				}
				for (int itemId = 162002085; itemId <= 162002090; itemId++) {
					drops.add(GameWorldServices.dropRegistrationService()
							.regDropItem(index++, player.getObjectId(), npcId, itemId, 2));
				}
			}
			return;
		}
		if (npcId != 834058 && npcId != 834212) {
			return;
		}
		String key = "smolder.chest_drop." + positionKey(npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ());
		int roll = runtimeState().getInt(key, 0);
		if (roll == 0) {
			roll = Rnd.get(1, 4);
			runtimeState().put(key, roll);
		}
		int itemId = switch (roll) {
			case 1 -> 188054631;
			case 2 -> 188054632;
			case 3 -> 188054629;
			default -> 188054630;
		};
		drops.add(GameWorldServices.dropRegistrationService().regDropItem(1, 0, npcId, itemId, 1));
	}

	@Override
	public void onDie(Npc npc) {
		var score = DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId());
		if (score == null) {
			return;
		}
		if (score.scoreApplyType() != 0 || score.equalizingScore() != 0) {
			throw new IllegalStateException("Unsupported Smoldering Fire Temple NPC score for " + npc.getNpcId());
		}
		KillEvent kill = recordKill(npc, score.value());
		delete(npc);
		switch (npc.getNpcId()) {
			case 244095, 245198 -> setDoorState(8, true);
			case 244100, 245203 -> startSettlement(kill.killedAt());
			default -> { }
		}
		reconcileProgress();
		if (kill.newlyCounted()) {
			sendScore(npc.getObjectTemplate().getNameId(), score.value());
		}
	}

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		PlayerEffectController effects = player.getEffectController();
		switch (npc.getNpcId()) {
			case 834055 -> useTransformation(player, npc, effects, 21375, 21378);
			case 834056 -> useTransformation(player, npc, effects, 21376, 21379);
			case 834057 -> useTransformation(player, npc, effects, 21377, 21380);
		}
	}

	@Override
	public void onOpenDoor(Player player, int doorId) {
		if (doorId != 2) {
			return;
		}
		setDoorState(2, true);
		sendSystemMessage(1401181);
		if (runtimeState().getLong("smolder.start_at", 0) == 0
				&& !runtimeState().getBoolean("smolder.completed", false)) {
			startMainTimer(System.currentTimeMillis());
		}
	}

	@Override
	public void onEnterInstance(Player player) {
		SmolderingPlayerReward reward = getOrCreatePlayerReward(player.getObjectId());
		if (runtimeState().getBoolean("smolder.completed", false)) {
			if (!reward.isRewarded()) {
				doReward(player);
			}
			sendScore(0, 0);
			return;
		}
		startPrepareTimer();
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(getTime(), instanceReward, null));
	}

	@Override
	public void doReward(Player player) {
		if (!runtimeState().getBoolean("smolder.completed", false)) {
			return;
		}
		SmolderingPlayerReward reward = getOrCreatePlayerReward(player.getObjectId());
		if (reward.isRewarded()) {
			return;
		}
		RewardPlan plan = InstanceSettlementService.timeAttackPlan(mapId, instanceReward.getRank());
		reward.setSmolderingKey(Math.toIntExact(plan.itemCount(185000270)));
		InstanceSettlementService.settleTimeAttack(instance, player, instanceReward.getRank());
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
		if (instanceReward != null) {
			instanceReward.clear();
		}
	}

	private void startPrepareTimer() {
		if (runtimeState().getLong("smolder.start_at", 0) > 0) {
			return;
		}
		long deadline = runtimeState().getLong("smolder.prepare_deadline", 0);
		if (deadline == 0) {
			deadline = System.currentTimeMillis() + InstanceSettlementService.timeAttackWaitSeconds(mapId) * 1000L;
			runtimeState().put("smolder.prepare_deadline", deadline);
		}
		long prepareDeadline = deadline;
		scheduleDeadline("prepare", deadline, () -> startMainTimer(prepareDeadline));
	}

	private synchronized void startMainTimer(long startAt) {
		if (runtimeState().getLong("smolder.start_at", 0) > 0
				|| runtimeState().getBoolean("smolder.completed", false)) {
			return;
		}
		cancelDeadline("prepare");
		long deadline = startAt + InstanceSettlementService.timeAttackLimitSeconds(mapId) * 1000L;
		runtimeState().put("smolder.start_at", startAt);
		runtimeState().put("smolder.expire_deadline", deadline);
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		sendScore(0, 0);
		scheduleDeadline("expire", deadline, this::completeInstance);
	}

	private void startSettlement(long finishAt) {
		if (runtimeState().getLong("smolder.settle_deadline", 0) > 0) {
			return;
		}
		runtimeState().put("smolder.finish_at", finishAt);
		long deadline = finishAt + SETTLEMENT_DELAY;
		runtimeState().put("smolder.settle_deadline", deadline);
		scheduleDeadline("settle", deadline, this::completeInstance);
	}

	private synchronized void completeInstance() {
		if (runtimeState().getBoolean("smolder.completed", false)) {
			return;
		}
		long startAt = runtimeState().getLong("smolder.start_at", 0);
		long finishAt = runtimeState().getLong("smolder.finish_at", 0);
		if (finishAt == 0) {
			finishAt = runtimeState().getLong("smolder.expire_deadline", System.currentTimeMillis());
			runtimeState().put("smolder.finish_at", finishAt);
		}
		int rank = checkRank(instanceReward.getPoints(), startAt, finishAt);
		instanceReward.setRank(rank);
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		runtimeState().put("smolder.rank", rank);
		runtimeState().put("smolder.completed", true);
		if (!RetailConditionSpawnEngine.setVariable(instance, "IDDF2_Dflame_Event_Reward", 1, 0)) {
			throw new IllegalStateException("Missing Smoldering Fire Temple reward condition variable");
		}
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
		if (runtimeState().getBoolean("smolder.completed", false)) {
			return;
		}
		long finalBossKilledAt = Math.max(killTime(244100), killTime(245203));
		if (finalBossKilledAt > 0) {
			long settlement = runtimeState().getLong("smolder.settle_deadline", 0);
			if (settlement == 0) {
				settlement = finalBossKilledAt + SETTLEMENT_DELAY;
				runtimeState().put("smolder.finish_at", finalBossKilledAt);
				runtimeState().put("smolder.settle_deadline", settlement);
			}
			scheduleDeadline("settle", settlement, this::completeInstance);
			return;
		}
		long expire = runtimeState().getLong("smolder.expire_deadline", 0);
		if (runtimeState().getLong("smolder.start_at", 0) > 0 && expire > 0) {
			scheduleDeadline("expire", expire, this::completeInstance);
			return;
		}
		long prepare = runtimeState().getLong("smolder.prepare_deadline", 0);
		if (prepare > 0) {
			scheduleDeadline("prepare", prepare, () -> startMainTimer(prepare));
		}
	}

	private void restoreKilledNpcs() {
		for (Npc npc : npcs()) {
			if (hasKillEvent(npc)) {
				delete(npc);
			}
		}
	}

	private void reconcileProgress() {
		if (killCount(244095) > 0 || killCount(245198) > 0) {
			setDoorState(8, true);
		}
	}

	private void restoreScore() {
		int points = 0;
		int kills = 0;
		for (String value : runtimeState().snapshot("smolder.kill.").values()) {
			KillEvent event = KillEvent.decode(value);
			points = Math.addExact(points, event.score());
			if (event.wasCounted()) {
				kills++;
			}
		}
		instanceReward.restore(points, kills, runtimeState().getInt("smolder.rank", 7));
	}

	private KillEvent recordKill(Npc npc, int retailScore) {
		String key = killKey(npc);
		String existing = runtimeState().get(key);
		if (existing != null) {
			return KillEvent.decode(existing).duplicate();
		}
		boolean counted = instanceReward.getInstanceScoreType().isStartProgress();
		KillEvent event = new KillEvent(counted ? retailScore : 0, counted, System.currentTimeMillis(), true, counted);
		runtimeState().put(key, event.encode());
		if (counted) {
			instanceReward.addPoints(retailScore);
			instanceReward.addNpcKill();
		}
		return event;
	}

	private int killCount(int npcId) {
		return runtimeState().snapshot("smolder.kill." + npcId + '.').size();
	}

	private long killTime(int npcId) {
		long killedAt = 0;
		for (String value : runtimeState().snapshot("smolder.kill." + npcId + '.').values()) {
			killedAt = Math.max(killedAt, KillEvent.decode(value).killedAt());
		}
		return killedAt;
	}

	private boolean hasKillEvent(Npc npc) {
		return runtimeState().get(killKey(npc)) != null;
	}

	private void despawnScoredNpcs() {
		for (Npc npc : npcs()) {
			if (DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId()) != null) {
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

	private SmolderingPlayerReward getOrCreatePlayerReward(int playerId) {
		SmolderingPlayerReward reward = (SmolderingPlayerReward) instanceReward.getPlayerReward(playerId);
		if (reward == null) {
			reward = new SmolderingPlayerReward(playerId);
			if (runtimeState().getBoolean(playerRewardKey(playerId), false)) {
				reward.setRewarded();
				reward.setSmolderingKey(Math.toIntExact(InstanceSettlementService
						.timeAttackPlan(mapId, instanceReward.getRank()).itemCount(185000270)));
			}
			instanceReward.addPlayerReward(reward);
		}
		return reward;
	}

	private InstanceScoreType scoreType() {
		if (runtimeState().getBoolean("smolder.completed", false)) {
			return InstanceScoreType.END_PROGRESS;
		}
		return runtimeState().getLong("smolder.start_at", 0) > 0
				? InstanceScoreType.START_PROGRESS : InstanceScoreType.PREPARING;
	}

	private int checkRank(int totalPoints, long startAt, long finishAt) {
		return InstanceSettlementService.timeAttackRank(mapId, totalPoints,
				Math.max(0, finishAt - startAt) / 1000);
	}

	private int getTime() {
		if (runtimeState().getBoolean("smolder.completed", false)) {
			return 0;
		}
		long deadline = runtimeState().getLong("smolder.start_at", 0) > 0
				? runtimeState().getLong("smolder.expire_deadline", 0)
				: runtimeState().getLong("smolder.prepare_deadline", 0);
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

	private void sendSystemMessage(int messageId) {
		for (Player player : instance.getPlayersInside()) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(messageId));
		}
	}

	private static void useTransformation(Player player, Npc npc, PlayerEffectController effects,
			int elyosSkill, int asmodianSkill) {
		removeEffects(player);
		int skillId = player.getRace() == Race.ELYOS ? elyosSkill : asmodianSkill;
		GameEngineServices.skillEngine().getSkill(npc, skillId, 1, player).useNoAnimationSkill();
	}

	private static void removeItems(Player player) {
		Storage inventory = player.getInventory();
		for (int itemId = 162002085; itemId <= 162002090; itemId++) {
			inventory.decreaseByItemId(itemId, inventory.getItemCountByItemId(itemId));
		}
		inventory.decreaseByItemId(185000270, inventory.getItemCountByItemId(185000270));
	}

	private static void removeEffects(Player player) {
		PlayerEffectController effects = player.getEffectController();
		for (int skillId = 21375; skillId <= 21380; skillId++) {
			effects.removeEffect(skillId);
		}
	}

	private static String playerRewardKey(int playerId) {
		return "smolder.player." + playerId + ".rewarded";
	}

	private static String killKey(Npc npc) {
		return killKey(npc.getNpcId(), npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ());
	}

	private static String killKey(int npcId, float x, float y, float z) {
		return "smolder.kill." + npcId + '.' + positionKey(x, y, z);
	}

	private static String positionKey(float x, float y, float z) {
		return Integer.toUnsignedString(Float.floatToIntBits(x)) + '_'
				+ Integer.toUnsignedString(Float.floatToIntBits(y)) + '_'
				+ Integer.toUnsignedString(Float.floatToIntBits(z));
	}

	private static void delete(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}

	private record KillEvent(int score, boolean wasCounted, long killedAt, boolean newlyRecorded,
			boolean newlyCounted) {
		private String encode() {
			return score + ":" + wasCounted + ":" + killedAt;
		}

		private KillEvent duplicate() {
			return new KillEvent(score, wasCounted, killedAt, false, false);
		}

		private static KillEvent decode(String value) {
			String[] parts = value.split(":", -1);
			if (parts.length != 3) {
				throw new IllegalStateException("Invalid Smoldering Fire Temple kill event");
			}
			return new KillEvent(Integer.parseInt(parts[0]), Boolean.parseBoolean(parts[1]),
					Long.parseLong(parts[2]), false, false);
		}
	}
}
