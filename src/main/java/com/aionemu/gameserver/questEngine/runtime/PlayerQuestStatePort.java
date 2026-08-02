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
		QuestState shadow = state == null
			? new QuestState(plan.questId(), plan.nextStatus(), plan.nextPackedVariables(), 0, null, null, null)
			: new QuestState(state.getQuestId(), plan.nextStatus(), plan.nextPackedVariables(),
				state.getCompleteCount(), state.getNextRepeatTime(), state.getReward(), state.getCompleteTime());
		if (state == null || state.getPersistentState() == PersistentState.NEW) {
			shadow.setPersistentState(PersistentState.NEW);
		} else {
			// QuestState 在 NEW 状态忽略 UPDATE_REQUIRED;先脱离 NEW 再标记 UPDATE。
			shadow.setPersistentState(PersistentState.UPDATED);
			shadow.setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
		questDao.store(connection, playerId, List.of(shadow));
	}

	@Override
	public void publish(int playerId, QuestMutationPlan plan) {
		Objects.requireNonNull(plan, "plan");
		Player player = players.find(playerId);
		if (player == null) {
			// 提交已成功但玩家已登出:内存无对象可发布,数据库值已是正确投影,重登时恢复。
			return;
		}
		QuestState state = player.getQuestStateList().getQuestState(plan.questId());
		if (state == null) {
			state = new QuestState(plan.questId(), plan.nextStatus(), plan.nextPackedVariables(), 0,
				null, null, null);
			player.getQuestStateList().addQuest(plan.questId(), state);
		}
		state.setQuestVar(plan.nextPackedVariables());
		state.setStatus(plan.nextStatus());
		// 已持久化:避免下一次 store(Connection, Player) 重复写。
		state.setPersistentState(PersistentState.UPDATED);
	}
}
