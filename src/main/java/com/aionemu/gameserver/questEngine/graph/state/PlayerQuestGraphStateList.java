package com.aionemu.gameserver.questEngine.graph.state;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * 保存玩家全部任务图状态及等待持久化的删除标识。
 * Holds all quest graph states for a player and deletion identifiers awaiting persistence.
 */
public final class PlayerQuestGraphStateList {

	private final Map<Integer, PlayerQuestGraphState> states = new TreeMap<>();
	private final Set<Integer> deletedQuestIds = new TreeSet<>();

	/**
	 * 添加数据库加载的状态并拒绝重复 owner。
	 * Adds a database-loaded state and rejects duplicate owners.
	 */
	public synchronized void addLoaded(PlayerQuestGraphState state) {
		if (states.putIfAbsent(state.getQuestId(), state) != null) {
			throw new IllegalArgumentException("Duplicate player quest graph state " + state.getQuestId());
		}
	}

	/**
	 * 新增或替换任务图状态，并取消同一任务尚未保存的删除。
	 * Adds or replaces a quest graph state and cancels an unpersisted deletion for the same quest.
	 */
	public synchronized void put(PlayerQuestGraphState state) {
		PlayerQuestGraphState current = states.get(state.getQuestId());
		if (current == null && state.getRevision() != 0) {
			throw new IllegalArgumentException("New quest graph state must start at revision 0");
		}
		if (current != null && (current.getRevision() == Long.MAX_VALUE || state.getRevision() != current.getRevision() + 1)) {
			throw new IllegalArgumentException("Quest graph state must advance exactly one revision");
		}
		states.put(state.getQuestId(), state);
		deletedQuestIds.remove(state.getQuestId());
	}

	/**
	 * 返回指定任务的图状态。
	 * Returns the graph state for the given quest.
	 */
	public synchronized PlayerQuestGraphState get(int questId) {
		return states.get(questId);
	}

	/**
	 * 删除任务图状态并记录数据库删除意图。
	 * Removes a quest graph state and records the database deletion intent.
	 */
	public synchronized boolean remove(int questId) {
		if (states.remove(questId) == null) {
			return false;
		}
		deletedQuestIds.add(questId);
		return true;
	}

	/**
	 * 返回按任务标识排序的不可变状态快照。
	 * Returns an immutable state snapshot ordered by quest identifier.
	 */
	public synchronized Collection<PlayerQuestGraphState> snapshot() {
		return List.copyOf(states.values());
	}

	/**
	 * 返回按任务标识排序的待删除快照。
	 * Returns a deletion snapshot ordered by quest identifier.
	 */
	public synchronized Set<Integer> deletedQuestIds() {
		return Collections.unmodifiableSet(new TreeSet<>(deletedQuestIds));
	}

	/**
	 * 在数据库事务提交后确认已完成的删除。
	 * Acknowledges completed deletions after the database transaction commits.
	 */
	public synchronized void acknowledgeDeleted(Set<Integer> questIds) {
		deletedQuestIds.removeAll(questIds);
	}
}
