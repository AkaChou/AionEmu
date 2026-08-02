package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.Npc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

/**
 * 领域持有的 quest NPC handle 注册表。
 *
 * <p>key = {@code playerId:questId:slot}。slot 由任务编译期常量决定,despawn 只能通过
 * slot 反引用本注册表里 spawn 过的权威 handle;禁止凭 templateId 删任意同类,也禁止把
 * handle/实体状态编码进 quest_vars。任务完成/失败/实例销毁时按 (playerId, questId)
 * 清理,避免孤儿 NPC。</p>
 */
public final class QuestSpawnRegistry {
	private static final QuestSpawnRegistry GLOBAL = new QuestSpawnRegistry();
	private static final Comparator<Key> KEY_ORDER = Comparator.comparingInt(Key::playerId)
		.thenComparingInt(Key::questId).thenComparing(Key::slot);
	private final ConcurrentMap<Key, Npc> spawns = new ConcurrentHashMap<>();
	private final ConcurrentMap<Key, Future<?>> followTasks = new ConcurrentHashMap<>();

	public QuestSpawnRegistry() {
	}

	public static QuestSpawnRegistry global() {
		return GLOBAL;
	}

	/**
	 * 幂等注册:slot 已存在时跳过并返回 false。
	 *
	 * @return true 表示本次注册成功; false 表示该 slot 已有 handle (跳过, 不重复刷怪)
	 */
	public boolean register(QuestSnapshot snapshot, String slot, Npc npc) {
		Objects.requireNonNull(snapshot, "snapshot");
		if (slot == null || slot.isBlank()) {
			throw new IllegalArgumentException("slot must not be blank");
		}
		Objects.requireNonNull(npc, "npc");
		Npc existing = spawns.putIfAbsent(key(snapshot, slot), npc);
		return existing == null;
	}

	/**
	 * 权威反引用并删除该 slot 的 NPC (由调用方执行 onDelete)。
	 *
	 * @return 被删除的 handle,或 null 表示该 slot 无 handle
	 */
	public Npc remove(QuestSnapshot snapshot, String slot) {
		Objects.requireNonNull(snapshot, "snapshot");
		if (slot == null || slot.isBlank()) {
			throw new IllegalArgumentException("slot must not be blank");
		}
		Key key = key(snapshot, slot);
		cancelFollowTask(key);
		return spawns.remove(key);
	}

	/** 该 slot 当前是否已注册 handle。 */
	public boolean contains(QuestSnapshot snapshot, String slot) {
		return spawns.containsKey(key(snapshot, slot));
	}

	/** 只读反查该 slot 的权威 handle;未注册返回 null (不删除)。 */
	public Npc get(QuestSnapshot snapshot, String slot) {
		return spawns.get(key(snapshot, slot));
	}

	/** Replaces the follow checker associated with one authoritative spawn slot. */
	public boolean registerFollowTask(QuestSnapshot snapshot, String slot, Future<?> task) {
		Objects.requireNonNull(task, "task");
		Key key = key(snapshot, slot);
		if (!spawns.containsKey(key)) {
			task.cancel(false);
			return false;
		}
		Future<?> previous = followTasks.put(key, task);
		if (previous != null) {
			previous.cancel(false);
		}
		if (!spawns.containsKey(key)) {
			followTasks.remove(key, task);
			task.cancel(false);
			return false;
		}
		return true;
	}

	/**
	 * 按 (playerId, questId) 清理该任务全部 spawn (完成/失败/实例销毁时调用),
	 * 返回被清理的 handle 供调用方逐个 onDelete。
	 */
	public List<Npc> cleanup(int playerId, int questId) {
		if (playerId <= 0 || questId <= 0) {
			throw new IllegalArgumentException("playerId and questId must be positive");
		}
		return cleanupMatching(key -> key.playerId == playerId && key.questId == questId);
	}

	public List<Npc> cleanupPlayer(int playerId) {
		if (playerId <= 0) {
			throw new IllegalArgumentException("playerId must be positive");
		}
		return cleanupMatching(key -> key.playerId == playerId);
	}

	public List<Npc> cleanupInstance(int instanceId) {
		if (instanceId <= 0) {
			throw new IllegalArgumentException("instanceId must be positive");
		}
		return cleanupMatching(key -> {
			Npc npc = spawns.get(key);
			return npc != null && npc.getPosition() != null && npc.getInstanceId() == instanceId;
		});
	}

	public List<Npc> cleanupAll() {
		return cleanupMatching(key -> true);
	}

	private List<Npc> cleanupMatching(java.util.function.Predicate<Key> predicate) {
		List<Npc> removed = new ArrayList<>();
		List<Map.Entry<Key, Npc>> matches = spawns.entrySet().stream()
			.filter(entry -> predicate.test(entry.getKey()))
			.sorted(Map.Entry.comparingByKey(KEY_ORDER))
			.toList();
		for (Map.Entry<Key, Npc> entry : matches) {
			if (spawns.remove(entry.getKey(), entry.getValue())) {
				removed.add(entry.getValue());
				cancelFollowTask(entry.getKey());
			}
		}
		List<Map.Entry<Key, Future<?>>> tasks = followTasks.entrySet().stream()
			.filter(entry -> predicate.test(entry.getKey()))
			.sorted(Map.Entry.comparingByKey(KEY_ORDER))
			.toList();
		for (Map.Entry<Key, Future<?>> entry : tasks) {
			if (followTasks.remove(entry.getKey(), entry.getValue())) {
				entry.getValue().cancel(false);
			}
		}
		return removed;
	}

	private void cancelFollowTask(Key key) {
		Future<?> task = followTasks.remove(key);
		if (task != null) {
			task.cancel(false);
		}
	}

	private static Key key(QuestSnapshot snapshot, String slot) {
		Objects.requireNonNull(snapshot, "snapshot");
		if (slot == null || slot.isBlank()) {
			throw new IllegalArgumentException("slot must not be blank");
		}
		return new Key(snapshot.playerId(), snapshot.questId(), slot);
	}

	private record Key(int playerId, int questId, String slot) {
	}
}
