package com.aionemu.gameserver.questEngine.graph;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.aionemu.gameserver.model.EmotionId;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.items.ItemId;
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
		PAY_KINAH_AND_ITEM(ActionPhase.REQUIRED),
		DIALOG_NPC_LIFECYCLE(ActionPhase.REQUIRED),
		REMOVE_COLLECTED_ITEMS(ActionPhase.REQUIRED),
		REMOVE_QUEST_WORK_ITEMS(ActionPhase.REQUIRED),
		LEARN_RECIPE(ActionPhase.REQUIRED),
		DELETE_RECIPE(ActionPhase.REQUIRED),
		GRANT_CRAFT_SKILL_REWARD(ActionPhase.REQUIRED),
		FINISH_QUEST(ActionPhase.REQUIRED),
		SPAWN_INSTANCE_NPC(ActionPhase.REQUIRED),
		START_ESCORT(ActionPhase.REQUIRED),
		TELEPORT_PLAYER(ActionPhase.REQUIRED),
		DELAY_ITEM_USE_CONTINUATION(ActionPhase.REQUIRED),
		REMOVE_USED_ITEM(ActionPhase.REQUIRED),
		START_QUEST_TIMER(ActionPhase.REQUIRED),
		END_QUEST_TIMER(ActionPhase.REQUIRED),
		SEND_DIALOG(ActionPhase.POST_COMMIT_PROTOCOL),
		CLOSE_DIALOG(ActionPhase.POST_COMMIT_PROTOCOL),
		SHOW_QUEST_LIST(ActionPhase.POST_COMMIT_PROTOCOL),
		SYNC_QUEST_STATUS(ActionPhase.POST_COMMIT_PROTOCOL),
		SEND_REPEAT_DEADLINE_MESSAGE(ActionPhase.POST_COMMIT_PROTOCOL),
		SYNC_QUEST_TIMER(ActionPhase.POST_COMMIT_PROTOCOL),
		SEND_PLAYER_MESSAGE(ActionPhase.POST_COMMIT_PROTOCOL),
		SEND_SYSTEM_MESSAGE(ActionPhase.POST_COMMIT_PROTOCOL),
		SEND_EMOTION(ActionPhase.POST_COMMIT_PROTOCOL),
		START_FLIGHT_TELEPORT(ActionPhase.POST_COMMIT_PROTOCOL),
		PLAY_MOVIE(ActionPhase.POST_COMMIT_PROTOCOL),
		SCHEDULE_ITEM_USE_DIALOG(ActionPhase.POST_COMMIT_PROTOCOL),
		SYNC_CRAFT_SKILL_REWARD(ActionPhase.POST_COMMIT_PROTOCOL),
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

	/** 封闭旧 Handler 使用的三种系统消息协议。 / Closes the three system-message protocols used by legacy handlers. */
	public enum SystemMessageKind {
		INSTANCE_DUNGEON_NEED_SOLO(1403080),
		WAREHOUSE_FULL_INVENTORY(1390149),
		COMMON_SAY_08(1111307);

		private final int code;

		SystemMessageKind(int code) {
			this.code = code;
		}

		public int code() {
			return code;
		}
	}

	/** 定义表情动作的服务端权威发起者。 / Defines the server-authoritative emote actor. */
	public enum EmotionTarget {
		PLAYER,
		DIALOG_NPC
	}

	/**
	 * 对话协议是否绑定当前任务 ID（UNBOUND 对应真端 quest_id=0）。
	 * Whether the dialog protocol binds the current quest id (UNBOUND matches retail quest_id=0).
	 */
	public enum DialogBindingMode {
		BOUND,
		UNBOUND
	}

	/** 区分具有 NPC 身份快照的对话与显式无目标任务对话。 / Distinguishes NPC-bound dialogs from explicit targetless quest dialogs. */
	public enum DialogTargetKind {
		NPC,
		NO_TARGET
	}

	/** 定义传送目标 instance 的服务端解析策略。 / Defines how the server resolves the destination instance. */
	public enum TeleportInstancePolicy {
		/** 使用显式 instanceId；0 交由普通传送服务选择当前/默认 instance。 / Uses the explicit id; 0 delegates to normal current/default routing. */
		EXPLICIT_OR_DEFAULT,
		/** 在 PREPARED 前冻结玩家当前 instance，并在恢复时复用该快照。 / Freezes the player's current instance before PREPARED and reuses it during recovery. */
		PLAYER_CURRENT,
		/** 复用玩家已注册副本；不存在时创建并注册。 / Reuses the player's registered instance, creating and registering one when absent. */
		PLAYER_REGISTERED_OR_CREATE
	}

	/** 定义传送朝向的服务端解析策略。 / Defines how the server resolves the destination heading. */
	public enum TeleportHeadingPolicy {
		/** 使用图中显式朝向；旧 XML 缺省为 0。 / Uses the explicit graph heading; legacy XML defaults to zero. */
		EXPLICIT,
		/** 在 PREPARED 前冻结玩家当前朝向，并在恢复时复用该快照。 / Freezes the player's current heading before PREPARED and reuses it during recovery. */
		PLAYER_CURRENT
	}

	/** 定义 escort follower 的服务端权威来源。 / Defines the server-authoritative source of an escort follower. */
	public enum EscortSource {
		/** 在玩家当前位置生成新的 follower。 / Spawns a new follower at the player's current position. */
		PLAYER_POSITION_SPAWN,
		/** 复用触发当前事件的 NPC。 / Reuses the NPC that triggered the current event. */
		EVENT_NPC,
		/** 生成 follower，并在全部准备完成后替换当前事件 NPC。 / Spawns the follower and replaces the event NPC after all preparation succeeds. */
		REPLACE_EVENT_NPC_AT_PLAYER_POSITION
	}

	/** 定义 escort 到达判定的封闭目标种类。 / Defines the closed destination kinds used by escort arrival checks. */
	public enum EscortDestinationKind {
		ZONE,
		NPC,
		COORDINATES
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
	public record Event(EventType type, int targetId, String qualifier, DialogTargetKind dialogTargetKind) {
		/** 为现有非零 NPC 对话与非对话事件保留紧凑构造。 / Preserves the compact constructor for existing positive-NPC and non-dialog events. */
		public Event(EventType type, int targetId, String qualifier) {
			this(type, targetId, qualifier, type == EventType.DIALOG ? DialogTargetKind.NPC : null);
		}

		/** 校验只有 DIALOG 携带目标种类，并关闭 NPC/NO_TARGET 与 targetId 的组合。 / Closes target-kind combinations for DIALOG only. */
		public Event {
			Objects.requireNonNull(type, "event type");
			if (type == EventType.DIALOG) {
				if (dialogTargetKind == null || dialogTargetKind == DialogTargetKind.NPC && targetId <= 0
						|| dialogTargetKind == DialogTargetKind.NO_TARGET && targetId != 0) {
					throw new IllegalArgumentException("Dialog event target is invalid");
				}
			} else if (dialogTargetKind != null) {
				throw new IllegalArgumentException("Non-dialog event cannot declare a dialog target kind");
			}
		}
	}

	/**
	 * 表示转换执行前必须满足的已类型化条件。
	 * Represents a typed condition that must hold before a transition executes.
	 */
	public sealed interface Condition permits QuestStatusCondition, QuestVariableCondition, QuestRepeatAvailableCondition,
		PackedCounterCondition, InvasionWorldActiveCondition,
		QuestCollectItemsCondition, RecipeLearnableCondition, CraftSkillEligibilityCondition, PlayerLevelCondition,
		KillVictimLevelDeltaCondition, PlayerRaceCondition, PlayerClassCondition,
		PlayerGenderCondition, PlayerTitleCondition, PlayerAbyssRankCondition, PlayerInventoryCondition, QuestRewardCondition,
		QuestCompletionCountCondition, PlayerEquippedCondition, PlayerRewardInventoryCapacityCondition,
		PlayerActiveHouseButlerCondition, PlayerGroupMembershipCondition {
	}

	/**
	 * 定义奖励结算前检查的库存范围；当前只证明 SPECIAL_CUBE。
	 * Defines the inventory scope checked before reward settlement; only SPECIAL_CUBE is proven today.
	 */
	public enum RewardInventoryScope {
		/** 特殊背包/任务奖励格 / Special cube used by fountain and similar reward flows */
		SPECIAL_CUBE
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

	/** 定义制作技能奖励在对话与影片提交阶段使用的两种精确资格策略。 / Defines the two exact eligibility policies used by craft rewards during dialog and movie settlement. */
	public enum CraftSkillEligibilityPolicy {
		CAPACITY_IF_EXISTING_NOT_TARGET,
		CAPACITY_REQUIRED
	}

	/** 通过制作服务的 typed snapshot 判断专家或大师奖励资格。 / Evaluates expert or master reward eligibility through a typed crafting-service snapshot. */
	public record CraftSkillEligibilityCondition(int craftSkillId, int targetLevel, CraftSkillEligibilityPolicy policy)
		implements Condition {
		/** 校验制作技能引用、当前可达奖励等级和封闭策略。 / Validates the craft-skill reference, currently reachable reward level, and closed policy. */
		public CraftSkillEligibilityCondition {
			if (craftSkillId <= 0 || targetLevel != 400 && targetLevel != 500 || policy == null) {
				throw new IllegalArgumentException("Craft skill eligibility condition is invalid");
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
	 * 比较奖励结算前特殊背包是否仍有空位；只读、无副作用。
	 * Compares whether the special cube still has free slots before reward settlement; read-only and side-effect free.
	 */
	public record PlayerRewardInventoryCapacityCondition(RewardInventoryScope scope, boolean expected) implements Condition {
		/** 校验封闭库存范围。 / Validates the closed inventory scope. */
		public PlayerRewardInventoryCapacityCondition {
			if (scope == null) {
				throw new IllegalArgumentException("Reward inventory capacity scope is invalid");
			}
		}
	}

	/** 要求对话目标就是玩家当前住宅的管家；目标身份来自服务端 DIALOG 事件。 / Requires the dialog target to be the player's active-house butler. */
	public record PlayerActiveHouseButlerCondition() implements Condition {
	}

	/** 精确比较玩家是否属于小队；语义对应 Player.isInGroup2()。 / Compares exact party membership via Player.isInGroup2(). */
	public record PlayerGroupMembershipCondition(boolean expected) implements Condition {
	}

	/**
	 * 定义转换动作的强类型封闭集合。
	 * Defines the closed typed set of transition actions.
	 */
	public sealed interface Action permits StartQuestAction, StartEventQuestAction, AbandonQuestAction, SetQuestStatusAction, SetQuestVariableAction,
		AddQuestVariableAction, IncrementPackedCounterAction,
		SetCompletionCountAction, AddCompletionCountAction, GiveQuestItemAction, RemoveQuestItemAction, PayKinahAndItemAction, DialogNpcLifecycleAction, RemoveCollectedItemsAction,
		RemoveQuestWorkItemsAction, LearnRecipeAction, DeleteRecipeAction, GrantCraftSkillRewardAction,
		SyncCraftSkillRewardAction, NotifyRecipeRejectionAction, FinishQuestAction,
		StartQuestTimerAction, EndQuestTimerAction, SendDialogAction, CloseDialogAction, ShowQuestListAction, SyncQuestStatusAction,
		SendRepeatDeadlineMessageAction, SyncQuestTimerAction, SendPlayerMessageAction, SendEmotionAction, PlayMovieAction,
		SendSystemMessageAction, StartFlightTeleportAction, ScheduleItemUseDialogAction, DelayItemUseContinuationAction,
		RemoveUsedItemAction, SpawnInstanceNpcAction,
		StartEscortAction, TeleportPlayerAction {

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
				case PayKinahAndItemAction ignored -> ActionType.PAY_KINAH_AND_ITEM;
				case DialogNpcLifecycleAction ignored -> ActionType.DIALOG_NPC_LIFECYCLE;
				case RemoveCollectedItemsAction ignored -> ActionType.REMOVE_COLLECTED_ITEMS;
				case RemoveQuestWorkItemsAction ignored -> ActionType.REMOVE_QUEST_WORK_ITEMS;
				case LearnRecipeAction ignored -> ActionType.LEARN_RECIPE;
				case DeleteRecipeAction ignored -> ActionType.DELETE_RECIPE;
				case GrantCraftSkillRewardAction ignored -> ActionType.GRANT_CRAFT_SKILL_REWARD;
				case SyncCraftSkillRewardAction ignored -> ActionType.SYNC_CRAFT_SKILL_REWARD;
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
				case SendSystemMessageAction ignored -> ActionType.SEND_SYSTEM_MESSAGE;
				case SendEmotionAction ignored -> ActionType.SEND_EMOTION;
				case StartFlightTeleportAction ignored -> ActionType.START_FLIGHT_TELEPORT;
				case PlayMovieAction ignored -> ActionType.PLAY_MOVIE;
				case ScheduleItemUseDialogAction ignored -> ActionType.SCHEDULE_ITEM_USE_DIALOG;
				case SpawnInstanceNpcAction ignored -> ActionType.SPAWN_INSTANCE_NPC;
				case StartEscortAction ignored -> ActionType.START_ESCORT;
				case TeleportPlayerAction ignored -> ActionType.TELEPORT_PLAYER;
				case DelayItemUseContinuationAction ignored -> ActionType.DELAY_ITEM_USE_CONTINUATION;
				case RemoveUsedItemAction ignored -> ActionType.REMOVE_USED_ITEM;
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
		OPTIONAL_EXACT,
		ALL
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

	/** 原子扣除显式 Kinah 与普通背包物品；任一余额不足时不产生副作用。 / Atomically charges explicit Kinah and ordinary inventory items, with no side effect when either balance is insufficient. */
	public record PayKinahAndItemAction(long kinah, int itemId, long itemCount) implements Action {
		/** 校验正数货币、物品引用和数量。 / Validates positive currency, item reference, and count. */
		public PayKinahAndItemAction {
			if (kinah <= 0 || itemId <= 0 || itemId == ItemId.KINAH.value() || itemCount <= 0) {
				throw new IllegalArgumentException("Kinah and item payment action is invalid");
			}
		}
	}

	/** 对话目标 NPC 的类型化生命周期动作；动作只接受当前 DIALOG 快照中的同一对象。 / Typed lifecycle action for the dialog-target NPC; only the exact object in the current DIALOG snapshot is accepted. */
	public record DialogNpcLifecycleAction(DialogNpcLifecycleMode mode) implements Action {
		public DialogNpcLifecycleAction {
			if (mode == null) {
				throw new IllegalArgumentException("Dialog NPC lifecycle mode is invalid");
			}
		}
	}

	/** 对话目标 NPC 生命周期的封闭模式。 / Closed modes for dialog-target NPC lifecycle. */
	public enum DialogNpcLifecycleMode {
		DELETE,
		SCHEDULE_RESPAWN_THEN_DELETE
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

	/** 通过持久化 typed bridge 收敛制作技能等级和该等级应自动学习的配方。 / Converges the craft-skill level and auto-learn recipes through a durable typed bridge. */
	public record GrantCraftSkillRewardAction(int craftSkillId, int targetLevel) implements Action {
		/** 校验制作技能引用及当前生产 Handler 使用的专家/大师等级。 / Validates the craft-skill reference and expert/master levels used by current production handlers. */
		public GrantCraftSkillRewardAction {
			if (craftSkillId <= 0 || targetLevel != 400 && targetLevel != 500) {
				throw new IllegalArgumentException("Grant craft skill reward action is invalid");
			}
		}
	}

	/** 提交后同步固定的制作晋升技能列表协议。 / Projects the fixed craft-promotion skill-list protocol after commit. */
	public record SyncCraftSkillRewardAction(int craftSkillId) implements Action {
		/** 校验制作技能引用。 / Validates the craft-skill reference. */
		public SyncCraftSkillRewardAction {
			if (craftSkillId <= 0) {
				throw new IllegalArgumentException("Craft skill reward protocol action is invalid");
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

	/**
	 * 提交后发送对话页面；BOUND 附带 questId，UNBOUND 不绑定任务。
	 * Sends a dialog page after commit; BOUND attaches questId, UNBOUND omits quest binding.
	 */
	public record SendDialogAction(int dialogId, DialogBindingMode binding) implements Action {
		/** 默认 BOUND 的便捷构造。 / Convenience constructor defaulting to BOUND. */
		public SendDialogAction(int dialogId) {
			this(dialogId, DialogBindingMode.BOUND);
		}

		/** 校验正数对话页与绑定模式。 / Validates a positive dialog page and binding mode. */
		public SendDialogAction {
			if (dialogId <= 0 || binding == null) {
				throw new IllegalArgumentException("Quest dialog action is invalid");
			}
		}
	}

	/** 提交后关闭当前客户端对话窗口。 / Closes the current client dialog window after commit. */
	public record CloseDialogAction() implements Action {
	}

	/** 提交后刷新当前 NPC 的任务选择列表。 / Refreshes the current NPC quest list after commit. */
	public record ShowQuestListAction() implements Action {
	}

	/**
	 * 提交后向客户端同步在指定 pre-protocol 动作前缀后冻结的任务状态和变量；-1 表示最终已提交快照。
	 * Syncs quest status and variables frozen after a pre-protocol action prefix; -1 selects the final committed snapshot.
	 */
	public record SyncQuestStatusAction(int snapshotAfterActionCount) implements Action {
		/** 保留旧空动作语义，使用最终已提交快照。 / Preserves the legacy empty action by selecting the final committed snapshot. */
		public SyncQuestStatusAction() {
			this(-1);
		}

		/** 拒绝除最终快照 sentinel 外的负 checkpoint。 / Rejects negative checkpoints other than the final-snapshot sentinel. */
		public SyncQuestStatusAction {
			if (snapshotAfterActionCount < -1) {
				throw new IllegalArgumentException("Quest-status sync checkpoint is invalid");
			}
		}
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

	/** 提交后发送一种封闭系统消息。 / Sends one closed system-message kind after commit. */
	public record SendSystemMessageAction(SystemMessageKind kind) implements Action {
		public SendSystemMessageAction {
			if (kind == null) {
				throw new IllegalArgumentException("System-message action kind is invalid");
			}
		}
	}

	/** 提交后启动引用闭合的客户端飞行路径；协议 ID 由 pathId 唯一推导。 / Starts a reference-closed flight path after commit. */
	public record StartFlightTeleportAction(int pathId) implements Action {
		public StartFlightTeleportAction {
			if (pathId <= 0 || pathId > (Integer.MAX_VALUE - 1) / 1000) {
				throw new IllegalArgumentException("Flight-teleport path is invalid");
			}
		}

		/** 返回客户端使用的严格 path*1000+1 协议 ID。 / Returns the strict path*1000+1 client protocol id. */
		public int protocolId() {
			return pathId * 1000 + 1;
		}
	}

	/** 提交后由玩家或当前对话 NPC 播放类型化表情。 / Plays a typed emote by the player or current dialog NPC after commit. */
	public record SendEmotionAction(EmotionTarget target, EmotionId emotion, boolean broadcast) implements Action {
		/** 校验封闭目标与有效表情。 / Validates the closed target and a meaningful emote. */
		public SendEmotionAction {
			if (target == null || emotion == null || emotion == EmotionId.NONE) {
				throw new IllegalArgumentException("Emotion action is invalid");
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

	/**
	 * 提交后播放物品使用动画，并在固定延迟后打开对话页。
	 * Plays the item-use animation after commit and opens a dialog page after a fixed delay.
	 */
	public record ScheduleItemUseDialogAction(int durationMs, int dialogId) implements Action {
		/** 校验延迟与对话页均为正。 / Validates that delay and dialog page are positive. */
		public ScheduleItemUseDialogAction {
			if (durationMs <= 0 || dialogId <= 0) {
				throw new IllegalArgumentException("Item-use dialog schedule is invalid");
			}
		}
	}

	/**
	 * 在 ITEM_USE 事件上建立可恢复的绝对时间屏障；屏障后的动作仍由 graph journal 顺序执行。
	 * Establishes a recoverable absolute-time barrier for an ITEM_USE event; the graph journal still executes the tail in order.
	 */
	public record DelayItemUseContinuationAction(int durationMs) implements Action {
		/** 校验动画与延迟时长。 / Validates the animation and delay duration. */
		public DelayItemUseContinuationAction {
			if (durationMs <= 0) {
				throw new IllegalArgumentException("Item-use continuation duration is invalid");
			}
		}
	}

	/** 冻结 ITEM_USE 事件物品的扣除身份。 / Freezes how the ITEM_USE event item is identified for removal. */
	public enum UsedItemRemovalMode {
		/** 只扣除事件携带的具体物品对象。 / Removes only the exact item object carried by the event. */
		EVENT_OBJECT_EXACT,
		/** 按事件携带的物品模板从背包总量扣除。 / Removes by the item template carried by the event. */
		EVENT_TEMPLATE_EXACT
	}

	/** 延迟屏障后扣除 ITEM_USE 事件冻结的物品。 / Removes the item frozen by the ITEM_USE event after the delay barrier. */
	public record RemoveUsedItemAction(long count, UsedItemRemovalMode mode) implements Action {
		/** 校验正数数量与封闭身份模式。 / Validates a positive count and closed identity mode. */
		public RemoveUsedItemAction {
			if (count <= 0 || mode == null) {
				throw new IllegalArgumentException("Used-item removal action is invalid");
			}
		}
	}

	/** 定义 instance NPC 生成位置的封闭集合。 / Defines the closed set of instance-NPC spawn placements. */
	public sealed interface SpawnPlacement permits StaticSpawnerPlacement, EventNpcPlacement, PlayerPlacement, FixedPlacement {
		/** 返回位置策略种类。 / Returns the placement kind. */
		SpawnPlacementKind kind();
	}

	/** Instance NPC 生成位置种类。 / Instance-NPC spawn placement kinds. */
	public enum SpawnPlacementKind {
		STATIC_SPAWNER,
		EVENT_NPC,
		PLAYER,
		FIXED
	}

	/** 定义 FIXED 位置的世界解析策略。 / Defines how a FIXED placement resolves its world. */
	public enum SpawnWorldPolicy {
		EXPLICIT,
		PLAYER_CURRENT
	}

	/** 定义 FIXED 位置的 instance 解析策略。 / Defines how a FIXED placement resolves its instance. */
	public enum SpawnInstancePolicy {
		EXPLICIT,
		PLAYER_CURRENT
	}

	/** 使用静态刷新目录中一个 NPC 模板的权威坐标。 / Uses authoritative static-spawn coordinates for an NPC template. */
	public record StaticSpawnerPlacement(int spawnerObjectId) implements SpawnPlacement {
		public StaticSpawnerPlacement {
			if (spawnerObjectId <= 0) {
				throw new IllegalArgumentException("Static-spawner placement is invalid");
			}
		}

		@Override
		public SpawnPlacementKind kind() {
			return SpawnPlacementKind.STATIC_SPAWNER;
		}
	}

	/** 使用当前 DIALOG 事件 NPC 的权威实时坐标。 / Uses the authoritative live coordinates of the current DIALOG event NPC. */
	public record EventNpcPlacement(int eventNpcId) implements SpawnPlacement {
		public EventNpcPlacement {
			if (eventNpcId <= 0) {
				throw new IllegalArgumentException("Event-NPC placement is invalid");
			}
		}

		@Override
		public SpawnPlacementKind kind() {
			return SpawnPlacementKind.EVENT_NPC;
		}
	}

	/** 使用玩家的权威实时坐标。 / Uses the player's authoritative live coordinates. */
	public record PlayerPlacement() implements SpawnPlacement {
		@Override
		public SpawnPlacementKind kind() {
			return SpawnPlacementKind.PLAYER;
		}
	}

	/** 使用显式或玩家当前 world/instance，以及显式坐标和朝向。 / Uses explicit or player-current context with fixed coordinates. */
	public record FixedPlacement(SpawnWorldPolicy worldPolicy, int worldId, SpawnInstancePolicy instancePolicy, int instanceId,
			float x, float y, float z, byte heading) implements SpawnPlacement {
		/** 兼容全部显式 context 的调用。 / Preserves callers that provide a fully explicit context. */
		public FixedPlacement(int worldId, int instanceId, float x, float y, float z, byte heading) {
			this(SpawnWorldPolicy.EXPLICIT, worldId, SpawnInstancePolicy.EXPLICIT, instanceId, x, y, z, heading);
		}

		public FixedPlacement {
			if (worldPolicy == null || instancePolicy == null
					|| (worldPolicy == SpawnWorldPolicy.EXPLICIT ? worldId <= 0 : worldId != 0)
					|| (instancePolicy == SpawnInstancePolicy.EXPLICIT ? instanceId < 0 : instanceId != 0)
					|| !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
				throw new IllegalArgumentException("Fixed spawn placement is invalid");
			}
		}

		@Override
		public SpawnPlacementKind kind() {
			return SpawnPlacementKind.FIXED;
		}
	}

	/** 以类型化位置生成 NPC，并登记 instance-scoped cleanup lease。 / Spawns an NPC at a typed placement and registers an instance-scoped cleanup lease. */
	public record SpawnInstanceNpcAction(int npcId, SpawnPlacement placement) implements Action {
		/** 兼容旧静态 spawner 构造；位置仍被显式类型化。 / Preserves the legacy static-spawner constructor while keeping placement typed. */
		public SpawnInstanceNpcAction(int spawnerObjectId, int npcId) {
			this(npcId, new StaticSpawnerPlacement(spawnerObjectId));
		}

		/** 校验 NPC 模板与位置策略。 / Validates the NPC template and placement policy. */
		public SpawnInstanceNpcAction {
			if (npcId <= 0 || placement == null) {
				throw new IllegalArgumentException("Instance spawn action is invalid");
			}
		}

		/** 兼容旧适配器读取静态来源；非静态位置没有 spawner ID。 / Compatibility accessor for the legacy static adapter. */
		public int spawnerObjectId() {
			return placement instanceof StaticSpawnerPlacement source ? source.spawnerObjectId() : 0;
		}
	}

	/** 定义 escort 到达判定的强类型封闭参数。 / Defines the closed typed parameters for an escort destination. */
	public sealed interface EscortDestination permits EscortZoneDestination, EscortNpcDestination, EscortCoordinatesDestination {
		/** 返回目的地种类。 / Returns the destination kind. */
		EscortDestinationKind kind();
	}

	/** 以静态数据中的命名区域作为 escort 目的地。 / Uses a static-data zone as the escort destination. */
	public record EscortZoneDestination(String zoneName) implements EscortDestination {
		/** 校验规范区域名。 / Validates the canonical zone name. */
		public EscortZoneDestination {
			if (zoneName == null || zoneName.isBlank()) {
				throw new IllegalArgumentException("Escort zone destination is invalid");
			}
		}

		@Override
		public EscortDestinationKind kind() {
			return EscortDestinationKind.ZONE;
		}
	}

	/** 以指定 NPC 模板的服务端刷新坐标作为 escort 目的地。 / Uses the authoritative spawn location of an NPC template as the escort destination. */
	public record EscortNpcDestination(int npcId) implements EscortDestination {
		/** 校验目标 NPC 模板。 / Validates the destination NPC template. */
		public EscortNpcDestination {
			if (npcId <= 0) {
				throw new IllegalArgumentException("Escort NPC destination is invalid");
			}
		}

		@Override
		public EscortDestinationKind kind() {
			return EscortDestinationKind.NPC;
		}
	}

	/** 以显式服务端坐标作为 escort 目的地。 / Uses explicit server coordinates as the escort destination. */
	public record EscortCoordinatesDestination(float x, float y, float z) implements EscortDestination {
		/** 校验有限坐标。 / Validates finite coordinates. */
		public EscortCoordinatesDestination {
			if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
				throw new IllegalArgumentException("Escort coordinates destination is invalid");
			}
		}

		@Override
		public EscortDestinationKind kind() {
			return EscortDestinationKind.COORDINATES;
		}
	}

	/**
	 * 原子启动一个 player/quest-scoped escort，并登记可清理 lease。
	 * Atomically starts a player/quest-scoped escort and registers a cleanup lease.
	 */
	public record StartEscortAction(EscortSource source, int npcId, byte heading, String walkerId, boolean startWalking,
			boolean followMe, boolean startEmote2, boolean sendNpcInfo, EscortDestination destination) implements Action {
		/** 校验来源、生成模板、可选 walker 与跟随目标组合。 / Validates source, spawn template, optional walker, and follow destination. */
		public StartEscortAction {
			if (source == null || source == EscortSource.EVENT_NPC && npcId != 0
					|| source != EscortSource.EVENT_NPC && npcId <= 0
					|| walkerId != null && walkerId.isBlank() || destination == null) {
				throw new IllegalArgumentException("Start escort action is invalid");
			}
			walkerId = walkerId == null ? null : walkerId.trim();
		}
	}

	/**
	 * 将玩家传送到服务端权威世界坐标（可选 instance）。
	 * Teleports the player to server-authoritative world coordinates (optional instance).
	 */
	public record TeleportPlayerAction(int worldId, int instanceId, TeleportInstancePolicy instancePolicy, float x, float y,
			float z, TeleportHeadingPolicy headingPolicy, byte heading) implements Action {
		/** 保持旧 XML/调用方的兼容构造，缺省使用普通显式/默认策略。 / Preserves existing callers and XML with the normal explicit/default policy. */
		public TeleportPlayerAction(int worldId, int instanceId, float x, float y, float z, byte heading) {
			this(worldId, instanceId, TeleportInstancePolicy.EXPLICIT_OR_DEFAULT, x, y, z, TeleportHeadingPolicy.EXPLICIT, heading);
		}

		/** 保持已有 instance 策略调用方兼容，并使用显式朝向。 / Preserves existing instance-policy callers with an explicit heading. */
		public TeleportPlayerAction(int worldId, int instanceId, TeleportInstancePolicy instancePolicy, float x, float y, float z, byte heading) {
			this(worldId, instanceId, instancePolicy, x, y, z, TeleportHeadingPolicy.EXPLICIT, heading);
		}

		/** 校验世界、封闭策略、instance 组合与有限坐标。 / Validates the world, closed policy/id combination, and finite coordinates. */
		public TeleportPlayerAction {
			if (worldId <= 0 || instanceId < 0 || instancePolicy == null || headingPolicy == null
					|| instancePolicy != TeleportInstancePolicy.EXPLICIT_OR_DEFAULT && instanceId != 0
					|| headingPolicy == TeleportHeadingPolicy.PLAYER_CURRENT && heading != 0
					|| !Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
				throw new IllegalArgumentException("Teleport player action is invalid");
			}
		}

		/** 是否必须在 PREPARED 前冻结玩家上下文。 / Returns whether player context must be frozen before PREPARED. */
		public boolean requiresCurrentContext() {
			return instancePolicy == TeleportInstancePolicy.PLAYER_CURRENT
				|| instancePolicy == TeleportInstancePolicy.EXPLICIT_OR_DEFAULT && instanceId == 0
				|| headingPolicy == TeleportHeadingPolicy.PLAYER_CURRENT;
		}
	}
}
