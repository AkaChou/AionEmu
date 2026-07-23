package com.aionemu.gameserver.instance.handlers.scripts;

import java.util.ArrayList;
import java.util.List;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.effect.PlayerEffectController;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.instance.instancereward.SealedArgentManorReward;
import com.aionemu.gameserver.model.instance.playerreward.SealedArgentManorPlayerReward;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService.RewardPlan;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(301510000)
public class SealedArgentManorInstance extends GeneralInstanceHandler {
	private static final long SETTLEMENT_DELAY = 3_000L;
	private static final float BOSS_X = 819.55664f;
	private static final float BOSS_Y = 1420.614f;
	private static final float BOSS_Z = 194.97882f;
	private static final byte BOSS_HEADING = 30;

	private SealedArgentManorReward instanceReward;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		instanceReward = new SealedArgentManorReward(mapId, instanceId);
		restoreScore();
		instanceReward.setInstanceScoreType(scoreType());
		restoreWorldState();
		restoreDeadline();
	}

	@Override
	public InstanceReward<?> getInstanceReward() {
		return instanceReward;
	}

	@Override
	public void handleUseItemFinish(Player player, Npc npc) {
		switch (npc.getNpcId()) {
			case 701001 -> GameEngineServices.skillEngine().getSkill(npc, 19316, 60, player).useNoAnimationSkill();
			case 701002 -> GameEngineServices.skillEngine().getSkill(npc, 19317, 60, player).useNoAnimationSkill();
			case 701003 -> GameEngineServices.skillEngine().getSkill(npc, 19318, 60, player).useNoAnimationSkill();
			case 701004 -> GameEngineServices.skillEngine().getSkill(npc, 19319, 60, player).useNoAnimationSkill();
			case 856547 -> activateHetgolem(player, npc);
		}
	}

	@Override
	public void onDie(Npc npc) {
		int npcId = npc.getNpcId();
		var score = DataManager.RETAIL_AI_DATA.getNpcScore(npcId);
		if (score != null && (score.scoreApplyType() != 0 || score.equalizingScore() != 0)) {
			throw new IllegalStateException("Unsupported Sealed Argent Manor NPC score for " + npcId);
		}
		KillEvent kill = score == null ? null : recordKill(npc, score.value());
		switch (npcId) {
			case 282208 -> delete(npc);
			case 237195 -> {
				delete(npc);
				delete(getNpc(701000));
			}
			case 237193, 237194 -> finishBoss(npc);
			default -> { }
		}
		if (kill != null && kill.newlyCounted()) {
			sendScore(npc.getObjectTemplate().getNameId(), kill.score());
		}
	}

	@Override
	public void onOpenDoor(Player player, int doorId) {
		if (doorId != 14) {
			return;
		}
		setDoorState(14, true);
		if (runtimeState().getLong("sealed.start_at", 0) == 0
				&& !runtimeState().getBoolean("sealed.completed", false)) {
			startMainTimer(System.currentTimeMillis());
		}
	}

	@Override
	public void onEnterInstance(Player player) {
		SealedArgentManorPlayerReward playerReward = getOrCreatePlayerReward(player.getObjectId());
		if (runtimeState().getBoolean("sealed.completed", false)) {
			if (!playerReward.isRewarded()) {
				doReward(player);
			}
			sendScore(0, 0);
			return;
		}
		selectBoss(player);
		startPrepareTimer();
		PacketSendUtility.sendPacket(player, new SM_INSTANCE_SCORE(getTime(), instanceReward, null));
	}

	@Override
	public void doReward(Player player) {
		if (!runtimeState().getBoolean("sealed.completed", false)) {
			return;
		}
		SealedArgentManorPlayerReward playerReward = getOrCreatePlayerReward(player.getObjectId());
		if (playerReward.isRewarded()) {
			return;
		}
		RewardPlan plan = InstanceSettlementService.timeAttackPlan(mapId, instanceReward.getRank());
		applyRewardDisplay(playerReward, plan);
		InstanceSettlementService.settleTimeAttack(instance, player, instanceReward.getRank());
		playerReward.setRewarded();
		runtimeState().put(playerRewardKey(player.getObjectId()), true);
	}

	@Override
	public void onInstanceDestroy() {
		if (instanceReward != null) {
			instanceReward.clear();
		}
	}

	@Override
	public void onPlayerLogOut(Player player) {
		removeEffects(player);
	}

	@Override
	public void onLeaveInstance(Player player) {
		removeEffects(player);
	}

	private void startPrepareTimer() {
		if (runtimeState().getLong("sealed.start_at", 0) > 0) {
			return;
		}
		long deadline = runtimeState().getLong("sealed.prepare_deadline", 0);
		if (deadline == 0) {
			deadline = System.currentTimeMillis() + InstanceSettlementService.timeAttackWaitSeconds(mapId) * 1000L;
			runtimeState().put("sealed.prepare_deadline", deadline);
		}
		long prepareDeadline = deadline;
		scheduleDeadline("prepare", deadline, () -> startMainTimer(prepareDeadline));
	}

	private synchronized void startMainTimer(long startAt) {
		if (runtimeState().getLong("sealed.start_at", 0) > 0
				|| runtimeState().getBoolean("sealed.completed", false)) {
			return;
		}
		cancelDeadline("prepare");
		long deadline = startAt + InstanceSettlementService.timeAttackLimitSeconds(mapId) * 1000L;
		runtimeState().put("sealed.start_at", startAt);
		runtimeState().put("sealed.expire_deadline", deadline);
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		sendScore(0, 0);
		scheduleDeadline("expire", deadline, this::completeInstance);
	}

	private void finishBoss(Npc npc) {
		long killedAt = killTime(npc);
		if (killedAt == 0 || runtimeState().getLong("sealed.settle_deadline", 0) > 0) {
			return;
		}
		runtimeState().put("sealed.finish_at", killedAt);
		long deadline = killedAt + SETTLEMENT_DELAY;
		runtimeState().put("sealed.settle_deadline", deadline);
		delete(npc);
		scheduleDeadline("settle", deadline, this::completeInstance);
	}

	private synchronized void completeInstance() {
		if (runtimeState().getBoolean("sealed.completed", false)) {
			return;
		}
		long startAt = runtimeState().getLong("sealed.start_at", 0);
		long finishAt = runtimeState().getLong("sealed.finish_at", 0);
		if (finishAt == 0) {
			finishAt = runtimeState().getLong("sealed.expire_deadline", System.currentTimeMillis());
			runtimeState().put("sealed.finish_at", finishAt);
		}
		int rank = checkRank(instanceReward.getPoints(), startAt, finishAt);
		instanceReward.setRank(rank);
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		runtimeState().put("sealed.rank", rank);
		runtimeState().put("sealed.completed", true);
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
		if (runtimeState().getBoolean("sealed.completed", false)) {
			return;
		}
		long settlement = runtimeState().getLong("sealed.settle_deadline", 0);
		if (bossKillTime() > 0) {
			if (settlement == 0) {
				settlement = bossKillTime() + SETTLEMENT_DELAY;
				runtimeState().put("sealed.finish_at", bossKillTime());
				runtimeState().put("sealed.settle_deadline", settlement);
			}
			scheduleDeadline("settle", settlement, this::completeInstance);
			return;
		}
		long expire = runtimeState().getLong("sealed.expire_deadline", 0);
		if (runtimeState().getLong("sealed.start_at", 0) > 0 && expire > 0) {
			scheduleDeadline("expire", expire, this::completeInstance);
			return;
		}
		long prepare = runtimeState().getLong("sealed.prepare_deadline", 0);
		if (prepare > 0) {
			scheduleDeadline("prepare", prepare, () -> startMainTimer(prepare));
		}
	}

	private void restoreWorldState() {
		restorePrison();
		restoreStaticDeaths();
		restoreActivatedHetgolems();
		restoreBoss();
		if (runtimeState().getBoolean("sealed.completed", false)) {
			despawnScoredNpcs();
		}
	}

	private void restorePrison() {
		Npc prison = getNpc(237195);
		if (prison != null && hasKillEvent(prison)) {
			delete(getNpc(237195));
			delete(getNpc(701000));
			return;
		}
		int skillId = runtimeState().getInt("sealed.resistance_skill", 0);
		if (skillId == 0) {
			skillId = 19311 + Rnd.get(1, 4);
			runtimeState().put("sealed.resistance_skill", skillId);
		}
		if (prison != null) {
			GameEngineServices.skillEngine().getSkill(prison, skillId, 60, prison).useNoAnimationSkill();
		}
	}

	private void restoreStaticDeaths() {
		for (Npc npc : npcs()) {
			if (hasKillEvent(npc)) {
				delete(npc);
			}
		}
	}

	private void restoreActivatedHetgolems() {
		for (Npc drained : new ArrayList<>(instance.getNpcs(856547))) {
			String key = activationKey(drained);
			if (!runtimeState().getBoolean(key, false)) {
				continue;
			}
			float x = Float.parseFloat(runtimeState().get(key + ".x"));
			float y = Float.parseFloat(runtimeState().get(key + ".y"));
			float z = Float.parseFloat(runtimeState().get(key + ".z"));
			byte heading = Byte.parseByte(runtimeState().get(key + ".heading"));
			delete(drained);
			if (!hasKillEvent(237196, x, y, z)) {
				spawn(237196, x, y, z, heading);
			}
		}
	}

	private void selectBoss(Player player) {
		if (runtimeState().getInt("sealed.boss_id", 0) > 0) {
			return;
		}
		int bossId = switch (player.getPlayerClass()) {
			case RANGER, CLERIC, TEMPLAR, CHANTER, ASSASSIN, GLADIATOR -> 237193;
			default -> 237194;
		};
		runtimeState().put("sealed.boss_id", bossId);
		spawnBoss(bossId);
	}

	private void restoreBoss() {
		int bossId = runtimeState().getInt("sealed.boss_id", 0);
		if (bossId > 0 && bossKillTime() == 0
				&& !runtimeState().getBoolean("sealed.completed", false)) {
			spawnBoss(bossId);
		}
	}

	private void spawnBoss(int bossId) {
		if (getNpc(237193) == null && getNpc(237194) == null) {
			spawn(bossId, BOSS_X, BOSS_Y, BOSS_Z, BOSS_HEADING);
		}
	}

	private void activateHetgolem(Player player, Npc drained) {
		String key = activationKey(drained);
		if (runtimeState().getBoolean(key, false)) {
			return;
		}
		if (!player.getInventory().decreaseByItemId(185000242, 1)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1402976));
			return;
		}
		runtimeState().put(key, true);
		float x = drained.getX();
		float y = drained.getY();
		float z = drained.getZ();
		byte heading = drained.getHeading();
		runtimeState().put(key + ".x", x);
		runtimeState().put(key + ".y", y);
		runtimeState().put(key + ".z", z);
		runtimeState().put(key + ".heading", heading);
		delete(drained);
		sendSystemMessage(1402978);
		spawn(237196, x, y, z, heading);
		long deadline = System.currentTimeMillis() + 2_000L;
		scheduleDeadline("activation_message_" + positionKey(x, y, z), deadline,
				() -> sendSystemMessage(1402977));
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

	private SealedArgentManorPlayerReward getOrCreatePlayerReward(int playerId) {
		SealedArgentManorPlayerReward reward = (SealedArgentManorPlayerReward) instanceReward.getPlayerReward(playerId);
		if (reward == null) {
			reward = new SealedArgentManorPlayerReward(playerId);
			if (runtimeState().getBoolean(playerRewardKey(playerId), false)) {
				reward.setRewarded();
				applyRewardDisplay(reward, InstanceSettlementService.timeAttackPlan(mapId, instanceReward.getRank()));
			}
			instanceReward.addPlayerReward(reward);
		}
		return reward;
	}

	private void applyRewardDisplay(SealedArgentManorPlayerReward reward, RewardPlan plan) {
		reward.setScoreAP(plan.ap());
		for (var item : plan.items()) {
			int count = Math.toIntExact(item.count());
			switch (item.itemId()) {
				case 188054114 -> reward.setGreaterArgentManorBox(count);
				case 188054115 -> reward.setArgentManorBox(count);
				case 188054116 -> reward.setLesserArgentManorBox(count);
			}
		}
	}

	private InstanceScoreType scoreType() {
		if (runtimeState().getBoolean("sealed.completed", false)) {
			return InstanceScoreType.END_PROGRESS;
		}
		return runtimeState().getLong("sealed.start_at", 0) > 0
				? InstanceScoreType.START_PROGRESS : InstanceScoreType.PREPARING;
	}

	private int checkRank(int totalPoints, long startAt, long finishAt) {
		return InstanceSettlementService.timeAttackRank(mapId, totalPoints,
				Math.max(0, finishAt - startAt) / 1000);
	}

	private int getTime() {
		if (runtimeState().getBoolean("sealed.completed", false)) {
			return 0;
		}
		long deadline = runtimeState().getLong("sealed.start_at", 0) > 0
				? runtimeState().getLong("sealed.expire_deadline", 0)
				: runtimeState().getLong("sealed.prepare_deadline", 0);
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

	private static String playerRewardKey(int playerId) {
		return "sealed.player." + playerId + ".rewarded";
	}

	private void restoreScore() {
		int points = 0;
		int kills = 0;
		for (String value : runtimeState().snapshot("sealed.kill.").values()) {
			KillEvent event = KillEvent.decode(value);
			points = Math.addExact(points, event.score());
			if (event.wasCounted()) {
				kills++;
			}
		}
		instanceReward.restore(points, kills, runtimeState().getInt("sealed.rank", 7));
	}

	private KillEvent recordKill(Npc npc, int retailScore) {
		String key = killKey(npc);
		String existing = runtimeState().get(key);
		if (existing != null) {
			return KillEvent.decode(existing).duplicate();
		}
		boolean counted = instanceReward.getInstanceScoreType().isStartProgress();
		KillEvent event = new KillEvent(counted ? retailScore : 0, counted, System.currentTimeMillis(), counted);
		runtimeState().put(key, event.encode());
		if (counted) {
			instanceReward.addPoints(retailScore);
			instanceReward.addNpcKill();
		}
		return event;
	}

	private long bossKillTime() {
		int bossId = runtimeState().getInt("sealed.boss_id", 0);
		if (bossId == 0) {
			return 0;
		}
		String value = runtimeState().get(killKey(bossId, BOSS_X, BOSS_Y, BOSS_Z));
		return value == null ? 0 : KillEvent.decode(value).killedAt();
	}

	private long killTime(Npc npc) {
		String value = runtimeState().get(killKey(npc));
		return value == null ? 0 : KillEvent.decode(value).killedAt();
	}

	private boolean hasKillEvent(Npc npc) {
		return runtimeState().get(killKey(npc)) != null;
	}

	private boolean hasKillEvent(int npcId, float x, float y, float z) {
		return runtimeState().get(killKey(npcId, x, y, z)) != null;
	}

	private static String killKey(Npc npc) {
		return killKey(npc.getNpcId(), npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ());
	}

	private static String killKey(int npcId, float x, float y, float z) {
		return "sealed.kill." + npcId + '.' + positionKey(x, y, z);
	}

	private static String activationKey(Npc npc) {
		return "sealed.activated." + positionKey(npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ());
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

	private static void removeEffects(Player player) {
		PlayerEffectController effects = player.getEffectController();
		effects.removeEffect(19316);
		effects.removeEffect(19317);
		effects.removeEffect(19318);
		effects.removeEffect(19319);
	}

	private record KillEvent(int score, boolean wasCounted, long killedAt, boolean newlyCounted) {
		private String encode() {
			return score + ":" + wasCounted + ":" + killedAt;
		}

		private KillEvent duplicate() {
			return new KillEvent(score, wasCounted, killedAt, false);
		}

		private static KillEvent decode(String value) {
			String[] parts = value.split(":", -1);
			if (parts.length != 3) {
				throw new IllegalStateException("Invalid Sealed Argent Manor kill event");
			}
			return new KillEvent(Integer.parseInt(parts[0]), Boolean.parseBoolean(parts[1]),
					Long.parseLong(parts[2]), false);
		}
	}
}
