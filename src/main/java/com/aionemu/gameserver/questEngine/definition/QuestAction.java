package com.aionemu.gameserver.questEngine.definition;

/** Closed set of required mutations in the quest transaction. */
public sealed interface QuestAction permits QuestAction.RemoveItem, QuestAction.SetVariable,
		QuestAction.SetStatus, QuestAction.GrantReward, QuestAction.LearnRecipe,
		QuestAction.ForgetRecipe, QuestAction.GrantCraftSkill {
	record RemoveItem(int itemId, int count) implements QuestAction {
		public RemoveItem {
			if (itemId <= 0 || count <= 0) {
				throw new IllegalArgumentException("item id and count must be positive");
			}
		}
	}

	record SetVariable(String field, int value) implements QuestAction {
		public SetVariable {
			if (field == null || field.isBlank()) {
				throw new IllegalArgumentException("field must not be blank");
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

	record GrantReward(String kind, int id, long amount) implements QuestAction {
		public GrantReward {
			QuestRewardKind.fromWire(kind);
			new QuestReward(kind, id, amount);
		}

		public QuestRewardKind rewardKind() {
			return QuestRewardKind.fromWire(kind);
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
