package com.aionemu.gameserver.questEngine.graph.state;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * 保存单个玩家任务图的不可变、可持久化运行状态。
 * Holds the immutable, persistable runtime state of one player quest graph.
 */
@Getter
public final class PlayerQuestGraphState {

	/**
	 * 表示状态是否可执行、正在恢复转换或已隔离。
	 * Indicates whether the state is executable, recovering a transition, or quarantined.
	 */
	public enum Lifecycle {
		ACTIVE,
		PREPARED,
		QUARANTINED
	}

	/**
	 * 定义可持久化任务变量支持的强类型集合。
	 * Defines the supported typed set of persistent quest variables.
	 */
	public sealed interface VariableValue permits IntValue, BooleanValue {
	}

	/**
	 * 表示整数任务变量值。
	 * Represents an integer quest variable value.
	 */
	public record IntValue(int value) implements VariableValue {
	}

	/**
	 * 表示布尔任务变量值。
	 * Represents a boolean quest variable value.
	 */
	public record BooleanValue(boolean value) implements VariableValue {
	}

	/**
	 * 保存崩溃恢复所需的已准备转换位置和事件快照。
	 * Holds the prepared transition position and event snapshot required for crash recovery.
	 */
	@Getter
	public static final class PreparedTransition {
		/** 基础状态 revision。 / Base state revision. */
		private final long baseRevision;
		/** 稳定事件标识。 / Stable event identifier. */
		private final String eventId;
		/** 任务图转换标识。 / Quest graph transition identifier. */
		private final String transitionId;
		/** 下一个待执行动作索引。 / Index of the next action to execute. */
		private final int nextActionIndex;
		/** 类型化事件 codec 生成的不可变负载。 / Immutable payload produced by the typed event codec. */
		@Getter(AccessLevel.NONE)
		private final byte[] eventPayload;

		/**
		 * 创建已准备转换并复制事件负载。
		 * Creates a prepared transition and copies its event payload.
		 */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex, byte[] eventPayload) {
			if (baseRevision < -1 || nextActionIndex < 0) {
				throw new IllegalArgumentException("Prepared transition base revision/action index is invalid");
			}
			this.baseRevision = baseRevision;
			this.eventId = requireText(eventId, "event id");
			this.transitionId = requireText(transitionId, "transition id");
			this.nextActionIndex = nextActionIndex;
			this.eventPayload = eventPayload == null ? new byte[0] : Arrays.copyOf(eventPayload, eventPayload.length);
		}

		/**
		 * 返回事件负载副本，防止 journal 被外部修改。
		 * Returns a copy of the event payload so callers cannot mutate the journal.
		 */
		public byte[] getEventPayload() {
			return Arrays.copyOf(eventPayload, eventPayload.length);
		}
	}

	/**
	 * 表示 cleanup ledger 中由类型化能力持有的稳定资源。
	 * Represents a stable resource held by a typed capability in the cleanup ledger.
	 */
	public record CleanupLease(String capability, String resourceKey) {
		/**
		 * 校验 cleanup 能力与资源键。
		 * Validates the cleanup capability and resource key.
		 */
		public CleanupLease {
			capability = requireText(capability, "cleanup capability");
			resourceKey = requireText(resourceKey, "cleanup resource key");
		}
	}

	/** 任务所有者标识。 / Quest owner identifier. */
	private final int questId;
	/** XML 定义版本。 / XML definition version. */
	private final int definitionVersion;
	/** 单调递增状态 revision。 / Monotonically increasing state revision. */
	private final long revision;
	/** 当前任务图节点。 / Current quest graph node. */
	private final String nodeId;
	/** 可选副本运行标识。 / Optional instance-run identifier. */
	private final Long instanceRunId;
	/** 当前恢复生命周期。 / Current recovery lifecycle. */
	private final Lifecycle lifecycle;
	/** 按名称排序的强类型变量。 / Typed variables ordered by name. */
	private final Map<String, VariableValue> variables;
	/** 按名称排序的绝对到期时间（Unix 毫秒）。 / Absolute deadlines ordered by name in Unix milliseconds. */
	private final Map<String, Long> deadlines;
	/** 可选的 PREPARED journal。 / Optional PREPARED journal. */
	private final PreparedTransition journal;
	/** 按 lease 标识排序的清理账本。 / Cleanup ledger ordered by lease identifier. */
	private final Map<String, CleanupLease> cleanupLeases;
	/** 隔离原因，仅 QUARANTINED 状态允许。 / Quarantine reason, allowed only for QUARANTINED state. */
	private final String quarantineReason;

	/**
	 * 创建并完整校验不可变玩家任务图状态。
	 * Creates and fully validates an immutable player quest graph state.
	 */
	public PlayerQuestGraphState(int questId, int definitionVersion, long revision, String nodeId, Long instanceRunId,
			Lifecycle lifecycle, Map<String, VariableValue> variables, Map<String, Long> deadlines, PreparedTransition journal,
			Map<String, CleanupLease> cleanupLeases, String quarantineReason) {
		if (questId <= 0 || definitionVersion <= 0 || revision < 0) {
			throw new IllegalArgumentException("Quest id/version must be positive and revision must be non-negative");
		}
		if (instanceRunId != null && instanceRunId <= 0) {
			throw new IllegalArgumentException("Instance run id must be positive");
		}
		this.questId = questId;
		this.definitionVersion = definitionVersion;
		this.revision = revision;
		this.nodeId = requireText(nodeId, "node id");
		if (this.nodeId.length() > 128) {
			throw new IllegalArgumentException("Node id exceeds the persisted 128-character limit");
		}
		this.instanceRunId = instanceRunId;
		this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
		this.variables = immutableVariables(variables);
		this.deadlines = immutableDeadlines(deadlines);
		this.journal = journal;
		this.cleanupLeases = immutableCleanupLeases(cleanupLeases);
		this.quarantineReason = quarantineReason;
		validateLifecycle();
	}

	/**
	 * 返回最早绝对 deadline；没有 deadline 时返回 null。
	 * Returns the earliest absolute deadline, or null when none exists.
	 */
	public Long nextDeadlineAt() {
		return deadlines.values().stream().min(Long::compareTo).orElse(null);
	}

	/**
	 * 校验 lifecycle、journal 和隔离原因的一致性。
	 * Validates consistency between lifecycle, journal, and quarantine reason.
	 */
	private void validateLifecycle() {
		if (lifecycle == Lifecycle.ACTIVE && (journal != null || quarantineReason != null)) {
			throw new IllegalArgumentException("ACTIVE state cannot contain journal/quarantine reason");
		}
		if (lifecycle == Lifecycle.PREPARED && (journal == null || quarantineReason != null)) {
			throw new IllegalArgumentException("PREPARED state requires only a journal");
		}
		if (lifecycle == Lifecycle.QUARANTINED && (quarantineReason == null || quarantineReason.isBlank())) {
			throw new IllegalArgumentException("QUARANTINED state requires a reason");
		}
		if (lifecycle != Lifecycle.QUARANTINED && quarantineReason != null) {
			throw new IllegalArgumentException("Only QUARANTINED state may contain a reason");
		}
		if (journal != null) {
			long expectedRevision;
			try {
				expectedRevision = Math.addExact(Math.addExact(journal.getBaseRevision(), journal.getNextActionIndex()), 1);
			} catch (ArithmeticException e) {
				throw new IllegalArgumentException("Prepared transition revision overflows", e);
			}
			if (expectedRevision != revision) {
				throw new IllegalArgumentException("Prepared transition revision does not match journal progress");
			}
		}
	}

	/**
	 * 校验并按名称复制强类型变量。
	 * Validates and copies typed variables in name order.
	 */
	private static Map<String, VariableValue> immutableVariables(Map<String, VariableValue> source) {
		TreeMap<String, VariableValue> result = new TreeMap<>();
		if (source != null) {
			source.forEach((name, value) -> result.put(requireText(name, "variable name"),
				java.util.Objects.requireNonNull(value, "variable value")));
		}
		return Collections.unmodifiableMap(result);
	}

	/**
	 * 校验并按名称复制绝对 deadline。
	 * Validates and copies absolute deadlines in name order.
	 */
	private static Map<String, Long> immutableDeadlines(Map<String, Long> source) {
		TreeMap<String, Long> result = new TreeMap<>();
		if (source != null) {
			source.forEach((name, value) -> {
				if (value == null || value <= 0) {
					throw new IllegalArgumentException("Deadline must be a positive Unix-millisecond value");
				}
				result.put(requireText(name, "deadline name"), value);
			});
		}
		return Collections.unmodifiableMap(result);
	}

	/**
	 * 校验并按 lease 标识复制 cleanup ledger。
	 * Validates and copies the cleanup ledger in lease-id order.
	 */
	private static Map<String, CleanupLease> immutableCleanupLeases(Map<String, CleanupLease> source) {
		TreeMap<String, CleanupLease> result = new TreeMap<>();
		if (source != null) {
			source.forEach((name, lease) -> result.put(requireText(name, "cleanup lease id"),
				java.util.Objects.requireNonNull(lease, "cleanup lease")));
		}
		return Collections.unmodifiableMap(result);
	}

	/**
	 * 返回非空文本，否则拒绝损坏状态。
	 * Returns non-blank text or rejects corrupt state.
	 */
	private static String requireText(String value, String label) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(label + " is missing");
		}
		return value;
	}
}
