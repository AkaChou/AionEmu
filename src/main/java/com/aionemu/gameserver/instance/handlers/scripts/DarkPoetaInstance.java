package com.aionemu.gameserver.instance.handlers.scripts;

import java.util.ArrayList;
import java.util.Set;

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.RetailAiData.NpcScore;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.instance.InstanceScoreType;
import com.aionemu.gameserver.model.instance.instancereward.DarkPoetaReward;
import com.aionemu.gameserver.model.instance.instancereward.InstanceReward;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INSTANCE_SCORE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.instance.InstanceSettlementService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;

@InstanceID(300040000)
public class DarkPoetaInstance extends GeneralInstanceHandler {
	private static final long SETTLEMENT_DELAY = 5_000;
	private static final int[] MARABATA_CONTROLLERS = {
		700439, 700440, 700441, 700442, 700443, 700444, 700445, 700446, 700447
	};
	private static final Set<Integer> GRADE_BOSSES = Set.of(215280, 215281, 215282, 215283, 215284, 217166);

	private DarkPoetaReward instanceReward;

	@Override
	public void onInstanceCreate(WorldMapInstance instance) {
		super.onInstanceCreate(instance);
		instanceReward = new DarkPoetaReward(mapId, instanceId);
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
		int highestLevel = Math.max(runtimeState().getInt("dark.highest_level", 0), player.getLevel());
		runtimeState().put("dark.highest_level", highestLevel);
		if (runtimeState().get("dark.race") == null) {
			runtimeState().put("dark.race", player.getRace().name());
			setCondition(player.getRace() == Race.ELYOS ? "light" : "dark", 1);
		}
		if (runtimeState().getBoolean("dark.completed", false)) {
			if (System.currentTimeMillis() >= runtimeState().getLong("dark.leave_deadline", Long.MAX_VALUE)) {
				TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
				return;
			}
			sendScore(0, 0);
			return;
		}
		startPrepareTimer();
	}

	@Override
	public void onOpenDoor(Player player, int doorId) {
		if (doorId != 33 || runtimeState().getBoolean("dark.completed", false)) {
			return;
		}
		setDoorState(doorId, true);
		if (runtimeState().getLong("dark.start_at", 0) == 0) {
			startMainTimer(System.currentTimeMillis());
		}
		sendSystemMessage(1401181);
	}

	@Override
	public void onDie(Npc npc) {
		Boolean conditionRespawn = RetailConditionSpawnEngine.consumeConditionSpawnDeath(instance, npc);
		NpcScore score = DataManager.RETAIL_AI_DATA == null ? null
			: DataManager.RETAIL_AI_DATA.getNpcScore(npc.getNpcId());
		if (score != null && (score.scoreApplyType() != 0 || score.equalizingScore() != 0)) {
			throw new IllegalStateException("Unsupported Dark Poeta NPC score for " + npc.getNpcId());
		}
		KillEvent event = recordKill(npc, score == null ? 0 : score.value(), score != null, conditionRespawn);
		if (!event.newlyRecorded()) {
			return;
		}

		int npcId = npc.getNpcId();
		if (isMarabataController(npcId)) {
			scheduleMarabataController(npcId);
			return;
		}
		handleObjectCleanup(npc);
		if (event.wasCounted()) {
			sendScore(npc.getObjectTemplate().getNameId(), event.score());
		}
		handleConditionProgress(npcId);

		Player killer = npc.getAggroList().getMostPlayerDamage();
		if (npcId == 214894) {
			sendMovieOnce(killer, 426);
			deleteNpc(281121);
		} else if (npcId == 214904) {
			startSettlement(event.killedAt());
		} else if (GRADE_BOSSES.contains(npcId)) {
			setCondition("boss_kill", 1);
			setCondition("idlf1_bonus_boss_kill", 1);
		}
	}

	@Override
	public void onGather(Player player, Gatherable gatherable) {
		if (!instanceReward.getInstanceScoreType().isStartProgress()) {
			return;
		}
		int points = InstanceSettlementService.darkPoetaGatherScore(
			gatherable.getObjectTemplate().getTemplateId());
		if (points == 0) {
			return;
		}
		recordGather(points);
		sendScore(gatherable.getObjectTemplate().getNameId(), points);
	}

	@Override
	public void onLeaveInstance(Player player) {
		PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400255, player.getName()));
		if (player.isInGroup2()) {
			PlayerGroupService.removePlayer(player);
		}
	}

	@Override
	public void onExitInstance(Player player) {
		InstanceService.destroyInstance(instance);
		if (instanceReward.getInstanceScoreType().isEndProgress()) {
			TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
		}
	}

	private void startPrepareTimer() {
		if (runtimeState().getLong("dark.start_at", 0) > 0) {
			sendScore(0, 0);
			return;
		}
		long deadline = runtimeState().getLong("dark.prepare_deadline", 0);
		if (deadline == 0) {
			deadline = System.currentTimeMillis() + InstanceSettlementService.darkPoetaPrepareSeconds() * 1000L;
			runtimeState().put("dark.prepare_deadline", deadline);
		}
		long prepareDeadline = deadline;
		scheduleDeadline("prepare", deadline, () -> startMainTimer(prepareDeadline));
		sendScore(0, 0);
	}

	private synchronized void startMainTimer(long startAt) {
		if (runtimeState().getLong("dark.start_at", 0) > 0
				|| runtimeState().getBoolean("dark.completed", false)) {
			return;
		}
		cancelDeadline("prepare");
		long deadline = startAt + InstanceSettlementService.darkPoetaLimitSeconds() * 1000L;
		runtimeState().put("dark.start_at", startAt);
		runtimeState().put("dark.expire_deadline", deadline);
		instanceReward.setInstanceScoreType(InstanceScoreType.START_PROGRESS);
		sendScore(0, 0);
		scheduleDeadline("expire", deadline, this::completeInstance);
	}

	private void startSettlement(long finishAt) {
		if (runtimeState().getBoolean("dark.completed", false)
				|| runtimeState().getLong("dark.settle_deadline", 0) > 0) {
			return;
		}
		runtimeState().put("dark.finish_at", finishAt);
		long deadline = finishAt + SETTLEMENT_DELAY;
		runtimeState().put("dark.settle_deadline", deadline);
		scheduleDeadline("settle", deadline, this::completeInstance);
	}

	private synchronized void completeInstance() {
		if (runtimeState().getBoolean("dark.completed", false)) {
			return;
		}
		long finishAt = runtimeState().getLong("dark.finish_at", 0);
		if (finishAt == 0) {
			finishAt = runtimeState().getLong("dark.expire_deadline", System.currentTimeMillis());
			runtimeState().put("dark.finish_at", finishAt);
		}
		long startAt = runtimeState().getLong("dark.start_at", finishAt);
		int rank = InstanceSettlementService.darkPoetaRank(instanceReward.getPoints(),
			Math.max(0, finishAt - startAt) / 1000);
		int grade = InstanceSettlementService.darkPoetaBossGrade(rank,
			runtimeState().getInt("dark.highest_level", 0));
		instanceReward.setRank(rank);
		instanceReward.setInstanceScoreType(InstanceScoreType.END_PROGRESS);
		runtimeState().put("dark.rank", rank);
		runtimeState().put("dark.grade", grade);
		setCondition("grade", grade);
		runtimeState().put("dark.completed", true);
		cancelDeadline("prepare");
		cancelDeadline("expire");
		cancelDeadline("settle");
		startLeaveTimer();
		sendScore(0, 0);
	}

	private void startLeaveTimer() {
		long deadline = runtimeState().getLong("dark.leave_deadline", 0);
		if (deadline == 0) {
			deadline = System.currentTimeMillis() + InstanceSettlementService.darkPoetaLeaveSeconds() * 1000L;
			runtimeState().put("dark.leave_deadline", deadline);
		}
		scheduleDeadline("leave", deadline, this::leaveCompletedInstance);
	}

	private void leaveCompletedInstance() {
		for (Player player : new ArrayList<>(instance.getPlayersInside())) {
			TeleportService2.moveToInstanceExit(player, mapId, player.getRace());
		}
	}

	private void restoreDeadlines() {
		restoreMarabataDeadlines();
		if (runtimeState().getBoolean("dark.completed", false)) {
			startLeaveTimer();
			return;
		}
		long settlement = runtimeState().getLong("dark.settle_deadline", 0);
		if (settlement > 0) {
			scheduleDeadline("settle", settlement, this::completeInstance);
			return;
		}
		long mainBossKilledAt = killTime(214904);
		if (mainBossKilledAt > 0) {
			startSettlement(mainBossKilledAt);
			return;
		}
		long expire = runtimeState().getLong("dark.expire_deadline", 0);
		if (runtimeState().getLong("dark.start_at", 0) > 0 && expire > 0) {
			scheduleDeadline("expire", expire, this::completeInstance);
			return;
		}
		long prepare = runtimeState().getLong("dark.prepare_deadline", 0);
		if (prepare > 0) {
			scheduleDeadline("prepare", prepare, () -> startMainTimer(prepare));
		}
	}

	private KillEvent recordKill(Npc npc, int retailScore, boolean scored, Boolean conditionRespawn) {
		String key = killKey(npc, conditionRespawn);
		String existing = runtimeState().get(key);
		if (existing != null) {
			return KillEvent.decode(existing).duplicate();
		}
		boolean counted = scored && instanceReward.getInstanceScoreType().isStartProgress();
		KillEvent event = new KillEvent(counted ? retailScore : 0, counted, System.currentTimeMillis(),
			npc.getNpcId(), true);
		runtimeState().put(key, event.encode());
		if (counted) {
			instanceReward.addPoints(retailScore);
			instanceReward.addNpcKill();
		}
		return event;
	}

	private synchronized void recordGather(int score) {
		int sequence = runtimeState().getInt("dark.gather_sequence", 0) + 1;
		runtimeState().put("dark.gather_sequence", sequence);
		runtimeState().put("dark.gather.event." + sequence, score);
		instanceReward.addPoints(score);
		instanceReward.addGatherCollection();
	}

	private void restoreScore() {
		int points = 0;
		int kills = 0;
		for (String value : runtimeState().snapshot("dark.kill.").values()) {
			KillEvent event = KillEvent.decode(value);
			points = Math.addExact(points, event.score());
			if (event.wasCounted()) {
				kills++;
			}
		}
		int gathers = 0;
		for (String value : runtimeState().snapshot("dark.gather.event.").values()) {
			points = Math.addExact(points, Integer.parseInt(value));
			gathers++;
		}
		instanceReward.restore(points, kills, gathers, runtimeState().getInt("dark.rank", 7));
	}

	private long killTime(int npcId) {
		long result = 0;
		for (String value : runtimeState().snapshot("dark.kill.").values()) {
			KillEvent event = KillEvent.decode(value);
			if (event.npcId() == npcId) {
				result = Math.max(result, event.killedAt());
			}
		}
		return result;
	}

	private void handleConditionProgress(int npcId) {
		switch (npcId) {
			case 214843 -> setCondition("nagaboss_kill", 1);
			case 214895 -> setCondition("middleboss_a_kill", 1);
			case 214896 -> setCondition("middleboss_b_kill", 1);
			case 214897 -> setCondition("middleboss_c_kill", 1);
			default -> {
				return;
			}
		}
		if (npcId >= 214895 && npcId <= 214897
				&& killTime(214895) > 0 && killTime(214896) > 0 && killTime(214897) > 0) {
			sendMovieOnce(instance.getPlayersInside().stream().findFirst().orElse(null), 427);
		}
	}

	private void setCondition(String variable, int value) {
		if (!RetailConditionSpawnEngine.setVariable(instance, variable, value, 0)) {
			throw new IllegalStateException("Missing Dark Poeta condition variable " + variable);
		}
	}

	private void handleObjectCleanup(Npc npc) {
		switch (npc.getNpcId()) {
			case 701869, 281088, 281089, 281090, 281091, 281092, 281095, 281096, 281097 -> delete(npc);
			case 214895 -> deleteNpcs(214898, 214899);
			case 214896 -> deleteNpcs(214900, 214901);
			case 214897 -> deleteNpcs(214902, 214903);
			case 214849 -> deleteMarabataControllers(700439, 700440, 700441);
			case 214850 -> deleteMarabataControllers(700442, 700443, 700444);
			case 214851 -> deleteMarabataControllers(700445, 700446, 700447);
		}
	}

	private void scheduleMarabataController(int npcId) {
		String stateKey = marabataDeadlineKey(npcId);
		long deadline = runtimeState().getLong(stateKey, 0);
		if (deadline == 0) {
			deadline = System.currentTimeMillis() + 30_000;
			runtimeState().put(stateKey, deadline);
		}
		scheduleDeadline("marabata_" + npcId, deadline, () -> respawnMarabataController(npcId));
	}

	private void restoreMarabataDeadlines() {
		for (int npcId : MARABATA_CONTROLLERS) {
			long deadline = runtimeState().getLong(marabataDeadlineKey(npcId), 0);
			if (deadline > 0) {
				scheduleDeadline("marabata_" + npcId, deadline, () -> respawnMarabataController(npcId));
			}
		}
	}

	private void respawnMarabataController(int npcId) {
		Npc boss = getNpc(marabataBossId(npcId));
		if (boss != null && !boss.getLifeStats().isAlreadyDead()) {
			SpawnPoint point = marabataPoint(npcId);
			spawn(npcId, point.x(), point.y(), point.z(), point.heading());
		}
		runtimeState().remove(marabataDeadlineKey(npcId));
	}

	private void deleteMarabataControllers(int... npcIds) {
		for (int npcId : npcIds) {
			cancelDeadline("marabata_" + npcId);
			runtimeState().remove(marabataDeadlineKey(npcId));
			deleteNpc(npcId);
		}
	}

	private void sendMovieOnce(Player player, int movieId) {
		String key = "dark.movie." + movieId;
		if (player == null || runtimeState().getBoolean(key, false)) {
			return;
		}
		runtimeState().put(key, true);
		PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(0, movieId));
	}

	private void sendSystemMessage(int messageId) {
		for (Player player : instance.getPlayersInside()) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(messageId));
		}
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

	private int getTime() {
		if (runtimeState().getBoolean("dark.completed", false)) {
			return 0;
		}
		long deadline = runtimeState().getLong("dark.start_at", 0) > 0
			? runtimeState().getLong("dark.expire_deadline", 0)
			: runtimeState().getLong("dark.prepare_deadline", 0);
		return (int) Math.max(0, deadline - System.currentTimeMillis());
	}

	private InstanceScoreType scoreType() {
		if (runtimeState().getBoolean("dark.completed", false)) {
			return InstanceScoreType.END_PROGRESS;
		}
		return runtimeState().getLong("dark.start_at", 0) > 0 ? InstanceScoreType.START_PROGRESS
			: InstanceScoreType.PREPARING;
	}

	private String killKey(Npc npc, Boolean conditionRespawn) {
		if (Boolean.TRUE.equals(conditionRespawn)
				|| (conditionRespawn == null && npc.getSpawn().getRespawnTime() > 0)) {
			return "dark.kill.object." + npc.getObjectId();
		}
		return killKey(npc.getNpcId(), npc.getSpawn().getX(), npc.getSpawn().getY(), npc.getSpawn().getZ());
	}

	private static String killKey(int npcId, float x, float y, float z) {
		return "dark.kill." + npcId + '.' + positionKey(x, y, z);
	}

	private static String positionKey(float x, float y, float z) {
		return Integer.toUnsignedString(Float.floatToIntBits(x)) + '_'
			+ Integer.toUnsignedString(Float.floatToIntBits(y)) + '_'
			+ Integer.toUnsignedString(Float.floatToIntBits(z));
	}

	private static boolean isMarabataController(int npcId) {
		return npcId >= 700439 && npcId <= 700447;
	}

	private static int marabataBossId(int npcId) {
		return switch (npcId) {
			case 700439, 700440, 700441 -> 214850;
			case 700442, 700443, 700444 -> 214851;
			case 700445, 700446, 700447 -> 214849;
			default -> throw new IllegalArgumentException("Unknown Marabata controller " + npcId);
		};
	}

	private static SpawnPoint marabataPoint(int npcId) {
		return switch (npcId) {
			case 700439 -> new SpawnPoint(665.374f, 372.751f, 99.375f, (byte) 90);
			case 700440 -> new SpawnPoint(681.851013f, 408.625f, 100.472f, (byte) 13);
			case 700441 -> new SpawnPoint(646.549988f, 406.088013f, 99.375f, (byte) 49);
			case 700442 -> new SpawnPoint(636.117981f, 325.536987f, 99.375f, (byte) 49);
			case 700443 -> new SpawnPoint(676.257019f, 319.649994f, 99.375f, (byte) 4);
			case 700444 -> new SpawnPoint(655.851013f, 292.710999f, 99.375f, (byte) 90);
			case 700445 -> new SpawnPoint(605.625f, 380.479004f, 99.375f, (byte) 14);
			case 700446 -> new SpawnPoint(598.706f, 345.978f, 99.375f, (byte) 98);
			case 700447 -> new SpawnPoint(567.775024f, 366.207001f, 99.375f, (byte) 59);
			default -> throw new IllegalArgumentException("Unknown Marabata controller " + npcId);
		};
	}

	private static String marabataDeadlineKey(int npcId) {
		return "dark.marabata." + npcId + ".deadline";
	}

	private void deleteNpcs(int... npcIds) {
		for (int npcId : npcIds) {
			deleteNpc(npcId);
		}
	}

	private void deleteNpc(int npcId) {
		delete(getNpc(npcId));
	}

	private static void delete(Npc npc) {
		if (npc != null) {
			npc.getController().onDelete();
		}
	}

	private record SpawnPoint(float x, float y, float z, byte heading) {
	}

	private record KillEvent(int score, boolean wasCounted, long killedAt, int npcId, boolean newlyRecorded) {
		private String encode() {
			return score + ":" + wasCounted + ":" + killedAt + ":" + npcId;
		}

		private KillEvent duplicate() {
			return new KillEvent(score, wasCounted, killedAt, npcId, false);
		}

		private static KillEvent decode(String value) {
			String[] parts = value.split(":", -1);
			if (parts.length != 4) {
				throw new IllegalStateException("Invalid Dark Poeta kill event");
			}
			return new KillEvent(Integer.parseInt(parts[0]), Boolean.parseBoolean(parts[1]),
				Long.parseLong(parts[2]), Integer.parseInt(parts[3]), false);
		}
	}
}
