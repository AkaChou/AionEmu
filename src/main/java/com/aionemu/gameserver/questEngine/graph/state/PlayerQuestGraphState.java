package com.aionemu.gameserver.questEngine.graph.state;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;

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
	 * 区分无 repeat policy、已计算 deadline 与旧 Handler 的高权限绕过。
	 * Distinguishes no repeat policy, a calculated deadline, and the legacy Handler's privileged bypass.
	 */
	public enum RepeatDeadlineDisposition {
		NOT_APPLICABLE,
		DEADLINE,
		PRIVILEGED_BYPASS
	}

	/**
	 * 保存副作用前冻结的 repeat deadline 解析结果。
	 * Holds the repeat-deadline resolution frozen before side effects.
	 */
	public record RepeatDeadlineResolution(RepeatDeadlineDisposition disposition, Long deadlineAt) {
		/** 无 repeat policy 的规范结果。 / Canonical result for no repeat policy. */
		public static final RepeatDeadlineResolution NOT_APPLICABLE =
			new RepeatDeadlineResolution(RepeatDeadlineDisposition.NOT_APPLICABLE, null);
		/** 高权限账号跳过 daily/weekly deadline 的规范结果。 / Canonical result for privileged daily/weekly bypass. */
		public static final RepeatDeadlineResolution PRIVILEGED_BYPASS =
			new RepeatDeadlineResolution(RepeatDeadlineDisposition.PRIVILEGED_BYPASS, null);

		/** 校验 disposition 与绝对时间的一致性。 / Validates consistency between the disposition and absolute timestamp. */
		public RepeatDeadlineResolution {
			if (disposition == null || disposition == RepeatDeadlineDisposition.DEADLINE != (deadlineAt != null)
					|| deadlineAt != null && deadlineAt <= 0) {
				throw new IllegalArgumentException("Repeat deadline resolution is invalid");
			}
		}

		/** 创建带正数 Unix 毫秒值的 deadline 结果。 / Creates a deadline result with a positive Unix-millisecond value. */
		public static RepeatDeadlineResolution deadline(long deadlineAt) {
			return new RepeatDeadlineResolution(RepeatDeadlineDisposition.DEADLINE, deadlineAt);
		}
	}

	/**
	 * 保存跨重复周期保留的 canonical 任务历史。
	 * Holds canonical quest history retained across repeat cycles.
	 */
	public record QuestHistory(int completionCount, int lastRewardIndex, Long completedAt, Long nextRepeatAt,
		RepeatDeadlineDisposition repeatDeadlineDisposition) {
		/** 没有完成历史的规范值。 / Canonical value for no completion history. */
		public static final QuestHistory EMPTY = new QuestHistory(0, 0, null, null, RepeatDeadlineDisposition.NOT_APPLICABLE);

		/**
		 * 从旧四字段调用推导无 deadline 或已计算 deadline 的 disposition。
		 * Infers the no-deadline or calculated-deadline disposition for legacy four-field callers.
		 */
		public QuestHistory(int completionCount, int lastRewardIndex, Long completedAt, Long nextRepeatAt) {
			this(completionCount, lastRewardIndex, completedAt, nextRepeatAt,
				nextRepeatAt == null ? RepeatDeadlineDisposition.NOT_APPLICABLE : RepeatDeadlineDisposition.DEADLINE);
		}

		/**
		 * 校验完成次数、奖励索引和时间戳的一致性。
		 * Validates consistency of completion count, reward index, and timestamps.
		 */
		public QuestHistory {
			if (completionCount < 0 || lastRewardIndex < 0) {
				throw new IllegalArgumentException("Quest history counts must be non-negative");
			}
			if ((completedAt != null && completedAt <= 0) || (nextRepeatAt != null && nextRepeatAt <= 0)) {
				throw new IllegalArgumentException("Quest history timestamps must be positive");
			}
			if (repeatDeadlineDisposition == null
					|| repeatDeadlineDisposition == RepeatDeadlineDisposition.DEADLINE != (nextRepeatAt != null)) {
				throw new IllegalArgumentException("Quest history repeat deadline disposition is invalid");
			}
			if (completionCount == 0 && (lastRewardIndex != 0 || completedAt != null || nextRepeatAt != null)) {
				throw new IllegalArgumentException("Empty quest history cannot contain completion metadata");
			}
			if (completionCount == 0 && repeatDeadlineDisposition != RepeatDeadlineDisposition.NOT_APPLICABLE) {
				throw new IllegalArgumentException("Empty quest history cannot contain repeat deadline metadata");
			}
			if (completionCount > 0 && completedAt == null) {
				throw new IllegalArgumentException("Completed quest history requires a completion timestamp");
			}
		}
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
		/** 在副作用前解析并冻结的 repeat deadline 结果。 / Repeat-deadline resolution frozen before side effects. */
		private final RepeatDeadlineResolution repeatDeadlineResolution;
		/** 类型化事件 codec 生成的不可变负载。 / Immutable payload produced by the typed event codec. */
		@Getter(AccessLevel.NONE)
		private final byte[] eventPayload;

		/**
		 * 创建已准备转换并复制事件负载。
		 * Creates a prepared transition and copies its event payload.
		 */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex, byte[] eventPayload) {
			this(baseRevision, eventId, transitionId, nextActionIndex, RepeatDeadlineResolution.NOT_APPLICABLE, eventPayload);
		}

		/**
		 * 创建带已解析 repeat deadline 结果的 journal，并复制事件负载。
		 * Creates a journal with a resolved repeat-deadline outcome and copies its event payload.
		 */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex,
				RepeatDeadlineResolution repeatDeadlineResolution, byte[] eventPayload) {
			if (baseRevision < -1 || nextActionIndex < 0) {
				throw new IllegalArgumentException("Prepared transition base revision/action index is invalid");
			}
			this.baseRevision = baseRevision;
			this.eventId = requireText(eventId, "event id");
			this.transitionId = requireText(transitionId, "transition id");
			this.nextActionIndex = nextActionIndex;
			this.repeatDeadlineResolution = java.util.Objects.requireNonNull(repeatDeadlineResolution, "repeatDeadlineResolution");
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
	/** 当前 canonical 任务生命周期状态。 / Current canonical quest lifecycle status. */
	private final QuestStatus questStatus;
	/** 跨重复周期保留的 canonical 完成历史。 / Canonical completion history retained across repeat cycles. */
	private final QuestHistory history;
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
	public PlayerQuestGraphState(int questId, int definitionVersion, long revision, String nodeId, QuestStatus questStatus, QuestHistory history,
			Long instanceRunId,
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
		this.questStatus = java.util.Objects.requireNonNull(questStatus, "questStatus");
		this.history = java.util.Objects.requireNonNull(history, "history");
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
	 * 返回图 deadline 与下次可重复时间中的最早值；都不存在时返回 null。
	 * Returns the earliest graph deadline or repeat time, or null when neither exists.
	 */
	public Long nextDeadlineAt() {
		Long graphDeadline = deadlines.values().stream().min(Long::compareTo).orElse(null);
		Long repeatDeadline = history.nextRepeatAt();
		if (graphDeadline == null) {
			return repeatDeadline;
		}
		return repeatDeadline == null ? graphDeadline : Math.min(graphDeadline, repeatDeadline);
	}

	/**
	 * 校验 lifecycle、journal 和隔离原因的一致性。
	 * Validates consistency between lifecycle, journal, and quarantine reason.
	 */
	private void validateLifecycle() {
		if (questStatus == QuestStatus.COMPLETE && history.completionCount() == 0) {
			throw new IllegalArgumentException("COMPLETE state requires completion history");
		}
		if (questStatus == QuestStatus.LOCKED && !history.equals(QuestHistory.EMPTY)) {
			throw new IllegalArgumentException("LOCKED state cannot contain completion history");
		}
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
