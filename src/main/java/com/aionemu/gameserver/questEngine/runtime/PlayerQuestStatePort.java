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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.QuestTemplate;
import com.aionemu.gameserver.model.templates.quest.QuestRepeatCycle;
import com.aionemu.gameserver.questEngine.definition.QuestAction;

/**
 * Persists the canonical quest projection through the caller-owned transaction
 * without advancing the live in-memory state.
 *
 * <p>{@link #apply} writes the projection from a shadow {@link QuestState} so a
 * failed commit never leaves the player's in-memory quest state ahead of the
 * database. {@link #publish} is invoked only after the transaction committed
 * and atomically publishes the projection to the live state, marking it as
 * persisted so the next store does not rewrite it.</p>
 */
public final class PlayerQuestStatePort implements QuestStatePort {
	private final QuestPlayerPort players;
	private final PlayerQuestListDAO questDao;
	private final Map<PendingKey, QuestState> pending = new ConcurrentHashMap<>();

	public PlayerQuestStatePort(QuestPlayerPort players, PlayerQuestListDAO questDao) {
		this.players = Objects.requireNonNull(players, "players");
		this.questDao = Objects.requireNonNull(questDao, "questDao");
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
		// 影子状态:写 DB 但不触碰 live 内存。新建状态保持 NEW 走 INSERT,
		// 已存在状态标记 UPDATE_REQUIRED 走 UPDATE,由同一个 UoW 连接完成。
		QuestState shadow = projection(player, state, plan);
		if (state == null || state.getPersistentState() == PersistentState.NEW) {
			shadow.setPersistentState(PersistentState.NEW);
		} else {
			// QuestState 在 NEW 状态忽略 UPDATE_REQUIRED;先脱离 NEW 再标记 UPDATE。
			shadow.setPersistentState(PersistentState.UPDATED);
			shadow.setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
		questDao.store(connection, playerId, List.of(shadow));
		PendingKey key = new PendingKey(playerId, plan);
		if (pending.putIfAbsent(key, shadow) != null) {
			throw new IllegalStateException("quest projection is already pending: " + plan.questId());
		}
	}

	@Override
	public void publish(int playerId, QuestMutationPlan plan) {
		Objects.requireNonNull(plan, "plan");
		QuestState committed = pending.remove(new PendingKey(playerId, plan));
		if (committed == null) {
			throw new IllegalStateException("quest projection was not prepared: " + plan.questId());
		}
		Player player = players.find(playerId);
		if (player == null) {
			// 提交已成功但玩家已登出:内存无对象可发布,数据库值已是正确投影,重登时恢复。
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
	}

	@Override
	public void rollback(int playerId, QuestMutationPlan plan) {
		pending.remove(new PendingKey(playerId, Objects.requireNonNull(plan, "plan")));
	}

	private static QuestState projection(Player player, QuestState state, QuestMutationPlan plan) {
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
			QuestTemplate template = DataManager.QUEST_DATA == null ? null : DataManager.QUEST_DATA.getQuestById(plan.questId());
			if (template != null && ((template.getRepeatCycle() != null && player.getAccessLevel() == 0)
					|| template.getQuestCoolTime() > 0)) {
				nextRepeatTime = nextRepeatTime(template);
			}
		}
		return new QuestState(plan.questId(), plan.nextStatus(), plan.nextPackedVariables(), completeCount,
			nextRepeatTime, reward, completeTime);
	}

	private static Timestamp nextRepeatTime(QuestTemplate template) {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime repeatDate = now.withHour(9).withMinute(0).withSecond(0).withNano(0);
		if (template.isDaily()) {
			if (now.isAfter(repeatDate)) repeatDate = repeatDate.plusHours(24);
		} else if (template.getQuestCoolTime() > 0) {
			repeatDate = repeatDate.plusSeconds(template.getQuestCoolTime());
		} else {
			int daysToAdd = 7;
			int startDay = 7;
			for (QuestRepeatCycle weekDay : template.getRepeatCycle()) {
				int dayValue = weekDay.getDay();
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

	private record PendingKey(int playerId, QuestMutationPlan plan) {
	}
}
