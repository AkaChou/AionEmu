package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerQuestGraphStateDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEscortActionAdapter.CleanupReason;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.ActionResult;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphTransitionExecutor.PersistenceResult;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphState.CleanupLease;
import com.aionemu.gameserver.questEngine.graph.state.PlayerQuestGraphStateList;

/**
 * 将持久 escort ledger 的物理清理与单状态 revision CAS 绑定，失败时保留 lease 供下一次补偿。
 * Binds physical cleanup of persisted escort ledgers to single-state revision CAS and retains leases for compensation
 * whenever cleanup or persistence fails.
 */
public final class QuestGraphEscortCleanupCoordinator {

	private final int playerId;
	private final PlayerQuestGraphStateList states;
	private final BiFunction<CleanupLease, CleanupReason, ActionResult> cleaner;
	private final BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence;

	/** 创建使用正式世界清理 endpoint 和 DAO CAS 的 coordinator。 / Creates a coordinator backed by production world cleanup and DAO CAS. */
	public QuestGraphEscortCleanupCoordinator(Player player) {
		this(requirePlayer(player).getObjectId(), player.getQuestGraphStateList(),
			(lease, reason) -> QuestGraphEscortActionAdapter.cleanupPersisted(player, lease, reason),
			(expectedRevision, state) -> DAOManager.getDAO(PlayerQuestGraphStateDAO.class)
				.compareAndSet(player.getObjectId(), expectedRevision, state) ? PersistenceResult.APPLIED : PersistenceResult.CONFLICT);
	}

	/** 创建可注入物理 endpoint 与 CAS 的聚焦测试 coordinator。 / Creates a focused-test coordinator with injectable physical and CAS endpoints. */
	QuestGraphEscortCleanupCoordinator(int playerId, PlayerQuestGraphStateList states,
			BiFunction<CleanupLease, CleanupReason, ActionResult> cleaner,
			BiFunction<Long, PlayerQuestGraphState, PersistenceResult> persistence) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("Escort cleanup coordinator player id is invalid");
		}
		this.playerId = playerId;
		this.states = Objects.requireNonNull(states, "states");
		this.cleaner = Objects.requireNonNull(cleaner, "cleaner");
		this.persistence = Objects.requireNonNull(persistence, "persistence");
	}

	/** 清理玩家所有已持久化 escort lease。 / Cleans every persisted escort lease owned by the player. */
	public ActionResult cleanupAll(CleanupReason reason) {
		Objects.requireNonNull(reason, "cleanup reason");
		ActionResult aggregate = ActionResult.ALREADY_APPLIED;
		for (PlayerQuestGraphState snapshot : states.snapshot()) {
			ActionResult result = cleanupState(snapshot.getQuestId(), reason);
			if (result == ActionResult.FAILED || result == ActionResult.REJECTED) {
				aggregate = result;
			} else if (result == ActionResult.APPLIED && aggregate == ActionResult.ALREADY_APPLIED) {
				aggregate = ActionResult.APPLIED;
			}
		}
		return aggregate;
	}

	/** 清理指定任务状态中的全部 escort lease。 / Cleans every escort lease in one quest state. */
	public ActionResult cleanupQuest(int questId, CleanupReason reason) {
		Objects.requireNonNull(reason, "cleanup reason");
		if (questId <= 0) {
			return ActionResult.FAILED;
		}
		return cleanupState(questId, reason);
	}

	private ActionResult cleanupState(int questId, CleanupReason reason) {
		synchronized (states) {
			PlayerQuestGraphState current = states.get(questId);
			if (current == null) {
				return ActionResult.ALREADY_APPLIED;
			}
			Map<String, CleanupLease> escorts;
			try {
				escorts = escortLeases(current);
			} catch (RuntimeException e) {
				return ActionResult.FAILED;
			}
			if (escorts.isEmpty()) {
				return ActionResult.ALREADY_APPLIED;
			}
			if (current.getJournal() != null) {
				return ActionResult.FAILED;
			}
			for (CleanupLease lease : escorts.values()) {
				ActionResult result;
				try {
					result = Objects.requireNonNull(cleaner.apply(lease, reason), "escort physical cleanup result");
				} catch (RuntimeException e) {
					return ActionResult.FAILED;
				}
				if (result != ActionResult.APPLIED && result != ActionResult.ALREADY_APPLIED) {
					return result;
				}
			}
			Map<String, CleanupLease> remaining = new LinkedHashMap<>(current.getCleanupLeases());
			remaining.keySet().removeAll(escorts.keySet());
			PlayerQuestGraphState cleaned;
			try {
				cleaned = copy(current, remaining);
			} catch (RuntimeException e) {
				return ActionResult.FAILED;
			}
			PersistenceResult persisted;
			try {
				persisted = Objects.requireNonNull(persistence.apply(current.getRevision(), cleaned), "escort cleanup persistence result");
			} catch (RuntimeException e) {
				return ActionResult.FAILED;
			}
			if (persisted != PersistenceResult.APPLIED) {
				return ActionResult.FAILED;
			}
			states.put(cleaned);
			escorts.values().forEach(lease -> QuestGraphEscortActionAdapter.acknowledgePersistedCleanup(playerId, lease));
			return ActionResult.APPLIED;
		}
	}

	private Map<String, CleanupLease> escortLeases(PlayerQuestGraphState state) {
		Map<String, CleanupLease> escorts = new LinkedHashMap<>();
		for (Map.Entry<String, CleanupLease> entry : state.getCleanupLeases().entrySet()) {
			CleanupLease lease = entry.getValue();
			if (!"QUEST_ESCORT".equals(lease.capability())) {
				continue;
			}
			if (!(lease.identity() instanceof PlayerQuestGraphState.EscortResourceIdentity identity)
					|| !identity.materialized() || identity.playerId() != playerId || identity.questId() != state.getQuestId()
					|| !entry.getKey().equals(lease.resourceKey())) {
				throw new IllegalArgumentException("Persisted escort lease owner or identity is invalid");
			}
			escorts.put(entry.getKey(), lease);
		}
		return escorts;
	}

	private static PlayerQuestGraphState copy(PlayerQuestGraphState state, Map<String, CleanupLease> cleanupLeases) {
		return new PlayerQuestGraphState(state.getQuestId(), state.getDefinitionVersion(), Math.addExact(state.getRevision(), 1), state.getNodeId(),
			state.getQuestStatus(), state.getHistory(), state.getInstanceRunId(), state.getLifecycle(), state.getVariables(), state.getDeadlines(),
			state.getJournal(), cleanupLeases, state.getQuarantineReason());
	}

	private static Player requirePlayer(Player player) {
		return Objects.requireNonNull(player, "player");
	}
}
