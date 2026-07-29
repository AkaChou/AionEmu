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
		KILL,
		ATTACK,
		PLAYER_DEATH,
		KILL_IN_WORLD,
		ITEM_USE,
		ITEM_DIALOG,
		ITEM_OBTAINED,
		ITEM_EQUIPPED,
		HOUSE_ITEM_USE,
		WORLD_ENTERED,
		ZONE_ENTERED,
		ZONE_LEFT,
		ZONE_MISSION_ENDED,
		LEVEL_UP,
		PLAYER_LOGOUT,
		QUEST_TIMER_ENDED,
		MOVIE_ENDED,
		NPC_PROXIMITY,
		ESCORT_REACHED_TARGET,
		ESCORT_LOST_TARGET,
		RANKED_PLAYER_KILL,
		DREDGION_SETTLED,
		CRAFT_FAILED,
		NPC_AGGRO_LISTED,
		WINDSTREAM_ENTERED,
		FLYING_RING_PASSED,
		SKILL_USED,
		INTERACTION_ELIGIBILITY
	}

	/** 定义当前生产入口证明的封闭交互资格动作。 / Defines the closed interaction-eligibility actions proven by current production entry points. */
	public enum InteractionAction {
		ACTION_ITEM_USE
	}

	/** 定义 skill-use owner 对两个服务端入口的重复处理策略。 / Defines how a skill-use owner handles duplicate server entry points. */
	public enum SkillDuplicatePolicy {
		/** 保留每个服务端入口，匹配手写 Handler 当前行为。 / Preserves every server entry point for current handwritten Handler parity. */
		RAW_SOURCE,
		/** 按旧 XML SkillUse 模板在 500ms 内拒绝同 owner 重复信号。 / Rejects same-owner duplicates within 500ms like the legacy XML SkillUse template. */
		LEGACY_500_MILLIS
	}

	/**
	 * 列出当前编译器已证明并支持的动作能力。
	 * Lists action capabilities currently proven and supported by the compiler.
	 */
	public enum ActionType {
		START_QUEST(ActionPhase.STATE),
		START_EVENT_QUEST(ActionPhase.STATE),
		ABANDON_QUEST(ActionPhase.STATE),
		SET_QUEST_STATUS(ActionPhase.STATE),
		SET_QUEST_VARIABLE(ActionPhase.STATE),
		ADD_QUEST_VARIABLE(ActionPhase.STATE),
		INCREMENT_PACKED_COUNTER(ActionPhase.STATE),
		SET_COMPLETION_COUNT(ActionPhase.STATE),
		ADD_COMPLETION_COUNT(ActionPhase.STATE),
		GIVE_QUEST_ITEM(ActionPhase.REQUIRED),
		REMOVE_QUEST_ITEM(ActionPhase.REQUIRED),
		REMOVE_COLLECTED_ITEMS(ActionPhase.REQUIRED),
		REMOVE_QUEST_WORK_ITEMS(ActionPhase.REQUIRED),
		LEARN_RECIPE(ActionPhase.REQUIRED),
		DELETE_RECIPE(ActionPhase.REQUIRED),
		FINISH_QUEST(ActionPhase.REQUIRED),
		START_QUEST_TIMER(ActionPhase.REQUIRED),
		END_QUEST_TIMER(ActionPhase.REQUIRED),
		SEND_DIALOG(ActionPhase.POST_COMMIT_PROTOCOL),
		CLOSE_DIALOG(ActionPhase.POST_COMMIT_PROTOCOL),
		SHOW_QUEST_LIST(ActionPhase.POST_COMMIT_PROTOCOL),
		SYNC_QUEST_STATUS(ActionPhase.POST_COMMIT_PROTOCOL),
		SEND_REPEAT_DEADLINE_MESSAGE(ActionPhase.POST_COMMIT_PROTOCOL),
		SYNC_QUEST_TIMER(ActionPhase.POST_COMMIT_PROTOCOL),
		SEND_PLAYER_MESSAGE(ActionPhase.POST_COMMIT_PROTOCOL),
		PLAY_MOVIE(ActionPhase.POST_COMMIT_PROTOCOL),
		NOTIFY_RECIPE_REJECTION(ActionPhase.POST_COMMIT_PROTOCOL);

		private final ActionPhase phase;

		ActionType(ActionPhase phase) {
			this.phase = phase;
		}

		/** 返回动作的固定执行阶段。 / Returns the action's fixed execution phase. */
		public ActionPhase phase() {
			return phase;
		}
	}

	/**
	 * 定义状态、必需副作用和提交后协议三个固定动作阶段。
	 * Defines the fixed state, required-side-effect, and post-commit protocol action phases.
	 */
	public enum ActionPhase {
		STATE,
		REQUIRED,
		POST_COMMIT_PROTOCOL
	}

	/**
	 * 定义玩家消息投影当前支持的客户端频道。
	 * Defines client channels currently supported by player-message projection.
	 */
	public enum PlayerMessageChannel {
		BRIGHT_YELLOW_CENTER
	}

	/** 定义 repeat deadline 使用的时间基准。 / Defines the time basis used by repeat deadlines. */
	public enum RepeatTimeBasis {
		SERVER_LOCAL
	}

	/** 定义高权限玩家完成计时任务时的 deadline 行为。 / Defines deadline behavior when a privileged player completes a timed quest. */
	public enum RepeatPrivilegeMode {
		NOT_APPLICABLE,
		BYPASS_FOR_PRIVILEGED,
		ENFORCE_FOR_PRIVILEGED
	}

	/** 定义当前任务数据使用的星期标识。 / Defines weekday identifiers used by current quest data. */
	public enum RepeatWeekday {
		MON(1),
		TUE(2),
		WED(3),
		THU(4),
		FRI(5),
		SAT(6),
		SUN(7);

		private final int dayOfWeek;

		RepeatWeekday(int dayOfWeek) {
			this.dayOfWeek = dayOfWeek;
		}

		/** 返回 ISO-8601 星期值，周一为 1。 / Returns the ISO-8601 weekday value where Monday is 1. */
		public int dayOfWeek() {
			return dayOfWeek;
		}
	}

	/** 定义完成任务后计算下次可重复时间的封闭策略集合。 / Defines the closed set of post-completion repeat-deadline policies. */
	public sealed interface RepeatDeadlinePolicy permits NoRepeatDeadlinePolicy, DailyRepeatDeadlinePolicy,
		WeeklyRepeatDeadlinePolicy, AnchoredCooldownRepeatDeadlinePolicy {

		/** 返回该策略对高权限玩家的显式处理方式。 / Returns the policy's explicit handling of privileged players. */
		default RepeatPrivilegeMode privilegeMode() {
			return switch (this) {
				case NoRepeatDeadlinePolicy ignored -> RepeatPrivilegeMode.NOT_APPLICABLE;
				case DailyRepeatDeadlinePolicy ignored -> RepeatPrivilegeMode.BYPASS_FOR_PRIVILEGED;
				case WeeklyRepeatDeadlinePolicy ignored -> RepeatPrivilegeMode.BYPASS_FOR_PRIVILEGED;
				case AnchoredCooldownRepeatDeadlinePolicy ignored -> RepeatPrivilegeMode.ENFORCE_FOR_PRIVILEGED;
			};
		}
	}

	/** 表示完成后不生成 repeat deadline。 / Represents completion without a repeat deadline. */
	public enum NoRepeatDeadlinePolicy implements RepeatDeadlinePolicy {
		INSTANCE
	}

	/** 在服务器本地每日固定小时重置。 / Resets at a fixed server-local hour every day. */
	public record DailyRepeatDeadlinePolicy(RepeatTimeBasis timeBasis, int resetHour) implements RepeatDeadlinePolicy {
		/** 校验显式时间基准和小时。 / Validates the explicit time basis and hour. */
		public DailyRepeatDeadlinePolicy {
			if (timeBasis == null || resetHour < 0 || resetHour > 23) {
				throw new IllegalArgumentException("Daily repeat deadline policy is invalid");
			}
		}
	}

	/** 在服务器本地指定星期的固定小时重置。 / Resets at a fixed server-local hour on selected weekdays. */
	public record WeeklyRepeatDeadlinePolicy(RepeatTimeBasis timeBasis, Set<RepeatWeekday> weekdays, int resetHour)
		implements RepeatDeadlinePolicy {
		/** 校验并复制星期集合、时间基准和小时。 / Validates and copies weekdays, time basis, and hour. */
		public WeeklyRepeatDeadlinePolicy {
			if (timeBasis == null || weekdays == null || weekdays.isEmpty() || weekdays.stream().anyMatch(java.util.Objects::isNull)
					|| resetHour < 0 || resetHour > 23) {
				throw new IllegalArgumentException("Weekly repeat deadline policy is invalid");
			}
			weekdays = Set.copyOf(weekdays);
		}
	}

	/** 从服务器本地当日锚点增加冷却秒数。 / Adds cooldown seconds to the current server-local day's anchor. */
	public record AnchoredCooldownRepeatDeadlinePolicy(RepeatTimeBasis timeBasis, long cooldownSeconds, int anchorHour)
		implements RepeatDeadlinePolicy {
		/** 校验显式时间基准、正冷却和锚点小时。 / Validates the time basis, positive cooldown, and anchor hour. */
		public AnchoredCooldownRepeatDeadlinePolicy {
			if (timeBasis == null || cooldownSeconds <= 0 || anchorHour < 0 || anchorHour > 23) {
				throw new IllegalArgumentException("Anchored cooldown repeat deadline policy is invalid");
			}
		}
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
	public record Event(EventType type, int targetId, String qualifier) {
	}

	/**
	 * 表示转换执行前必须满足的已类型化条件。
	 * Represents a typed condition that must hold before a transition executes.
	 */
	public sealed interface Condition permits QuestStatusCondition, QuestVariableCondition, QuestRepeatAvailableCondition,
		PackedCounterCondition, InvasionWorldActiveCondition,
		QuestCollectItemsCondition, RecipeLearnableCondition, PlayerLevelCondition, KillVictimLevelDeltaCondition, PlayerRaceCondition, PlayerClassCondition,
		PlayerGenderCondition, PlayerTitleCondition, PlayerAbyssRankCondition, PlayerInventoryCondition, QuestRewardCondition,
		QuestCompletionCountCondition, PlayerEquippedCondition {
	}

	/**
	 * 比较当前任务的强类型整数变量。
	 * Compares a typed integer variable of the current quest.
	 */
	public record QuestVariableCondition(String variable, ConditionOperation operation, int value) implements Condition {
		/** 校验变量名和数值比较操作。 / Validates the variable name and numeric comparison operation. */
		public QuestVariableCondition {
			if (variable == null || variable.isBlank() || operation == null || operation == ConditionOperation.IN
					|| operation == ConditionOperation.NOT_IN) {
				throw new IllegalArgumentException("Quest variable condition is invalid");
			}
		}
	}

	/**
	 * 比较由低位到高位整数变量组成的定基数计数器。
	 * Compares a fixed-radix counter composed of low-to-high integer variables.
	 */
	public record PackedCounterCondition(List<String> variables, int radix, ConditionOperation operation, int value) implements Condition {
		/** 校验变量序列、基数、操作和非负阈值。 / Validates the variable sequence, radix, operation, and non-negative operand. */
		public PackedCounterCondition {
			variables = validatedPackedVariables(variables, radix);
			if (operation == null || operation == ConditionOperation.IN || operation == ConditionOperation.NOT_IN || value < 0) {
				throw new IllegalArgumentException("Packed counter condition is invalid");
			}
		}
	}

	/** 要求 WORLD_ENTERED 快照确认目标世界的漩涡或裂隙在事件发生时活跃。 / Requires the WORLD_ENTERED snapshot to confirm active vortex or rift access for the target world. */
	public record InvasionWorldActiveCondition(int worldId) implements Condition {
		/** 校验正数世界标识。 / Validates a positive world identifier. */
		public InvasionWorldActiveCondition {
			if (worldId <= 0) {
				throw new IllegalArgumentException("Invasion world condition is invalid");
			}
		}
	}

	/**
	 * 比较当前 canonical 任务状态是否允许开始新的重复周期。
	 * Compares whether the current canonical quest state allows a new repeat cycle.
	 */
	public record QuestRepeatAvailableCondition(int maxCompletions, boolean requiresDeadline, boolean expectedAvailable) implements Condition {
		/** 校验重复次数上限；255 表示旧任务模型中的无限重复。 / Validates the repeat cap; 255 means unlimited in the legacy model. */
		public QuestRepeatAvailableCondition {
			if (maxCompletions <= 0 || maxCompletions > 255) {
				throw new IllegalArgumentException("Quest repeat limit is invalid");
			}
		}
	}

	/**
	 * 要求玩家持有 quest_data 中声明的全部交付物品；条件本身不扣除物品。
	 * Requires all delivery items declared by quest_data; the condition itself never removes items.
	 */
	public record QuestCollectItemsCondition() implements Condition {
	}

	/** 要求 recipe typed bridge 判定当前玩家可或不可学习指定配方。 / Requires the recipe typed bridge to confirm whether the player can learn a recipe. */
	public record RecipeLearnableCondition(int recipeId, boolean expected) implements Condition {
		/** 校验正数配方引用。 / Validates a positive recipe reference. */
		public RecipeLearnableCondition {
			if (recipeId <= 0) {
				throw new IllegalArgumentException("Recipe learnable condition is invalid");
			}
		}
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
	 * 比较当前玩家等级与 KILL_IN_WORLD 服务端受害者快照的差值闭区间。
	 * Compares the current player level minus the server-authoritative KILL_IN_WORLD victim level against an inclusive range.
	 */
	public record KillVictimLevelDeltaCondition(Integer min, Integer max) implements Condition {
		/** 校验至少一个边界存在且闭区间有效。 / Validates that at least one bound exists and the inclusive range is valid. */
		public KillVictimLevelDeltaCondition {
			if (min == null && max == null || min != null && max != null && max < min) {
				throw new IllegalArgumentException("Kill-victim level delta condition range is invalid");
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
	 * 定义转换动作的强类型封闭集合。
	 * Defines the closed typed set of transition actions.
	 */
	public sealed interface Action permits StartQuestAction, StartEventQuestAction, AbandonQuestAction, SetQuestStatusAction, SetQuestVariableAction,
		AddQuestVariableAction, IncrementPackedCounterAction,
		SetCompletionCountAction, AddCompletionCountAction, GiveQuestItemAction, RemoveQuestItemAction, RemoveCollectedItemsAction,
		RemoveQuestWorkItemsAction, LearnRecipeAction, DeleteRecipeAction, NotifyRecipeRejectionAction, FinishQuestAction,
		StartQuestTimerAction, EndQuestTimerAction, SendDialogAction, CloseDialogAction, ShowQuestListAction, SyncQuestStatusAction,
		SendRepeatDeadlineMessageAction, SyncQuestTimerAction, SendPlayerMessageAction, PlayMovieAction {

		/** 返回动作种类及其固定执行阶段。 / Returns the action kind and its fixed execution phase. */
		default ActionType type() {
			return switch (this) {
				case StartQuestAction ignored -> ActionType.START_QUEST;
				case StartEventQuestAction ignored -> ActionType.START_EVENT_QUEST;
				case AbandonQuestAction ignored -> ActionType.ABANDON_QUEST;
				case SetQuestStatusAction ignored -> ActionType.SET_QUEST_STATUS;
				case SetQuestVariableAction ignored -> ActionType.SET_QUEST_VARIABLE;
				case AddQuestVariableAction ignored -> ActionType.ADD_QUEST_VARIABLE;
				case IncrementPackedCounterAction ignored -> ActionType.INCREMENT_PACKED_COUNTER;
				case SetCompletionCountAction ignored -> ActionType.SET_COMPLETION_COUNT;
				case AddCompletionCountAction ignored -> ActionType.ADD_COMPLETION_COUNT;
				case GiveQuestItemAction ignored -> ActionType.GIVE_QUEST_ITEM;
				case RemoveQuestItemAction ignored -> ActionType.REMOVE_QUEST_ITEM;
				case RemoveCollectedItemsAction ignored -> ActionType.REMOVE_COLLECTED_ITEMS;
				case RemoveQuestWorkItemsAction ignored -> ActionType.REMOVE_QUEST_WORK_ITEMS;
				case LearnRecipeAction ignored -> ActionType.LEARN_RECIPE;
				case DeleteRecipeAction ignored -> ActionType.DELETE_RECIPE;
				case NotifyRecipeRejectionAction ignored -> ActionType.NOTIFY_RECIPE_REJECTION;
				case FinishQuestAction ignored -> ActionType.FINISH_QUEST;
				case StartQuestTimerAction ignored -> ActionType.START_QUEST_TIMER;
				case EndQuestTimerAction ignored -> ActionType.END_QUEST_TIMER;
				case SendDialogAction ignored -> ActionType.SEND_DIALOG;
				case CloseDialogAction ignored -> ActionType.CLOSE_DIALOG;
				case ShowQuestListAction ignored -> ActionType.SHOW_QUEST_LIST;
				case SyncQuestStatusAction ignored -> ActionType.SYNC_QUEST_STATUS;
				case SendRepeatDeadlineMessageAction ignored -> ActionType.SEND_REPEAT_DEADLINE_MESSAGE;
				case SyncQuestTimerAction ignored -> ActionType.SYNC_QUEST_TIMER;
				case SendPlayerMessageAction ignored -> ActionType.SEND_PLAYER_MESSAGE;
				case PlayMovieAction ignored -> ActionType.PLAY_MOVIE;
			};
		}
	}

	/** 启动或重新启动当前标准任务周期。 / Starts or restarts the current standard quest cycle. */
	public record StartQuestAction() implements Action {
	}

	/** 通过 typed lifecycle bridge 以显式状态启动活动任务 owner。 / Starts an event-quest owner with an explicit status through the typed lifecycle bridge. */
	public record StartEventQuestAction(int targetQuestId, QuestStatus status) implements Action {
		/** 校验目标 owner 和初始状态。 / Validates the target owner and initial status. */
		public StartEventQuestAction {
			if (targetQuestId <= 0 || status == null) {
				throw new IllegalArgumentException("Event quest start action is invalid");
			}
		}
	}

	/** 放弃当前任务并释放其全部 typed cleanup 资源。 / Abandons the current quest and releases all typed cleanup resources. */
	public record AbandonQuestAction() implements Action {
	}

	/** 设置当前 canonical 任务状态。 / Sets the current canonical quest status. */
	public record SetQuestStatusAction(QuestStatus status) implements Action {
		/** 拒绝空状态；COMPLETE 表示有证据的无奖励直接完成。 / Rejects null; COMPLETE represents a proven direct completion without settlement. */
		public SetQuestStatusAction {
			if (status == null) {
				throw new IllegalArgumentException("Quest status action is invalid");
			}
		}
	}

	/** 将整数任务变量设置为显式值。 / Sets an integer quest variable to an explicit value. */
	public record SetQuestVariableAction(String variable, int value) implements Action {
		/** 校验变量名。 / Validates the variable name. */
		public SetQuestVariableAction {
			if (variable == null || variable.isBlank()) {
				throw new IllegalArgumentException("Quest variable action name is missing");
			}
		}
	}

	/** 为整数任务变量增加显式增量。 / Adds an explicit delta to an integer quest variable. */
	public record AddQuestVariableAction(String variable, int delta) implements Action {
		/** 校验变量名和非零增量。 / Validates the variable name and non-zero delta. */
		public AddQuestVariableAction {
			if (variable == null || variable.isBlank() || delta == 0) {
				throw new IllegalArgumentException("Quest variable increment is invalid");
			}
		}
	}

	/**
	 * 对低位到高位整数变量执行一次有上限的定基数原子递增。
	 * Atomically increments a bounded fixed-radix counter over low-to-high integer variables.
	 */
	public record IncrementPackedCounterAction(List<String> variables, int radix, int maximum) implements Action {
		/** 校验变量序列、基数和正上限。 / Validates the variable sequence, radix, and positive maximum. */
		public IncrementPackedCounterAction {
			variables = validatedPackedVariables(variables, radix);
			if (maximum <= 0) {
				throw new IllegalArgumentException("Packed counter increment maximum is invalid");
			}
		}
	}

	/** 将 canonical 完成次数设置为显式非负值。 / Sets the canonical completion count to an explicit non-negative value. */
	public record SetCompletionCountAction(int count) implements Action {
		/** 校验非负完成次数。 / Validates a non-negative completion count. */
		public SetCompletionCountAction {
			if (count < 0) {
				throw new IllegalArgumentException("Quest completion count is invalid");
			}
		}
	}

	/** 为 canonical 完成次数增加显式非零增量。 / Adds an explicit non-zero delta to the canonical completion count. */
	public record AddCompletionCountAction(int delta) implements Action {
		/** 校验非零增量。 / Validates a non-zero delta. */
		public AddCompletionCountAction {
			if (delta == 0) {
				throw new IllegalArgumentException("Quest completion-count delta is invalid");
			}
		}
	}

	/** 定义发放任务物品时支持的封闭模式。 / Defines the closed set of supported quest-item grant modes. */
	public enum QuestItemGrantMode {
		TOP_UP_TO,
		ADD_EXACT
	}

	/** 定义移除任务物品时支持的封闭模式。 / Defines the closed set of supported quest-item removal modes. */
	public enum QuestItemRemovalMode {
		EXACT,
		OPTIONAL_EXACT
	}

	/** 把玩家背包中的任务物品补齐到显式目标总数。 / Tops a quest item in the player's inventory up to an explicit target total. */
	public record GiveQuestItemAction(int itemId, long count, QuestItemGrantMode mode) implements Action {
		/** 校验物品引用、目标总数和封闭模式。 / Validates the item reference, target total, and closed mode. */
		public GiveQuestItemAction {
			if (itemId <= 0 || count <= 0 || mode == null) {
				throw new IllegalArgumentException("Give quest item action is invalid");
			}
		}
	}

	/** 从玩家背包按封闭模式扣除显式数量的任务物品。 / Removes an explicit quest-item count using a closed removal mode. */
	public record RemoveQuestItemAction(int itemId, long count, QuestItemRemovalMode mode) implements Action {
		/** 校验物品引用、扣除数量和封闭模式。 / Validates the item reference, removal count, and closed mode. */
		public RemoveQuestItemAction {
			if (itemId <= 0 || count <= 0 || mode == null) {
				throw new IllegalArgumentException("Remove quest item action is invalid");
			}
		}
	}

	/** 扣除 quest_data 中声明的交付物品。 / Removes delivery items declared by quest_data. */
	public record RemoveCollectedItemsAction() implements Action {
	}

	/** 扣除 quest_data 中声明的全部工单过程物品。 / Removes all work-order intermediate items declared by quest_data. */
	public record RemoveQuestWorkItemsAction() implements Action {
	}

	/** 通过 recipe typed bridge 学习引用闭合的配方。 / Learns a reference-closed recipe through the recipe typed bridge. */
	public record LearnRecipeAction(int recipeId) implements Action {
		/** 校验正数配方引用。 / Validates a positive recipe reference. */
		public LearnRecipeAction {
			if (recipeId <= 0) {
				throw new IllegalArgumentException("Learn recipe action is invalid");
			}
		}
	}

	/** 通过 recipe typed bridge 删除引用闭合的临时工单配方。 / Deletes a reference-closed temporary work-order recipe through the recipe typed bridge. */
	public record DeleteRecipeAction(int recipeId) implements Action {
		/** 校验正数配方引用。 / Validates a positive recipe reference. */
		public DeleteRecipeAction {
			if (recipeId <= 0) {
				throw new IllegalArgumentException("Delete recipe action is invalid");
			}
		}
	}

	/** 将 recipe eligibility 拒绝原因投影到客户端协议。 / Projects a recipe-eligibility rejection to the client protocol. */
	public record NotifyRecipeRejectionAction(int recipeId) implements Action {
		/** 校验正数配方引用。 / Validates a positive recipe reference. */
		public NotifyRecipeRejectionAction {
			if (recipeId <= 0) {
				throw new IllegalArgumentException("Recipe rejection action is invalid");
			}
		}
	}

	/** 发放指定奖励组并完成当前任务周期。 / Grants the selected reward group and completes the current quest cycle. */
	public record FinishQuestAction(int rewardIndex, RepeatDeadlinePolicy repeatDeadlinePolicy) implements Action {
		/** 创建没有 repeat deadline 的完成动作。 / Creates a finish action without a repeat deadline. */
		public FinishQuestAction(int rewardIndex) {
			this(rewardIndex, NoRepeatDeadlinePolicy.INSTANCE);
		}

		/** 校验非负奖励组索引和显式 repeat policy。 / Validates the reward index and explicit repeat policy. */
		public FinishQuestAction {
			if (rewardIndex < 0 || repeatDeadlinePolicy == null) {
				throw new IllegalArgumentException("Quest reward index is invalid");
			}
		}
	}

	/** 通过 typed timer bridge 启动命名任务计时器。 / Starts a named quest timer through the typed timer bridge. */
	public record StartQuestTimerAction(String timer, long durationSeconds) implements Action {
		/** 校验命名计时器和正持续时间。 / Validates the named timer and positive duration. */
		public StartQuestTimerAction {
			if (!validTimerName(timer) || durationSeconds <= 0) {
				throw new IllegalArgumentException("Start quest timer action is invalid");
			}
		}
	}

	/** 通过 typed timer bridge 停止命名任务计时器。 / Stops a named quest timer through the typed timer bridge. */
	public record EndQuestTimerAction(String timer) implements Action {
		/** 校验命名计时器。 / Validates the named timer. */
		public EndQuestTimerAction {
			if (!validTimerName(timer)) {
				throw new IllegalArgumentException("End quest timer action is invalid");
			}
		}
	}

	/** 提交后发送绑定当前任务的对话页面。 / Sends a quest-bound dialog page after commit. */
	public record SendDialogAction(int dialogId) implements Action {
		/** 校验正数对话页面 ID。 / Validates a positive dialog-page id. */
		public SendDialogAction {
			if (dialogId <= 0) {
				throw new IllegalArgumentException("Quest dialog id is invalid");
			}
		}
	}

	/** 提交后关闭当前客户端对话窗口。 / Closes the current client dialog window after commit. */
	public record CloseDialogAction() implements Action {
	}

	/** 提交后刷新当前 NPC 的任务选择列表。 / Refreshes the current NPC quest list after commit. */
	public record ShowQuestListAction() implements Action {
	}

	/** 提交后向客户端同步 canonical 任务状态和变量。 / Syncs canonical quest status and variables after commit. */
	public record SyncQuestStatusAction() implements Action {
	}

	/** 提交后发送与已持久化 repeat deadline 一致的系统提示。 / Sends a system message matching the persisted repeat deadline after commit. */
	public record SendRepeatDeadlineMessageAction(RepeatDeadlinePolicy repeatDeadlinePolicy) implements Action {
		/** 提示必须引用一个真实 repeat policy。 / Requires a real repeat policy for the message. */
		public SendRepeatDeadlineMessageAction {
			if (repeatDeadlinePolicy == null || repeatDeadlinePolicy == NoRepeatDeadlinePolicy.INSTANCE) {
				throw new IllegalArgumentException("Repeat deadline message policy is invalid");
			}
		}
	}

	/** 提交后同步命名任务计时器的剩余秒数。 / Syncs the remaining seconds of a named quest timer after commit. */
	public record SyncQuestTimerAction(String timer, long remainingSeconds) implements Action {
		/** 校验命名计时器和非负剩余秒数。 / Validates the named timer and non-negative remaining seconds. */
		public SyncQuestTimerAction {
			if (!validTimerName(timer) || remainingSeconds < 0) {
				throw new IllegalArgumentException("Quest timer protocol action is invalid");
			}
		}
	}

	/** 校验任务计时器使用稳定 identifier 语法。 / Validates stable identifier syntax for quest timers. */
	private static boolean validTimerName(String timer) {
		return timer != null && timer.length() <= 128 && timer.matches("[A-Za-z][A-Za-z0-9_.-]*");
	}

	/** 校验 packed counter 使用非重复稳定变量名和受控基数。 / Validates unique stable variable names and a bounded radix for packed counters. */
	private static List<String> validatedPackedVariables(List<String> variables, int radix) {
		if (variables == null || variables.isEmpty() || radix < 2 || radix > 256
				|| variables.stream().anyMatch(value -> value == null || value.isBlank())
				|| Set.copyOf(variables).size() != variables.size()) {
			throw new IllegalArgumentException("Packed counter shape is invalid");
		}
		return List.copyOf(variables);
	}

	/** 提交后向玩家发送类型化频道消息。 / Sends a typed-channel player message after commit. */
	public record SendPlayerMessageAction(String text, PlayerMessageChannel channel) implements Action {
		/** 校验消息正文和频道。 / Validates message text and channel. */
		public SendPlayerMessageAction {
			if (text == null || text.isBlank() || channel == null) {
				throw new IllegalArgumentException("Player message action is invalid");
			}
		}
	}

	/** 提交后通过客户端影片协议播放引用闭合的影片。 / Plays a reference-closed movie through the client protocol after commit. */
	public record PlayMovieAction(int movieId) implements Action {
		/** 校验影片 ID 可无损写入协议的无符号 16 位字段。 / Validates that the movie id fits the protocol unsigned 16-bit field. */
		public PlayMovieAction {
			if (movieId <= 0 || movieId > 0xFFFF) {
				throw new IllegalArgumentException("Quest movie action id is invalid");
			}
		}
	}
}
