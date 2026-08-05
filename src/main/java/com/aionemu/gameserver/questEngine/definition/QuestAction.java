package com.aionemu.gameserver.questEngine.definition;

/** Closed set of required mutations in the quest transaction. */
public sealed interface QuestAction permits QuestAction.RemoveItem, QuestAction.SetVariable,
		QuestAction.IncrementVariable, QuestAction.SetStatus, QuestAction.GrantReward,
		QuestAction.LearnRecipe, QuestAction.ForgetRecipe, QuestAction.GrantCraftSkill,
		QuestAction.CompleteQuest, QuestAction.GiveItem, QuestAction.BlockDefaultItemUse {
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

	/** 阻止任务占有该物品时执行普通物品动作。 / Prevents the normal item action when the quest owns this item-use state. */
	record BlockDefaultItemUse() implements QuestAction {
	}

	record SetVariable(String field, int value) implements QuestAction {
		public SetVariable {
			if (field == null || field.isBlank()) {
				throw new IllegalArgumentException("field must not be blank");
			}
		}
	}

	/** 在现有值上做增量（delta 可正可负），用于多次收集/击杀/使用技能的计数推进。 */
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

	/** Completes the quest and freezes completion count, reward index, and timestamps in the state transaction. */
	record CompleteQuest(int rewardIndex) implements QuestAction {
		public CompleteQuest {
			if (rewardIndex < 0) {
				throw new IllegalArgumentException("rewardIndex must be non-negative");
			}
		}
	}

	/** Learns a recipe in the same transaction as the quest state. */
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

	/** Explicitly releases a recipe; quest-owned recipes are never removed implicitly on logout or shutdown. */
	record ForgetRecipe(int recipeId) implements QuestAction {
		public ForgetRecipe {
			if (recipeId <= 0) {
				throw new IllegalArgumentException("recipeId must be positive");
			}
		}
	}

	/** Grants a crafting skill level and optionally learns all authoritative auto-learn recipes up to that level. */
	record GrantCraftSkill(int skillId, int targetLevel, boolean autoLearnRecipes) implements QuestAction {
		public GrantCraftSkill {
			if (skillId <= 0 || targetLevel <= 0) {
				throw new IllegalArgumentException("skillId and targetLevel must be positive");
			}
		}
	}
}
