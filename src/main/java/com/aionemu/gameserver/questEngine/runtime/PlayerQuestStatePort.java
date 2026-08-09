package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.dao.PlayerQuestListDAO;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.model.QuestState;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntFunction;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.questEngine.definition.QuestAction;
import com.aionemu.gameserver.questEngine.definition.QuestMetadata;

/**
 * 通过调用方事务持久化规范任务投影，不提前推进实时内存状态。
 * Persists the canonical quest projection through the caller-owned transaction without advancing live memory.
 *
 * <p>{@link #apply} 先写入暂存 {@link QuestState}，因此提交失败不会让玩家内存状态领先数据库；
 * {@link #publish} 仅在事务提交后原子发布到实时状态并标记为已持久化。
 * {@link #apply} first writes a staged {@link QuestState}; {@link #publish} atomically publishes it only after commit.</p>
 */
public final class PlayerQuestStatePort implements QuestStatePort {
	private final QuestPlayerPort players;
	private final PlayerQuestListDAO questDao;
	private final IntFunction<QuestMetadata> metadata;
	private final Map<PendingKey, QuestState> pending = new ConcurrentHashMap<>();

	public PlayerQuestStatePort(QuestPlayerPort players, PlayerQuestListDAO questDao) {
		this(players, questDao,
			questId -> GameEngineServices.questEngine().questCatalog().findMetadata(questId).orElse(null));
	}

	PlayerQuestStatePort(QuestPlayerPort players, PlayerQuestListDAO questDao,
			IntFunction<QuestMetadata> metadata) {
		this.players = Objects.requireNonNull(players, "players");
		this.questDao = Objects.requireNonNull(questDao, "questDao");
		this.metadata = Objects.requireNonNull(metadata, "metadata");
	}

	@Override
	public void apply(Connection connection, int playerId, QuestMutationPlan plan) throws SQLException {
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(plan, "plan");
		Player player = players.find(playerId);
		if (player == null) {
			throw new IllegalStateException("player is unavailable: " + playerId);
		}
		QuestState state = player.getQuestStateList().getQuestState(plan.questId());
		// 暂存投影写入 DB 但不触碰 live 内存；新状态走 INSERT，已有状态走 UPDATE。
		// The staged projection is written without mutating live memory; new states insert and existing states update.
		QuestMetadata questMetadata = plan.requiredActions().stream()
			.anyMatch(QuestAction.CompleteQuest.class::isInstance) ? metadata.apply(plan.questId()) : null;
		QuestState stagedProjection = projection(player, state, plan, questMetadata);
		if (state == null || state.getPersistentState() == PersistentState.NEW) {
			stagedProjection.setPersistentState(PersistentState.NEW);
		} else {
			// QuestState 在 NEW 状态忽略 UPDATE_REQUIRED;先脱离 NEW 再标记 UPDATE。
			stagedProjection.setPersistentState(PersistentState.UPDATED);
			stagedProjection.setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
		questDao.store(connection, playerId, List.of(stagedProjection));
		PendingKey key = new PendingKey(playerId, plan);
		if (pending.putIfAbsent(key, stagedProjection) != null) {
			throw new IllegalStateException("quest projection is already pending: " + plan.questId());
		}
	}

	@Override
	public void publish(int playerId, QuestMutationPlan plan) {
		Objects.requireNonNull(plan, "plan");
		PendingKey key = new PendingKey(playerId, plan);
		QuestState committed = pending.get(key);
		if (committed == null) {
			throw new IllegalStateException("quest projection was not prepared: " + plan.questId());
		}
		Player player = players.find(playerId);
		if (player == null) {
			// 提交已成功但玩家已登出:内存无对象可发布,数据库值已是正确投影,重登时恢复。
			pending.remove(key, committed);
			return;
		}
		QuestState state = player.getQuestStateList().getQuestState(plan.questId());
		if (state == null) {
			state = new QuestState(plan.questId(), committed.getStatus(), committed.getQuestVars().getQuestVars(),
				committed.getCompleteCount(), committed.getNextRepeatTime(), committed.getRewardOrNull(),
				committed.getCompleteTime());
			player.getQuestStateList().addQuest(plan.questId(), state);
		} else {
			state.setQuestVar(committed.getQuestVars().getQuestVars());
			state.setStatus(committed.getStatus());
			state.setCompleteCount(committed.getCompleteCount());
			state.setReward(committed.getRewardOrNull());
			state.setCompleteTime(committed.getCompleteTime());
			state.setNextRepeatTime(committed.getNextRepeatTime());
		}
		// 已持久化:避免下一次 store(Connection, Player) 重复写。
		state.setPersistentState(PersistentState.UPDATED);
		pending.remove(key, committed);
	}

	@Override
	public void rollback(int playerId, QuestMutationPlan plan) {
		pending.remove(new PendingKey(playerId, Objects.requireNonNull(plan, "plan")));
	}

	private static QuestState projection(Player player, QuestState state, QuestMutationPlan plan,
			QuestMetadata metadata) {
		int completeCount = state == null ? 0 : state.getCompleteCount();
		Timestamp nextRepeatTime = state == null ? null : state.getNextRepeatTime();
		Integer reward = state == null ? null : state.getRewardOrNull();
		Timestamp completeTime = state == null ? null : state.getCompleteTime();
		QuestAction.CompleteQuest completion = plan.requiredActions().stream()
			.filter(QuestAction.CompleteQuest.class::isInstance)
			.map(QuestAction.CompleteQuest.class::cast)
			.findFirst().orElse(null);
		if (completion != null) {
			completeCount++;
			reward = completion.rewardIndex();
			completeTime = new Timestamp(System.currentTimeMillis());
			if (metadata != null && shouldScheduleRepeat(player, metadata)) {
				nextRepeatTime = nextRepeatTime(metadata);
			}
		}
		return new QuestState(plan.questId(), plan.nextStatus(), plan.nextPackedVariables(), completeCount,
			nextRepeatTime, reward, completeTime);
	}

	private static boolean shouldScheduleRepeat(Player player, QuestMetadata metadata) {
		var repeat = metadata.repeatPolicy();
		boolean timeBased = repeat.daily() || repeat.weekly() || !metadata.repeatCycles().isEmpty();
		return (timeBased && player.getAccessLevel() == 0) || repeat.cooldownSeconds() > 0;
	}

	private static Timestamp nextRepeatTime(QuestMetadata metadata) {
		var repeat = metadata.repeatPolicy();
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime repeatDate = now.withHour(9).withMinute(0).withSecond(0).withNano(0);
		if (repeat.daily()) {
			if (now.isAfter(repeatDate)) repeatDate = repeatDate.plusHours(24);
		} else if (repeat.cooldownSeconds() > 0) {
			repeatDate = repeatDate.plusSeconds(repeat.cooldownSeconds());
		} else {
			Set<Integer> cycleDays = cycleDays(metadata.repeatCycles());
			if (cycleDays.isEmpty()) {
				throw new IllegalStateException("weekly quest metadata has no repeat cycle");
			}
			int daysToAdd = 7;
			int startDay = 7;
			for (int dayValue : cycleDays) {
				int diff = dayValue - repeatDate.getDayOfWeek().getValue();
				if (diff > 0 && diff < daysToAdd) daysToAdd = diff;
				if (startDay > dayValue) startDay = dayValue;
			}
			if (startDay == daysToAdd) daysToAdd = 7;
			else if (daysToAdd == 7 && startDay < 7) {
				daysToAdd = 7 - repeatDate.getDayOfWeek().getValue() + startDay;
			}
			repeatDate = repeatDate.plusDays(daysToAdd);
		}
		return new Timestamp(repeatDate.toInstant().toEpochMilli());
	}

	private static Set<Integer> cycleDays(Set<String> cycles) {
		Set<Integer> days = new LinkedHashSet<>();
		for (String cycle : cycles) {
			if ("ALL".equals(cycle)) {
				for (int day = 1; day <= 7; day++) days.add(day);
				continue;
			}
			days.add(switch (cycle) {
				case "MON" -> 1;
				case "TUE" -> 2;
				case "WED" -> 3;
				case "THU" -> 4;
				case "FRI" -> 5;
				case "SAT" -> 6;
				case "SUN" -> 7;
				default -> throw new IllegalStateException("unsupported repeat cycle " + cycle);
			});
		}
		return Set.copyOf(days);
	}

	private record PendingKey(int playerId, QuestMutationPlan plan) {
	}
}
