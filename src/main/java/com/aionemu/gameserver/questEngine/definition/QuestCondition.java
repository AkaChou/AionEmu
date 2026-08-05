package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Closed set of pure conditions evaluated against a quest snapshot. */
public sealed interface QuestCondition permits QuestCondition.StatusIs, QuestCondition.HasItem,
		QuestCondition.QuestVariableIs, QuestCondition.VariableAtLeast, QuestCondition.VariableBelow,
		QuestCondition.VariableSumIs, QuestCondition.VariableSumBelow,
		QuestCondition.RecipeKnown, QuestCondition.CanGrantCraftSkill, QuestCondition.PvpVictimLevelDelta,
		QuestCondition.PvpRecipientInZone, QuestCondition.StartEligible, QuestCondition.PlayerClassIs,
		QuestCondition.WorldIs, QuestCondition.WorldNpcIs, QuestCondition.ZoneIs {
	record StartEligible() implements QuestCondition {
	}

	/** Matches the player's starting class (advanced classes normalize to their base). */
	record PlayerClassIs(PlayerClass startingClass) implements QuestCondition {
		public PlayerClassIs {
			if (startingClass == null) {
				throw new NullPointerException("startingClass");
			}
		}
	}

	/** Matches the world the player is currently in; expected=false is the explicit "not in world" case. */
	record WorldIs(int worldId, boolean expected) implements QuestCondition {
		public WorldIs {
			if (worldId <= 0) {
				throw new IllegalArgumentException("worldId must be positive");
			}
		}
	}

	/** Matches one authoritative zone occupied by the player. */
	record ZoneIs(String zone, boolean expected) implements QuestCondition {
		public ZoneIs {
			if (zone == null || zone.isBlank()) {
				throw new IllegalArgumentException("zone must not be blank");
			}
			zone = zone.toUpperCase(java.util.Locale.ROOT);
		}

		public ZoneIs(String zone) {
			this(zone, true);
		}
	}

	/** Matches presence of an NPC template in the player's current world instance. */
	record WorldNpcIs(int npcId, boolean expected) implements QuestCondition {
		public WorldNpcIs {
			if (npcId <= 0) {
				throw new IllegalArgumentException("npcId must be positive");
			}
		}
	}

	record StatusIs(QuestStatus status) implements QuestCondition {
		public StatusIs {
			if (status == null) {
				throw new NullPointerException("status");
			}
		}
	}

	record HasItem(int itemId, int count) implements QuestCondition {
		public HasItem {
			if (itemId <= 0 || count <= 0) {
				throw new IllegalArgumentException("item id and count must be positive");
			}
		}
	}

	record QuestVariableIs(String field, int value) implements QuestCondition {
		public QuestVariableIs {
			if (field == null || field.isBlank()) {
				throw new IllegalArgumentException("field must not be blank");
			}
		}
	}

	/** 字段值达到阈值（大于等于）。Matches when the field is at least the given value. */
	record VariableAtLeast(String field, int value) implements QuestCondition {
		public VariableAtLeast {
			if (field == null || field.isBlank()) {
				throw new IllegalArgumentException("field must not be blank");
			}
		}
	}

	/** 字段值低于上限（小于）。Matches when the field is below the given cap. */
	record VariableBelow(String field, int value) implements QuestCondition {
		public VariableBelow {
			if (field == null || field.isBlank()) {
				throw new IllegalArgumentException("field must not be blank");
			}
		}
	}

	/** Matches when the sum of the listed progress fields equals the target value. */
	record VariableSumIs(List<String> fields, int value) implements QuestCondition {
		public VariableSumIs {
			fields = validatedFields(fields);
		}
	}

	/** Matches when the sum of the listed progress fields is below the target value. */
	record VariableSumBelow(List<String> fields, int value) implements QuestCondition {
		public VariableSumBelow {
			fields = validatedFields(fields);
		}
	}

	/** Matches an authoritative recipe fact, including the explicit "not known" case. */
	record RecipeKnown(int recipeId, boolean expected) implements QuestCondition {
		public RecipeKnown {
			if (recipeId <= 0) {
				throw new IllegalArgumentException("recipeId must be positive");
			}
		}
	}

	/** Matches expert/master eligibility captured before the event. */
	record CanGrantCraftSkill(int skillId, int targetLevel) implements QuestCondition {
		public CanGrantCraftSkill {
			if (skillId <= 0 || targetLevel <= 0) {
				throw new IllegalArgumentException("skillId and targetLevel must be positive");
			}
		}
	}

	/** Matches recipientLevel - victimLevel against an inclusive legacy PvP window. */
	record PvpVictimLevelDelta(int minimumRecipientDelta, int maximumRecipientDelta) implements QuestCondition {
		public PvpVictimLevelDelta {
			if (minimumRecipientDelta > maximumRecipientDelta) {
				throw new IllegalArgumentException("minimumRecipientDelta must not exceed maximumRecipientDelta");
			}
		}
	}

	/** Matches one authoritative Zone occupied by the credited PvP recipient. */
	record PvpRecipientInZone(String zone) implements QuestCondition {
		public PvpRecipientInZone {
			if (zone == null || zone.isBlank()) {
				throw new IllegalArgumentException("zone must not be blank");
			}
			zone = zone.toUpperCase(java.util.Locale.ROOT);
		}
	}

	private static List<String> validatedFields(List<String> fields) {
		Objects.requireNonNull(fields, "fields");
		if (fields.isEmpty()) {
			throw new IllegalArgumentException("fields must not be empty");
		}
		List<String> copy = List.copyOf(fields);
		if (copy.stream().anyMatch(field -> field == null || field.isBlank())) {
			throw new IllegalArgumentException("fields must not contain blank values");
		}
		if (copy.size() != new HashSet<>(copy).size()) {
			throw new IllegalArgumentException("fields must not contain duplicates");
		}
		return copy;
	}
}
