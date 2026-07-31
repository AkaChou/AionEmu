package com.aionemu.gameserver.questEngine.graph.state;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.aionemu.gameserver.model.items.ItemId;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.EscortSource;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.QuestStatus;
import com.aionemu.gameserver.questEngine.graph.CompiledQuestGraph.StartEscortAction;

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

	/** 定义冻结物品动作的封闭语义。 / Defines the closed semantics of a frozen item action. */
	public enum ItemMutationKind {
		GIVE_TOP_UP_TO,
		GIVE_ADD_EXACT,
		REMOVE_EXACT,
		REMOVE_OPTIONAL_EXACT,
		REMOVE_ALL,
		PAY_KINAH_AND_ITEM,
		REMOVE_EVENT_OBJECT_EXACT,
		REMOVE_EVENT_TEMPLATE_EXACT
	}

	/**
	 * 保存 PREPARED 前按动作序号冻结的物品数量转换。
	 * Holds an item-count transition frozen by action index before PREPARED persistence.
	 */
	public record ItemMutationPlan(int actionIndex, ItemMutationKind kind, int itemId, int itemObjectId, long requestedCount,
			long beforeCount, long afterCount, long beforeObjectCount, long kinahAmount, long beforeKinah, long afterKinah) {
		/** 保留模板级动作的旧构造签名。 / Preserves the legacy constructor for template-level mutations. */
		public ItemMutationPlan(int actionIndex, ItemMutationKind kind, int itemId, long requestedCount, long beforeCount, long afterCount) {
			this(actionIndex, kind, itemId, 0, requestedCount, beforeCount, afterCount, 0, 0, 0, 0);
		}

		/** 保留事件对象动作的旧构造签名。 / Preserves the event-object mutation constructor. */
		public ItemMutationPlan(int actionIndex, ItemMutationKind kind, int itemId, int itemObjectId, long requestedCount,
				long beforeCount, long afterCount, long beforeObjectCount) {
			this(actionIndex, kind, itemId, itemObjectId, requestedCount, beforeCount, afterCount, beforeObjectCount, 0, 0, 0);
		}

		/** 创建 Kinah 与普通物品的原子支付计划。 / Creates an atomic Kinah-and-ordinary-item payment plan. */
		public static ItemMutationPlan payment(int actionIndex, int itemId, long itemCount, long beforeCount, long kinahAmount,
				long beforeKinah) {
			return new ItemMutationPlan(actionIndex, ItemMutationKind.PAY_KINAH_AND_ITEM, itemId, 0, itemCount, beforeCount,
				Math.subtractExact(beforeCount, itemCount), 0, kinahAmount, beforeKinah, Math.subtractExact(beforeKinah, kinahAmount));
		}

		/** 校验动作索引、物品引用和 before/after 关系。 / Validates the action index, item reference, and before/after relation. */
		public ItemMutationPlan {
			if (actionIndex < 0 || kind == null || itemId <= 0 || itemObjectId < 0 || requestedCount <= 0
					|| beforeCount < 0 || afterCount < 0 || beforeObjectCount < 0 || kinahAmount < 0 || beforeKinah < 0 || afterKinah < 0) {
				throw new IllegalArgumentException("Item mutation plan is invalid");
			}
			boolean valid = switch (kind) {
				case GIVE_TOP_UP_TO -> itemObjectId == 0 && beforeObjectCount == 0
					&& afterCount == Math.max(beforeCount, requestedCount);
				case GIVE_ADD_EXACT -> itemObjectId == 0 && beforeObjectCount == 0
					&& afterCount == Math.addExact(beforeCount, requestedCount);
				case REMOVE_EXACT -> itemObjectId == 0 && beforeObjectCount == 0 && beforeCount >= requestedCount
					&& afterCount == beforeCount - requestedCount;
				case REMOVE_OPTIONAL_EXACT -> itemObjectId == 0 && beforeObjectCount == 0
					&& afterCount == (beforeCount >= requestedCount ? beforeCount - requestedCount : beforeCount);
				case REMOVE_ALL -> itemObjectId == 0 && beforeObjectCount == 0 && afterCount == 0;
				case PAY_KINAH_AND_ITEM -> itemId != ItemId.KINAH.value() && itemObjectId == 0 && beforeObjectCount == 0
					&& beforeCount >= requestedCount
					&& afterCount == beforeCount - requestedCount && kinahAmount > 0 && beforeKinah >= kinahAmount
					&& afterKinah == beforeKinah - kinahAmount;
				case REMOVE_EVENT_OBJECT_EXACT -> itemObjectId > 0 && beforeObjectCount >= requestedCount
					&& beforeObjectCount <= beforeCount && beforeCount >= requestedCount
					&& afterCount == beforeCount - requestedCount;
				case REMOVE_EVENT_TEMPLATE_EXACT -> itemObjectId == 0 && beforeObjectCount == 0
					&& beforeCount >= requestedCount && afterCount == beforeCount - requestedCount;
			};
			if (!valid || kind != ItemMutationKind.PAY_KINAH_AND_ITEM && (kinahAmount != 0 || beforeKinah != 0 || afterKinah != 0)) {
				throw new IllegalArgumentException("Item mutation before/after counts do not match its semantics");
			}
		}
	}

	/**
	 * 保存 PREPARED 前冻结的 Kinah+普通物品原子支付计划。
	 * Holds a Kinah-and-ordinary-item atomic payment plan frozen before PREPARED.
	 */
	public record PaymentPlan(int actionIndex, int itemId, long itemCount, long kinah,
		long beforeItemCount, long afterItemCount, long beforeKinah, long afterKinah) {
		/** 校验支付数量及 before/after 收敛关系。 / Validates payment quantities and before/after convergence. */
		public PaymentPlan {
			if (actionIndex < 0 || itemId <= 0 || itemId == ItemId.KINAH.value() || itemCount <= 0 || kinah <= 0
					|| beforeItemCount < itemCount || beforeKinah < kinah
					|| afterItemCount != beforeItemCount - itemCount || afterKinah != beforeKinah - kinah) {
				throw new IllegalArgumentException("Payment plan is invalid");
			}
		}
	}

	/**
	 * 保存 ITEM_USE 延迟屏障、冻结事件物品身份和由定义版本约束的 tail 边界。
	 * Holds an ITEM_USE delay barrier, frozen event-item identity, and tail bounds constrained by the definition version.
	 */
	public record ItemUseContinuationPlan(int actionIndex, int itemId, int itemObjectId, int durationMs, long readyAt,
			int tailStartActionIndex, int tailEndActionIndex) {
		/** 校验正数身份、绝对时间和非空 tail。 / Validates positive identity, absolute time, and a non-empty tail. */
		public ItemUseContinuationPlan {
			if (actionIndex < 0 || itemId <= 0 || itemObjectId <= 0 || durationMs <= 0 || readyAt <= 0
					|| tailStartActionIndex != actionIndex + 1 || tailEndActionIndex < tailStartActionIndex) {
				throw new IllegalArgumentException("Item-use continuation plan is invalid");
			}
		}
	}

	/**
	 * 保存 PREPARED 前冻结的完整传送命令，不表示需要清理的持久资源。
	 * Holds a complete teleport command frozen before PREPARED persistence and does not represent a cleanup-owned resource.
	 */
	public record TeleportPlan(int actionIndex, int worldId, int instanceId, float x, float y, float z, byte heading) {
		/** 校验动作索引、世界、instance 和有限坐标。 / Validates the action index, world, instance, and finite coordinates. */
		public TeleportPlan {
			if (actionIndex < 0 || worldId <= 0 || instanceId < 0
					|| !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
				throw new IllegalArgumentException("Teleport plan is invalid");
			}
		}
	}

	/**
	 * 保存一个协议动作应发送的冻结任务状态与打包变量。
	 * Holds the frozen quest status and packed variables that one protocol action must send.
	 */
	public record QuestStatusSyncSnapshot(int actionIndex, int snapshotAfterActionCount, QuestStatus status, int packedQuestVars) {
		/** 校验动作索引、状态 checkpoint 与 canonical 状态。 / Validates the action index, state checkpoint, and canonical status. */
		public QuestStatusSyncSnapshot {
			if (actionIndex < 0 || snapshotAfterActionCount < 0 || status == null) {
				throw new IllegalArgumentException("Quest status sync snapshot is invalid");
			}
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
		/** 目标节点是否已在协议投递前提交。 / Whether the target node was committed before protocol delivery. */
		private final boolean targetCommitted;
		/** 在副作用前解析并冻结的 repeat deadline 结果。 / Repeat-deadline resolution frozen before side effects. */
		private final RepeatDeadlineResolution repeatDeadlineResolution;
		/** 按动作序号排序的冻结物品动作。 / Frozen item actions ordered by action index. */
		private final Map<Integer, ItemMutationPlan> itemMutationPlans;
		/** 按动作序号排序的冻结 Kinah+物品支付动作。 / Frozen Kinah-and-item payments ordered by action index. */
		private final Map<Integer, PaymentPlan> paymentPlans;
		/** 按动作序号排序的冻结传送命令。 / Frozen teleport commands ordered by action index. */
		private final Map<Integer, TeleportPlan> teleportPlans;
		/** 按屏障动作序号排序的冻结物品使用 continuation。 / Frozen item-use continuations ordered by barrier action index. */
		private final Map<Integer, ItemUseContinuationPlan> itemUseContinuationPlans;
		/** 按协议动作序号排序的冻结任务状态快照。 / Frozen quest-status snapshots ordered by protocol action index. */
		private final Map<Integer, QuestStatusSyncSnapshot> questStatusSyncSnapshots;
		/** 冻结任务状态快照及其 journal 身份的可选 SHA-256 摘要。 / Optional SHA-256 digest of frozen snapshots and their journal identity. */
		@Getter(AccessLevel.NONE)
		private final byte[] questStatusSyncSnapshotDigest;
		/** 类型化事件 codec 生成的不可变负载。 / Immutable payload produced by the typed event codec. */
		@Getter(AccessLevel.NONE)
		private final byte[] eventPayload;

		/**
		 * 创建已准备转换并复制事件负载。
		 * Creates a prepared transition and copies its event payload.
		 */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex, byte[] eventPayload) {
			this(baseRevision, eventId, transitionId, nextActionIndex, false, RepeatDeadlineResolution.NOT_APPLICABLE, Map.of(), Map.of(), Map.of(),
				new byte[0], eventPayload);
		}

		/**
		 * 创建带已解析 repeat deadline 结果的 journal，并复制事件负载。
		 * Creates a journal with a resolved repeat-deadline outcome and copies its event payload.
		 */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex,
				RepeatDeadlineResolution repeatDeadlineResolution, byte[] eventPayload) {
			this(baseRevision, eventId, transitionId, nextActionIndex, false, repeatDeadlineResolution, Map.of(), Map.of(), Map.of(), new byte[0],
				eventPayload);
		}

		/**
		 * 创建带 repeat deadline 与冻结物品计划的 journal。
		 * Creates a journal with repeat-deadline and frozen item plans.
		 */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex,
				RepeatDeadlineResolution repeatDeadlineResolution, Map<Integer, ItemMutationPlan> itemMutationPlans, byte[] eventPayload) {
			this(baseRevision, eventId, transitionId, nextActionIndex, false, repeatDeadlineResolution, itemMutationPlans, Map.of(), Map.of(), new byte[0],
				eventPayload);
		}

		/** 创建带冻结物品与传送计划的 journal。 / Creates a journal with frozen item and teleport plans. */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex,
				RepeatDeadlineResolution repeatDeadlineResolution, Map<Integer, ItemMutationPlan> itemMutationPlans,
				Map<Integer, TeleportPlan> teleportPlans, byte[] eventPayload) {
			this(baseRevision, eventId, transitionId, nextActionIndex, false, repeatDeadlineResolution, itemMutationPlans, teleportPlans, Map.of(),
				new byte[0], eventPayload);
		}

		/** 创建带全部冻结动作输入和协议快照的 journal。 / Creates a journal with every frozen action input and protocol snapshot. */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex,
				RepeatDeadlineResolution repeatDeadlineResolution, Map<Integer, ItemMutationPlan> itemMutationPlans,
				Map<Integer, TeleportPlan> teleportPlans, Map<Integer, QuestStatusSyncSnapshot> questStatusSyncSnapshots, byte[] eventPayload) {
			this(baseRevision, eventId, transitionId, nextActionIndex, false, repeatDeadlineResolution, itemMutationPlans, teleportPlans,
				questStatusSyncSnapshots, new byte[0], eventPayload);
		}

		/** 创建带 snapshot 完整性摘要的 journal。 / Creates a journal with a snapshot-integrity digest. */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex,
				RepeatDeadlineResolution repeatDeadlineResolution, Map<Integer, ItemMutationPlan> itemMutationPlans,
				Map<Integer, TeleportPlan> teleportPlans, Map<Integer, QuestStatusSyncSnapshot> questStatusSyncSnapshots,
				byte[] questStatusSyncSnapshotDigest, byte[] eventPayload) {
			this(baseRevision, eventId, transitionId, nextActionIndex, false, repeatDeadlineResolution, itemMutationPlans, teleportPlans,
				questStatusSyncSnapshots, questStatusSyncSnapshotDigest, eventPayload);
		}

		/** 创建可恢复到已提交目标节点的 journal。 / Creates a journal that can resume after the target node was committed. */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex, boolean targetCommitted,
				RepeatDeadlineResolution repeatDeadlineResolution, Map<Integer, ItemMutationPlan> itemMutationPlans, byte[] eventPayload) {
			this(baseRevision, eventId, transitionId, nextActionIndex, targetCommitted, repeatDeadlineResolution, itemMutationPlans, Map.of(), Map.of(),
				new byte[0], eventPayload);
		}

		/** 创建包含全部冻结动作输入的可恢复 journal。 / Creates a recoverable journal containing every frozen action input. */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex, boolean targetCommitted,
				RepeatDeadlineResolution repeatDeadlineResolution, Map<Integer, ItemMutationPlan> itemMutationPlans,
				Map<Integer, TeleportPlan> teleportPlans, byte[] eventPayload) {
			this(baseRevision, eventId, transitionId, nextActionIndex, targetCommitted, repeatDeadlineResolution, itemMutationPlans, teleportPlans,
				Map.of(), new byte[0], eventPayload);
		}

		/** 创建包含全部冻结动作输入与协议快照的可恢复 journal。 / Creates a recoverable journal with all frozen action inputs and protocol snapshots. */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex, boolean targetCommitted,
				RepeatDeadlineResolution repeatDeadlineResolution, Map<Integer, ItemMutationPlan> itemMutationPlans,
				Map<Integer, TeleportPlan> teleportPlans, Map<Integer, QuestStatusSyncSnapshot> questStatusSyncSnapshots, byte[] eventPayload) {
			this(baseRevision, eventId, transitionId, nextActionIndex, targetCommitted, repeatDeadlineResolution, itemMutationPlans, teleportPlans,
				questStatusSyncSnapshots, new byte[0], eventPayload);
		}

		/** 创建带 snapshot 完整性摘要的完整可恢复 journal。 / Creates a complete recoverable journal with a snapshot-integrity digest. */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex, boolean targetCommitted,
				RepeatDeadlineResolution repeatDeadlineResolution, Map<Integer, ItemMutationPlan> itemMutationPlans,
				Map<Integer, TeleportPlan> teleportPlans, Map<Integer, QuestStatusSyncSnapshot> questStatusSyncSnapshots,
				byte[] questStatusSyncSnapshotDigest, byte[] eventPayload) {
			this(baseRevision, eventId, transitionId, nextActionIndex, targetCommitted, repeatDeadlineResolution, itemMutationPlans,
				teleportPlans, Map.of(), questStatusSyncSnapshots, questStatusSyncSnapshotDigest, eventPayload);
		}

		/** 创建带 durable item-use continuation 的完整可恢复 journal。 / Creates a complete recoverable journal with durable item-use continuations. */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex, boolean targetCommitted,
				RepeatDeadlineResolution repeatDeadlineResolution, Map<Integer, ItemMutationPlan> itemMutationPlans,
				Map<Integer, TeleportPlan> teleportPlans, Map<Integer, ItemUseContinuationPlan> itemUseContinuationPlans,
				Map<Integer, QuestStatusSyncSnapshot> questStatusSyncSnapshots, byte[] questStatusSyncSnapshotDigest, byte[] eventPayload) {
			this(baseRevision, eventId, transitionId, nextActionIndex, targetCommitted, repeatDeadlineResolution, itemMutationPlans,
				Map.of(), teleportPlans, itemUseContinuationPlans, questStatusSyncSnapshots, questStatusSyncSnapshotDigest, eventPayload);
		}

		/** 创建包含 Kinah+普通物品支付计划的完整可恢复 journal。 / Creates a complete recoverable journal with payment plans. */
		public PreparedTransition(long baseRevision, String eventId, String transitionId, int nextActionIndex, boolean targetCommitted,
				RepeatDeadlineResolution repeatDeadlineResolution, Map<Integer, ItemMutationPlan> itemMutationPlans,
				Map<Integer, PaymentPlan> paymentPlans, Map<Integer, TeleportPlan> teleportPlans,
				Map<Integer, ItemUseContinuationPlan> itemUseContinuationPlans,
				Map<Integer, QuestStatusSyncSnapshot> questStatusSyncSnapshots, byte[] questStatusSyncSnapshotDigest, byte[] eventPayload) {
			if (baseRevision < -1 || nextActionIndex < 0) {
				throw new IllegalArgumentException("Prepared transition base revision/action index is invalid");
			}
			this.baseRevision = baseRevision;
			this.eventId = requireText(eventId, "event id");
			this.transitionId = requireText(transitionId, "transition id");
			this.nextActionIndex = nextActionIndex;
			this.targetCommitted = targetCommitted;
			this.repeatDeadlineResolution = java.util.Objects.requireNonNull(repeatDeadlineResolution, "repeatDeadlineResolution");
			TreeMap<Integer, ItemMutationPlan> plans = new TreeMap<>();
			if (itemMutationPlans != null) {
				itemMutationPlans.forEach((index, plan) -> {
					if (index == null || plan == null || index != plan.actionIndex() || plans.putIfAbsent(index, plan) != null) {
						throw new IllegalArgumentException("Prepared item mutation plans are invalid");
					}
				});
			}
			this.itemMutationPlans = Collections.unmodifiableMap(plans);
			TreeMap<Integer, PaymentPlan> frozenPayments = new TreeMap<>();
			if (paymentPlans != null) {
				paymentPlans.forEach((index, plan) -> {
					ItemMutationPlan itemPlan = index == null ? null : plans.get(index);
					if (index == null || plan == null || index != plan.actionIndex() || frozenPayments.putIfAbsent(index, plan) != null
							|| itemPlan == null || itemPlan.kind() != ItemMutationKind.PAY_KINAH_AND_ITEM
							|| itemPlan.itemId() != plan.itemId() || itemPlan.requestedCount() != plan.itemCount()
							|| itemPlan.kinahAmount() != plan.kinah() || itemPlan.beforeCount() != plan.beforeItemCount()
							|| itemPlan.afterCount() != plan.afterItemCount() || itemPlan.beforeKinah() != plan.beforeKinah()
							|| itemPlan.afterKinah() != plan.afterKinah()) {
						throw new IllegalArgumentException("Prepared payment plans are invalid");
					}
				});
			}
			if (plans.values().stream().anyMatch(itemPlan -> itemPlan.kind() == ItemMutationKind.PAY_KINAH_AND_ITEM
					&& !frozenPayments.containsKey(itemPlan.actionIndex()))) {
				throw new IllegalArgumentException("Prepared payment item plan is missing its typed payment plan");
			}
			this.paymentPlans = Collections.unmodifiableMap(frozenPayments);
			TreeMap<Integer, TeleportPlan> frozenTeleports = new TreeMap<>();
			if (teleportPlans != null) {
				teleportPlans.forEach((index, plan) -> {
					if (index == null || plan == null || index != plan.actionIndex() || frozenTeleports.putIfAbsent(index, plan) != null) {
						throw new IllegalArgumentException("Prepared teleport plans are invalid");
					}
				});
			}
			this.teleportPlans = Collections.unmodifiableMap(frozenTeleports);
			TreeMap<Integer, ItemUseContinuationPlan> frozenContinuations = new TreeMap<>();
			if (itemUseContinuationPlans != null) {
				itemUseContinuationPlans.forEach((index, plan) -> {
					if (index == null || plan == null || index != plan.actionIndex()
							|| frozenContinuations.putIfAbsent(index, plan) != null) {
						throw new IllegalArgumentException("Prepared item-use continuation plans are invalid");
					}
				});
			}
			this.itemUseContinuationPlans = Collections.unmodifiableMap(frozenContinuations);
			TreeMap<Integer, QuestStatusSyncSnapshot> syncSnapshots = new TreeMap<>();
			if (questStatusSyncSnapshots != null) {
				questStatusSyncSnapshots.forEach((index, snapshot) -> {
					if (index == null || snapshot == null || index != snapshot.actionIndex()
							|| syncSnapshots.putIfAbsent(index, snapshot) != null) {
						throw new IllegalArgumentException("Prepared quest status sync snapshots are invalid");
					}
				});
			}
			this.questStatusSyncSnapshots = Collections.unmodifiableMap(syncSnapshots);
			if (questStatusSyncSnapshotDigest != null && questStatusSyncSnapshotDigest.length != 0
					&& questStatusSyncSnapshotDigest.length != 32) {
				throw new IllegalArgumentException("Quest status sync snapshot digest must be empty or SHA-256 length");
			}
			this.questStatusSyncSnapshotDigest = questStatusSyncSnapshotDigest == null ? new byte[0]
				: Arrays.copyOf(questStatusSyncSnapshotDigest, questStatusSyncSnapshotDigest.length);
			this.eventPayload = eventPayload == null ? new byte[0] : Arrays.copyOf(eventPayload, eventPayload.length);
		}

		/** 返回 snapshot 摘要副本，防止 journal 完整性材料被修改。 / Returns a digest copy so journal integrity material cannot be mutated. */
		public byte[] getQuestStatusSyncSnapshotDigest() {
			return Arrays.copyOf(questStatusSyncSnapshotDigest, questStatusSyncSnapshotDigest.length);
		}

		/**
		 * 返回事件负载副本，防止 journal 被外部修改。
		 * Returns a copy of the event payload so callers cannot mutate the journal.
		 */
		public byte[] getEventPayload() {
			return Arrays.copyOf(eventPayload, eventPayload.length);
		}
	}

	/** 冻结的 NPC 生成位置种类，独立于 runtime adapter 实现。 / Frozen NPC spawn placement kind, independent of runtime adapters. */
	public enum SpawnPlacementKind {
		STATIC_SPAWN,
		DIALOG_TARGET,
		PLAYER,
		FIXED
	}

	/** cleanup ledger 支持的封闭资源身份。 / Closed resource identities supported by the cleanup ledger. */
	public sealed interface ResourceIdentity permits InstanceSpawnResourceIdentity, EscortResourceIdentity {
		int playerId();

		int questId();

		int objectId();

		int npcId();

		int worldId();

		int instanceId();

		String idempotencyKey();

		default boolean materialized() {
			return objectId() > 0;
		}
	}

	/**
	 * 保存副本 NPC 的冻结生成计划或已物化身份；objectId=0 表示 PREPARED 计划。
	 * Holds a frozen instance-NPC plan or materialized identity; objectId=0 denotes a PREPARED plan.
	 */
	public record InstanceSpawnResourceIdentity(int playerId, int questId, int objectId, int npcId,
			SpawnPlacementKind placement, int sourceNpcId, int sourceObjectId, int worldId, int instanceId,
			float x, float y, float z, byte heading, String idempotencyKey) implements ResourceIdentity {
		public InstanceSpawnResourceIdentity {
			if (playerId <= 0 || questId <= 0 || objectId < 0 || npcId <= 0 || placement == null
					|| sourceNpcId < 0 || sourceObjectId < 0 || worldId <= 0 || instanceId < 0
					|| !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
					|| placement == SpawnPlacementKind.STATIC_SPAWN && (sourceNpcId <= 0 || sourceObjectId != 0)
					|| placement == SpawnPlacementKind.DIALOG_TARGET && (sourceNpcId <= 0 || sourceObjectId <= 0)
					|| placement == SpawnPlacementKind.PLAYER && (sourceNpcId != 0 || sourceObjectId != playerId)
					|| placement == SpawnPlacementKind.FIXED && (sourceNpcId != 0 || sourceObjectId != 0)) {
				throw new IllegalArgumentException("Instance spawn resource identity is invalid");
			}
			idempotencyKey = requireText(idempotencyKey, "resource idempotency key");
		}

		public InstanceSpawnResourceIdentity materialize(int spawnedObjectId) {
			if (spawnedObjectId <= 0) {
				throw new IllegalArgumentException("Spawned object id is invalid");
			}
			return new InstanceSpawnResourceIdentity(playerId, questId, spawnedObjectId, npcId, placement, sourceNpcId,
				sourceObjectId, worldId, instanceId, x, y, z, heading, idempotencyKey);
		}
	}

	/**
	 * 保存 escort 的冻结恢复计划或已物化 follower 身份；完整 action 保留 AI、walker 与目的地参数。
	 * Holds a frozen escort recovery plan or materialized follower identity; the full action retains AI, walker, and destination parameters.
	 */
	public record EscortResourceIdentity(int playerId, int questId, int objectId, int npcId, int worldId, int instanceId, float x, float y, float z,
			int eventNpcId, int eventNpcObjectId, boolean spawnedFollower, String previousWalkerId,
			StartEscortAction action, String idempotencyKey) implements ResourceIdentity {
		public EscortResourceIdentity {
			if (playerId <= 0 || questId <= 0 || objectId < 0 || npcId <= 0 || worldId <= 0 || instanceId < 0
					|| !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)
					|| eventNpcId < 0 || eventNpcObjectId < 0 || (eventNpcId == 0) != (eventNpcObjectId == 0)
					|| action == null || spawnedFollower != (action.source() != EscortSource.EVENT_NPC)
					|| npcId != (spawnedFollower ? action.npcId() : eventNpcId) || objectId == 0 && previousWalkerId != null) {
				throw new IllegalArgumentException("Escort resource identity is invalid");
			}
			previousWalkerId = previousWalkerId == null ? null : requireText(previousWalkerId, "previous walker id");
			idempotencyKey = requireText(idempotencyKey, "resource idempotency key");
		}

		public EscortResourceIdentity materialize(int followerObjectId, String walkerBeforeStart) {
			if (followerObjectId <= 0) {
				throw new IllegalArgumentException("Follower object id is invalid");
			}
			return new EscortResourceIdentity(playerId, questId, followerObjectId, npcId, worldId, instanceId, x, y, z,
				eventNpcId, eventNpcObjectId, spawnedFollower, walkerBeforeStart, action, idempotencyKey);
		}
	}

	/**
	 * 表示 cleanup ledger 中由类型化能力持有的稳定资源。旧 payload 的 identity 为 null 且只能 fail closed。
	 * Represents a stable resource held by a typed capability. Legacy payloads have a null identity and must fail closed.
	 */
	public record CleanupLease(String capability, String resourceKey, ResourceIdentity identity) {
		/** 保留旧调用方；该形式没有可恢复身份。 / Retains legacy callers; this form has no recoverable identity. */
		public CleanupLease(String capability, String resourceKey) {
			this(capability, resourceKey, null);
		}

		public static CleanupLease instanceSpawn(InstanceSpawnResourceIdentity identity) {
			return new CleanupLease("INSTANCE_SCOPED_SPAWN", identity.idempotencyKey(), identity);
		}

		public static CleanupLease escort(EscortResourceIdentity identity) {
			return new CleanupLease("QUEST_ESCORT", identity.idempotencyKey(), identity);
		}

		/**
		 * 校验 cleanup 能力与资源键。
		 * Validates the cleanup capability and resource key.
		 */
		public CleanupLease {
			capability = requireText(capability, "cleanup capability");
			resourceKey = requireText(resourceKey, "cleanup resource key");
			if (identity != null && (!resourceKey.equals(identity.idempotencyKey())
					|| identity instanceof InstanceSpawnResourceIdentity && !"INSTANCE_SCOPED_SPAWN".equals(capability)
					|| identity instanceof EscortResourceIdentity && !"QUEST_ESCORT".equals(capability))) {
				throw new IllegalArgumentException("Cleanup lease capability or resource key does not match its typed identity");
			}
		}

		public boolean resolved() {
			return identity != null;
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
		this.cleanupLeases = immutableCleanupLeases(cleanupLeases, questId);
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
		if (journal != null && lifecycle != Lifecycle.PREPARED) {
			throw new IllegalArgumentException("Only PREPARED state may contain a journal");
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
				expectedRevision = Math.addExact(Math.addExact(journal.getBaseRevision(), journal.getNextActionIndex()),
					journal.isTargetCommitted() ? 2 : 1);
			} catch (ArithmeticException e) {
				throw new IllegalArgumentException("Prepared transition revision overflows", e);
			}
			if (expectedRevision != revision) {
				throw new IllegalArgumentException("Prepared transition revision does not match journal progress");
			}
		}
		if (lifecycle != Lifecycle.PREPARED && cleanupLeases.values().stream()
				.anyMatch(lease -> lease.identity() != null && !lease.identity().materialized())) {
			throw new IllegalArgumentException("Only PREPARED state may contain frozen resource plans");
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
	private static Map<String, CleanupLease> immutableCleanupLeases(Map<String, CleanupLease> source, int questId) {
		TreeMap<String, CleanupLease> result = new TreeMap<>();
		if (source != null) {
			source.forEach((name, lease) -> {
				String leaseId = requireText(name, "cleanup lease id");
				CleanupLease validated = java.util.Objects.requireNonNull(lease, "cleanup lease");
				if (validated.identity() != null && (!leaseId.equals(validated.resourceKey())
						|| validated.identity().questId() != questId)) {
					throw new IllegalArgumentException("Typed cleanup lease key or quest owner does not match its state");
				}
				result.put(leaseId, validated);
			});
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
