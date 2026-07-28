package com.aionemu.gameserver.questEngine.graph;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.model.ConditionOperation;
import com.aionemu.gameserver.utils.stats.AbyssRankEnum;

/**
 * 表示经过校验、可供运行时直接消费的不可变任务图。
 * Represents an immutable, validated quest graph ready for runtime consumption.
 */
public record CompiledQuestGraph(int questId, int version, StateScope scope, String initialNode, Map<String, Variable> variables,
	Map<String, Node> nodes) {

	/**
	 * 复制图集合，防止编译后的定义被调用方修改。
	 * Copies graph collections so callers cannot mutate compiled definitions.
	 */
	public CompiledQuestGraph {
		variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
		nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
	}

	/**
	 * 定义任务变量和运行状态的归属范围。
	 * Defines the ownership scope of quest variables and runtime state.
	 */
	public enum StateScope {
		PLAYER,
		PARTY,
		ALLIANCE,
		INSTANCE_RUN,
		WORLD
	}

	/**
	 * 列出当前编译器已证明并支持的事件能力。
	 * Lists event capabilities currently proven and supported by the compiler.
	 */
	public enum EventType {
		DIALOG,
		KILL
	}

	/**
	 * 列出当前编译器已证明并支持的动作能力。
	 * Lists action capabilities currently proven and supported by the compiler.
	 */
	public enum ActionType {
		START_QUEST
	}

	/**
	 * 表示任务生命周期中可供条件判断的状态。
	 * Represents quest lifecycle states available to conditions.
	 */
	public enum QuestStatus {
		NONE,
		START,
		REWARD,
		COMPLETE,
		LOCKED
	}

	/**
	 * 定义所有强类型任务变量共享的最小合同。
	 * Defines the minimal contract shared by all typed quest variables.
	 */
	public sealed interface Variable permits IntVariable, BooleanVariable {
		/**
		 * 返回变量在任务图内的唯一名称。
		 * Returns the variable's unique name within the quest graph.
		 */
		String name();

		/**
		 * 返回变量状态的归属范围。
		 * Returns the ownership scope of the variable state.
		 */
		StateScope scope();
	}

	/**
	 * 表示带显式初值和边界的整数变量。
	 * Represents an integer variable with an explicit initial value and bounds.
	 */
	public record IntVariable(String name, StateScope scope, int initial, int min, int max) implements Variable {
	}

	/**
	 * 表示带显式初值的布尔变量。
	 * Represents a boolean variable with an explicit initial value.
	 */
	public record BooleanVariable(String name, StateScope scope, boolean initial) implements Variable {
	}

	/**
	 * 表示任务图节点及其按优先级排序的出边。
	 * Represents a quest graph node and its priority-ordered outgoing transitions.
	 */
	public record Node(String id, boolean terminal, List<Transition> transitions) {
		/**
		 * 复制转换列表，保持节点定义不可变。
		 * Copies the transition list to keep the node definition immutable.
		 */
		public Node {
			transitions = List.copyOf(transitions);
		}
	}

	/**
	 * 表示由事件触发并经过条件、动作后跳转的有向边。
	 * Represents a directed edge triggered by an event and followed by conditions and actions.
	 */
	public record Transition(String id, int priority, String targetNode, Event event, List<Condition> conditions, List<Action> actions) {
		/**
		 * 复制条件和动作列表，保持转换定义不可变。
		 * Copies condition and action lists to keep the transition definition immutable.
		 */
		public Transition {
			conditions = List.copyOf(conditions);
			actions = List.copyOf(actions);
		}
	}

	/**
	 * 表示已类型化的任务事件及其目标参数。
	 * Represents a typed quest event and its target parameters.
	 */
	public record Event(EventType type, int npcId, String dialog) {
	}

	/**
	 * 表示转换执行前必须满足的已类型化条件。
	 * Represents a typed condition that must hold before a transition executes.
	 */
	public sealed interface Condition permits QuestStatusCondition, PlayerLevelCondition, PlayerRaceCondition, PlayerClassCondition,
		PlayerGenderCondition, PlayerTitleCondition, PlayerAbyssRankCondition, PlayerInventoryCondition, QuestRewardCondition,
		QuestCompletionCountCondition, PlayerEquippedCondition {
	}

	/**
	 * 要求当前或指定任务的 canonical 状态位于或不位于显式集合。
	 * Requires the current or specified quest's canonical status to be in or outside an explicit set.
	 */
	public record QuestStatusCondition(Integer questId, ConditionOperation operation, Set<QuestStatus> statuses) implements Condition {
		/** 校验任务引用、集合操作和状态集。 / Validates the quest reference, set operation, and status set. */
		public QuestStatusCondition {
			if (questId != null && questId <= 0 || operation != ConditionOperation.IN && operation != ConditionOperation.NOT_IN
					|| statuses == null || statuses.isEmpty()) {
				throw new IllegalArgumentException("Quest status condition is invalid");
			}
			statuses = Collections.unmodifiableSet(EnumSet.copyOf(statuses));
		}

		/**
		 * 创建当前 owner 的单状态包含条件。
		 * Creates a single-status inclusion condition for the current owner.
		 */
		public QuestStatusCondition(QuestStatus status) {
			this(null, ConditionOperation.IN, Set.of(status));
		}

		/**
		 * 比较实际 canonical 状态与显式状态集。
		 * Compares an actual canonical status with the explicit status set.
		 */
		public boolean matches(QuestStatus actual) {
			boolean contains = statuses.contains(actual);
			return operation == ConditionOperation.IN ? contains : !contains;
		}
	}

	/**
	 * 要求指定任务至少完成一次且末次奖励索引匹配。
	 * Requires the specified quest to have completed at least once with a matching last reward index.
	 */
	public record QuestRewardCondition(int questId, int rewardIndex) implements Condition {
		/** 校验任务引用和奖励索引。 / Validates the quest reference and reward index. */
		public QuestRewardCondition {
			if (questId <= 0 || rewardIndex < 0) {
				throw new IllegalArgumentException("Quest reward condition is invalid");
			}
		}
	}

	/**
	 * 对指定任务的 canonical 完成次数执行数值比较。
	 * Applies a numeric comparison to the specified quest's canonical completion count.
	 */
	public record QuestCompletionCountCondition(int questId, ConditionOperation operation, int count) implements Condition {
		/** 校验任务引用、数值操作和完成次数。 / Validates the quest reference, numeric operation, and completion count. */
		public QuestCompletionCountCondition {
			if (questId <= 0 || count < 0 || operation == null || operation == ConditionOperation.IN
					|| operation == ConditionOperation.NOT_IN) {
				throw new IllegalArgumentException("Quest completion-count condition is invalid");
			}
		}

		/**
		 * 将实际完成次数与配置阈值比较。
		 * Compares an actual completion count with the configured operand.
		 */
		public boolean matches(int actual) {
			return switch (operation) {
				case EQUAL -> actual == count;
				case GREATER -> actual > count;
				case GREATER_EQUAL -> actual >= count;
				case LESSER -> actual < count;
				case LESSER_EQUAL -> actual <= count;
				case NOT_EQUAL -> actual != count;
				case IN, NOT_IN -> throw new IllegalStateException("Set operation is invalid for a completion count");
			};
		}
	}

	/**
	 * 要求玩家等级位于闭区间；max 为 null 时没有上限。
	 * Requires player level within an inclusive range; a null max means no upper bound.
	 */
	public record PlayerLevelCondition(int min, Integer max) implements Condition {
		/** 校验等级闭区间。 / Validates the inclusive level range. */
		public PlayerLevelCondition {
			if (min <= 0 || max != null && max < min) {
				throw new IllegalArgumentException("Player level condition range is invalid");
			}
		}
	}

	/**
	 * 要求玩家阵营属于显式允许集合。
	 * Requires the player's race to belong to the explicit allowed set.
	 */
	public record PlayerRaceCondition(Set<Race> allowed) implements Condition {
		/**
		 * 复制允许集合，保持条件不可变。
		 * Copies the allowed set to keep the condition immutable.
		 */
		public PlayerRaceCondition {
			Objects.requireNonNull(allowed, "allowed");
			if (allowed.isEmpty() || allowed.stream().anyMatch(race -> race != Race.ELYOS && race != Race.ASMODIANS)) {
				throw new IllegalArgumentException("Player race condition is empty or contains a non-player race");
			}
			allowed = Collections.unmodifiableSet(EnumSet.copyOf(allowed));
		}
	}

	/**
	 * 要求玩家职业属于显式允许集合。
	 * Requires the player's class to belong to the explicit allowed set.
	 */
	public record PlayerClassCondition(Set<PlayerClass> allowed) implements Condition {
		/**
		 * 复制允许集合，保持条件不可变。
		 * Copies the allowed set to keep the condition immutable.
		 */
		public PlayerClassCondition {
			Objects.requireNonNull(allowed, "allowed");
			if (allowed.isEmpty() || allowed.contains(PlayerClass.ALL)) {
				throw new IllegalArgumentException("Player class condition is empty or contains ALL");
			}
			allowed = Collections.unmodifiableSet(EnumSet.copyOf(allowed));
		}
	}

	/**
	 * 要求玩家性别等于期望值。
	 * Requires the player's gender to equal the expected value.
	 */
	public record PlayerGenderCondition(Gender expected) implements Condition {
		/** 校验玩家性别且拒绝创建占位值。 / Validates player gender and rejects the creation-only placeholder. */
		public PlayerGenderCondition {
			if (expected == null || expected == Gender.DUMMY) {
				throw new IllegalArgumentException("Player gender condition is invalid");
			}
		}
	}

	/**
	 * 要求玩家持有指定称号。
	 * Requires the player to own a specific title.
	 */
	public record PlayerTitleCondition(int titleId) implements Condition {
		/** 校验称号模板 ID。 / Validates the title template id. */
		public PlayerTitleCondition {
			if (titleId <= 0) {
				throw new IllegalArgumentException("Player title condition id is invalid");
			}
		}
	}

	/**
	 * 要求玩家达到指定最低深渊军衔。
	 * Requires the player to reach a specified minimum Abyss rank.
	 */
	public record PlayerAbyssRankCondition(AbyssRankEnum minimum) implements Condition {
		/** 校验最低军衔。 / Validates the minimum rank. */
		public PlayerAbyssRankCondition {
			Objects.requireNonNull(minimum, "minimum");
		}
	}

	/**
	 * 比较玩家背包中指定物品的总数量。
	 * Compares the total count of a specified item in the player's inventory.
	 */
	public record PlayerInventoryCondition(int itemId, ConditionOperation operation, long count) implements Condition {
		/** 校验物品引用、比较操作符和非负阈值。 / Validates the item reference, comparison operator, and non-negative threshold. */
		public PlayerInventoryCondition {
			if (itemId <= 0 || count < 0 || operation == null || operation == ConditionOperation.IN
					|| operation == ConditionOperation.NOT_IN) {
				throw new IllegalArgumentException("Player inventory condition is invalid");
			}
		}
	}

	/**
	 * 要求玩家当前装备指定物品。
	 * Requires the player to have the specified item currently equipped.
	 */
	public record PlayerEquippedCondition(int itemId) implements Condition {
		/** 校验装备物品引用。 / Validates the equipped-item reference. */
		public PlayerEquippedCondition {
			if (itemId <= 0) {
				throw new IllegalArgumentException("Player equipped condition id is invalid");
			}
		}
	}

	/**
	 * 表示转换命中后执行的已类型化动作。
	 * Represents a typed action executed after a transition matches.
	 */
	public record Action(ActionType type) {
	}
}
