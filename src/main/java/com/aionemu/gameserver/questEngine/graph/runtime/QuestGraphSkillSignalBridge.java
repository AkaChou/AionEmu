package com.aionemu.gameserver.questEngine.graph.runtime;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.SkillDuplicatePolicy;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.SkillUsedEvent;
import com.aionemu.gameserver.questEngine.graph.runtime.QuestGraphEvent.SkillUseSource;

/** 把技能服务 authority 快照转换为 typed event，并执行 owner 级旧去重合同。 / Converts skill-service authority into typed events and enforces legacy owner-level deduplication. */
public final class QuestGraphSkillSignalBridge {

	/** 禁止实例化纯静态 bridge。 / Prevents instantiation of this static bridge. */
	private QuestGraphSkillSignalBridge() {
	}

	/** 保存一次服务端已接受技能使用的不可变快照。 / Holds an immutable snapshot of one server-accepted skill use. */
	public record SkillUseSnapshot(int playerId, long serverUseId, int skillId, int skillLevel, int targetObjectId, int worldId,
			int instanceId, SkillUseSource source, boolean serverExecutionAccepted) {
		/** 校验技能服务提供的结构边界。 / Validates structural boundaries supplied by the skill service. */
		public SkillUseSnapshot {
			if (playerId <= 0 || serverUseId <= 0 || skillId <= 0 || skillLevel <= 0 || targetObjectId < 0 || worldId <= 0
					|| instanceId <= 0 || source == null) {
				throw new IllegalArgumentException("Skill-use snapshot is invalid");
			}
		}
	}

	/** 创建只由服务端已接受执行授权的 skill-use 事件。 / Creates a skill-use event authorized only by server-accepted execution. */
	public static SkillUsedEvent skillUsed(String eventId, long occurredAt, SkillUseSnapshot snapshot) {
		if (snapshot == null) {
			throw new IllegalArgumentException("Skill-use snapshot is missing");
		}
		return new SkillUsedEvent(eventId, snapshot.playerId(), occurredAt, snapshot.serverUseId(), snapshot.skillId(),
			snapshot.skillLevel(), snapshot.targetObjectId(), snapshot.worldId(), snapshot.instanceId(), snapshot.source(),
			snapshot.serverExecutionAccepted());
	}

	/** 实现旧 XML SkillUse 的临时 `(player, owner, skill)` 500ms 去重与显式 cleanup。 / Implements legacy XML SkillUse temporary 500ms deduplication and explicit cleanup. */
	public static final class DeduplicationGate {
		private static final long LEGACY_WINDOW_MILLIS = 500;
		private static final long CLEANUP_AGE_MILLIS = 30_000;
		private static final int CLEANUP_THRESHOLD = 1000;
		private final Map<Key, AcceptedSignal> acceptedSignals = new HashMap<>();

		/** 在 owner 处于 START 时执行选定重复策略；其他状态不占用窗口。 / Applies the selected duplicate policy only for a START owner; other states do not consume the window. */
		public synchronized boolean allow(int questId, QuestStatus questStatus, SkillDuplicatePolicy policy, SkillUsedEvent event) {
			if (questId <= 0 || questStatus == null || policy == null || event == null) {
				throw new IllegalArgumentException("Skill-use deduplication input is invalid");
			}
			if (policy == SkillDuplicatePolicy.RAW_SOURCE || questStatus != QuestStatus.START) {
				return true;
			}
			Key key = new Key(event.playerId(), questId, event.skillId());
			AcceptedSignal previous = acceptedSignals.get(key);
			if (previous != null && previous.eventId().equals(event.eventId())) {
				return true;
			}
			if (previous != null && event.occurredAt() - previous.occurredAt() < LEGACY_WINDOW_MILLIS) {
				return false;
			}
			acceptedSignals.put(key, new AcceptedSignal(event.eventId(), event.occurredAt()));
			if (acceptedSignals.size() > CLEANUP_THRESHOLD) {
				long cutoff = event.occurredAt() - CLEANUP_AGE_MILLIS;
				acceptedSignals.entrySet().removeIf(entry -> entry.getValue().occurredAt() < cutoff);
			}
			return true;
		}

		/** 清理玩家登出后的全部临时窗口。 / Clears every temporary window after player logout. */
		public synchronized void clearPlayer(int playerId) {
			if (playerId <= 0) {
				throw new IllegalArgumentException("Player id must be positive");
			}
			Iterator<Key> iterator = acceptedSignals.keySet().iterator();
			while (iterator.hasNext()) {
				if (iterator.next().playerId() == playerId) {
					iterator.remove();
				}
			}
		}

		/** 清理 owner reload/retirement 后的全部临时窗口。 / Clears every temporary window after owner reload or retirement. */
		public synchronized void clearQuest(int questId) {
			if (questId <= 0) {
				throw new IllegalArgumentException("Quest id must be positive");
			}
			acceptedSignals.keySet().removeIf(key -> key.questId() == questId);
		}

		/** 返回当前临时窗口数量，仅用于确定性测试和审计。 / Returns the current temporary-window count for deterministic tests and audit. */
		public synchronized int size() {
			return acceptedSignals.size();
		}
	}

	/** 定义旧模板去重的稳定临时 scope 键。 / Defines the stable temporary-scope key for legacy-template deduplication. */
	private record Key(int playerId, int questId, int skillId) {
	}

	/** 保存最近一次被接受信号的稳定事件标识与时间。 / Holds the stable event id and time of the most recently accepted signal. */
	private record AcceptedSignal(String eventId, long occurredAt) {
	}
}
