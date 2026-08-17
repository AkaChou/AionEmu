package com.aionemu.gameserver.questEngine.definition;

/**
 * 任务事务中所需变更的封闭集合。
 * Closed set of required mutations in the quest transaction.
 */
public sealed interface QuestAction permits QuestAction.RemoveItem, QuestAction.SetVariable,
		QuestAction.IncrementVariable, QuestAction.SetStatus, QuestAction.GrantReward,
		QuestAction.GrantSelectedReward,
		QuestAction.DecreaseCurrency,
		QuestAction.SetCurrency,
		QuestAction.LearnRecipe, QuestAction.ForgetRecipe, QuestAction.GrantCraftSkill,
		QuestAction.CompleteQuest, QuestAction.GiveItem, QuestAction.UnequipItem,
		QuestAction.PromoteArchDaeva, QuestAction.BlockDefaultItemUse, QuestAction.AbandonQuest {
	record RemoveItem(int itemId, int count) implements QuestAction {
		/**
		 * transition 需要移除当前完整堆叠时使用的哨兵数量。
		 * Sentinel count used when the transition must remove the complete live stack.
		 */
		public static final int ALL = -1;

		public RemoveItem {
			if (itemId <= 0 || (count <= 0 && count != ALL)) {
				throw new IllegalArgumentException("item id must be positive and count must be positive or ALL");
			}
		}

		public boolean removeAll() {
			return count == ALL;
		}
	}

	/** 发放任务工作物品（quest work item），接取/步骤推进时使用。 / Grants a quest work item. */
	record GiveItem(int itemId, int count) implements QuestAction {
		public GiveItem {
			if (itemId <= 0 || count <= 0) {
				throw new IllegalArgumentException("item id and count must be positive");
			}
		}
	}

	/**
	 * 在同一变更计划内的任何背包移除之前，先卸下所有已装备的该物品副本。
	 * 可选移除数量会从结果背包中最多消耗相应数量的副本，包括事件前已卸下的副本。
	 * Unequips every currently equipped copy of the requested item before any
	 * inventory removal in the same mutation plan. The optional removal count
	 * consumes up to that many copies from the resulting inventory, including a
	 * copy that was already unequipped before the event.
	 */
	record UnequipItem(int itemId, int removeReturnedCount) implements QuestAction {
		public UnequipItem(int itemId) {
			this(itemId, 0);
		}

		public UnequipItem {
			if (itemId <= 0 || removeReturnedCount < 0) {
				throw new IllegalArgumentException("item id must be positive and removeReturnedCount must be non-negative");
			}
		}
	}

	/** 阻止任务占有该物品时执行普通物品动作。 / Prevents the normal item action when the quest owns this item-use state. */
	record BlockDefaultItemUse() implements QuestAction {
	}

	/**
	 * 将任务变量字段设置为精确值。
	 * Sets a quest variable field to an exact value.
	 */
	record SetVariable(String field, int value) implements QuestAction {
		public SetVariable {
			if (field == null || field.isBlank()) {
				throw new IllegalArgumentException("field must not be blank");
			}
		}
	}

	/**
	 * 在现有值上做增量（delta 可正可负），用于多次收集/击杀/使用技能的计数推进。
	 * Increments an existing value (delta may be positive or negative) for multi-collect/kill/skill-use counting.
	 */
	record IncrementVariable(String field, int delta) implements QuestAction {
		public IncrementVariable {
			if (field == null || field.isBlank()) {
				throw new IllegalArgumentException("field must not be blank");
			}
			if (delta == 0) {
				throw new IllegalArgumentException("delta must be non-zero");
			}
		}
	}

	/**
	 * 将任务状态投影切换为指定状态。
	 * Switches the quest status projection to the given status.
	 */
	record SetStatus(com.aionemu.gameserver.questEngine.model.QuestStatus status)
			implements QuestAction {
		public SetStatus {
			if (status == null) {
				throw new NullPointerException("status");
			}
		}
	}

	record GrantReward(String kind, int id, long amount, QuestRewardAmountMode amountMode) implements QuestAction {
		public GrantReward(String kind, int id, long amount) {
			this(kind, id, amount, QuestRewardAmountMode.EXACT);
		}

		public GrantReward {
			QuestRewardKind.fromWire(kind);
			new QuestReward(kind, id, amount);
			if (amountMode == null) {
				throw new NullPointerException("amountMode");
			}
		}

		public QuestRewardKind rewardKind() {
			return QuestRewardKind.fromWire(kind);
		}
	}

	/**
	 * 发放任务元数据中 {@code rewardIndex} 处的权威奖励条目。规划器在调用任何
	 * 事务端口之前，会将该声明式动作降级为具体的 GrantReward，因此 XML 无法重复官方奖励数据。
	 * Grants the authoritative reward entry at {@code rewardIndex} in quest metadata.
	 * The planner lowers this declarative action to a concrete GrantReward before
	 * any transactional port is called, so XML cannot duplicate retail reward data.
	 */
	record GrantSelectedReward(int rewardIndex) implements QuestAction {
		public GrantSelectedReward {
			if (rewardIndex < 0) {
				throw new IllegalArgumentException("rewardIndex must be non-negative");
			}
		}
	}

	/**
	 * 以事务方式移除正数量的受支持任务货币。
	 * Atomically removes a positive amount of a supported quest currency.
	 */
	record DecreaseCurrency(QuestRewardKind kind, long amount) implements QuestAction {
		public DecreaseCurrency {
			if (kind == null || !kind.isCurrency()) {
				throw new IllegalArgumentException("currency kind must be a supported currency");
			}
			if (amount <= 0) {
				throw new IllegalArgumentException("currency decrease amount must be positive");
			}
		}
	}

	/**
	 * 以事务方式将受支持货币设置为精确的非负余额。
	 * Sets a supported currency to an exact non-negative balance transactionally.
	 */
	record SetCurrency(QuestRewardKind kind, long amount) implements QuestAction {
		public SetCurrency {
			if (kind == null || !kind.isCurrency()) {
				throw new IllegalArgumentException("currency kind must be a supported currency");
			}
			if (amount < 0) {
				throw new IllegalArgumentException("currency balance must be non-negative");
			}
		}
	}

	/**
	 * 完成任务并在状态事务中冻结完成次数、奖励索引与时间戳。
	 * Completes the quest and freezes completion count, reward index, and timestamps in the state transaction.
	 */
	record CompleteQuest(int rewardIndex) implements QuestAction {
		public CompleteQuest {
			if (rewardIndex < 0) {
				throw new IllegalArgumentException("rewardIndex must be non-negative");
			}
		}
	}

	/**
	 * 在任务完成事务中持久化高阶守护者晋升，并在提交后将在线角色直接提升到 66 级。
	 * Persists ArchDaeva promotion in the quest-completion transaction and advances the live player to level 66
	 * after commit.
	 */
	record PromoteArchDaeva() implements QuestAction {
	}

	/**
	 * 在与任务状态相同的事务中学会配方。
	 * Learns a recipe in the same transaction as the quest state.
	 */
	record LearnRecipe(int recipeId, QuestRecipeOwnership ownership) implements QuestAction {
		public LearnRecipe {
			if (recipeId <= 0) {
				throw new IllegalArgumentException("recipeId must be positive");
			}
			if (ownership == null) {
				throw new NullPointerException("ownership");
			}
		}
	}

	/**
	 * 显式释放配方；任务拥有的配方不会在登出或关服时被隐式移除。
	 * Explicitly releases a recipe; quest-owned recipes are never removed implicitly on logout or shutdown.
	 */
	record ForgetRecipe(int recipeId) implements QuestAction {
		public ForgetRecipe {
			if (recipeId <= 0) {
				throw new IllegalArgumentException("recipeId must be positive");
			}
		}
	}

	/**
	 * 授予制作技能等级，并可选地学会该等级以下全部权威自动学习配方。
	 * Grants a crafting skill level and optionally learns all authoritative auto-learn recipes up to that level.
	 */
	record GrantCraftSkill(int skillId, int targetLevel, boolean autoLearnRecipes) implements QuestAction {
		public GrantCraftSkill {
			if (skillId <= 0 || targetLevel <= 0) {
				throw new IllegalArgumentException("skillId and targetLevel must be positive");
			}
		}
	}

	/**
	 * 服务端强制放弃任务,执行 {@code QuestService.abandonQuest} 的完整清理语义
	 * (状态删除 + 任务物品清理)。区别于 {@code abandon} 事件 (玩家主动放弃) 与
	 * {@code set-status NONE} (仅投影切换)。要求目标节点为 NONE 投影。
	 * Server-forced quest abandon with full lifecycle cleanup; distinct from the
	 * player-initiated {@code abandon} event and from a bare NONE projection.
	 */
	record AbandonQuest() implements QuestAction {
	}
}
