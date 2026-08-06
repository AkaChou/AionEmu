package com.aionemu.gameserver.questEngine.definition;

import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.questEngine.model.QuestStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Closed set of pure conditions evaluated against a quest snapshot. */
public sealed interface QuestCondition permits QuestCondition.StatusIs, QuestCondition.HasItem,
		QuestCondition.QuestVariableIs, QuestCondition.VariableAtLeast, QuestCondition.VariableBelow,
		QuestCondition.VariableSumIs, QuestCondition.VariableSumBelow,
		QuestCondition.RecipeKnown, QuestCondition.CanGrantCraftSkill, QuestCondition.PvpVictimLevelDelta,
		QuestCondition.PvpRecipientInZone, QuestCondition.StartEligible, QuestCondition.PlayerClassIs,
		QuestCondition.AdvancedClassIs, QuestCondition.GenderIs, QuestCondition.PlayerInGroup,
		QuestCondition.WorldIs, QuestCondition.WorldNpcIs, QuestCondition.ZoneIs,
		QuestCondition.NpcHpBelowPercent, QuestCondition.CurrencyAtLeast, QuestCondition.CurrencyBelow,
		QuestCondition.QuestsFinished, QuestCondition.UnfinishedQuest, QuestCondition.NoAcquiredQuest,
		QuestCondition.AcquiredQuest, QuestCondition.EquipmentSetEquipped, QuestCondition.EquippedItem,
		QuestCondition.MembershipPermission, QuestCondition.DpAtMax, QuestCondition.CompleteCountIs,
		QuestCondition.EventActive {
	/** Matches a typed membership capability captured from the live account. */
	record MembershipPermission(QuestMembershipPermission permission, boolean expected) implements QuestCondition {
		public MembershipPermission {
			if (permission == null) {
				throw new NullPointerException("permission");
			}
		}

		public MembershipPermission(QuestMembershipPermission permission) {
			this(permission, true);
		}
	}

	/** Matches the number of copies of a concrete item in the equipment projection. */
	record EquippedItem(int itemId, int count, boolean expected) implements QuestCondition {
		public EquippedItem {
			if (itemId <= 0 || count <= 0) {
				throw new IllegalArgumentException("item id and count must be positive");
			}
		}

		public EquippedItem(int itemId, int count) {
			this(itemId, count, true);
		}

		public EquippedItem(int itemId) {
			this(itemId, 1, true);
		}
	}

	record StartEligible() implements QuestCondition {
	}

	/** Matches when every listed prerequisite quest is already completed. */
	record QuestsFinished(Set<Integer> questIds) implements QuestCondition {
		public QuestsFinished {
			if (questIds == null || questIds.isEmpty()) {
				throw new IllegalArgumentException("questIds must not be empty");
			}
			if (questIds.stream().anyMatch(id -> id == null || id <= 0)) {
				throw new IllegalArgumentException("questIds must be positive");
			}
			questIds = Set.copyOf(questIds);
		}
	}

	/** Matches when none of the listed prerequisite quests is completed yet. */
	record UnfinishedQuest(Set<Integer> questIds) implements QuestCondition {
		public UnfinishedQuest {
			if (questIds == null || questIds.isEmpty()) {
				throw new IllegalArgumentException("questIds must not be empty");
			}
			if (questIds.stream().anyMatch(id -> id == null || id <= 0)) {
				throw new IllegalArgumentException("questIds must be positive");
			}
			questIds = Set.copyOf(questIds);
		}
	}

	/** Matches when none of the listed quests is completed or currently acquired (START/REWARD). */
	record NoAcquiredQuest(Set<Integer> questIds) implements QuestCondition {
		public NoAcquiredQuest {
			if (questIds == null || questIds.isEmpty()) {
				throw new IllegalArgumentException("questIds must not be empty");
			}
			if (questIds.stream().anyMatch(id -> id == null || id <= 0)) {
				throw new IllegalArgumentException("questIds must be positive");
			}
			questIds = Set.copyOf(questIds);
		}
	}

	/**
	 * Matches when every listed quest is already acquired (completed or in progress),
	 * mirroring the legacy {@code acquired} start condition.
	 */
	record AcquiredQuest(Set<Integer> questIds) implements QuestCondition {
		public AcquiredQuest {
			if (questIds == null || questIds.isEmpty()) {
				throw new IllegalArgumentException("questIds must not be empty");
			}
			if (questIds.stream().anyMatch(id -> id == null || id <= 0)) {
				throw new IllegalArgumentException("questIds must be positive");
			}
			questIds = Set.copyOf(questIds);
		}
	}

	/** Matches the player's starting class (advanced classes normalize to their base). */
	record PlayerClassIs(PlayerClass startingClass) implements QuestCondition {
		public PlayerClassIs {
			if (startingClass == null) {
				throw new NullPointerException("startingClass");
			}
		}
	}

	/** Matches the concrete advanced class captured from the player. */
	record AdvancedClassIs(PlayerClass playerClass) implements QuestCondition {
		public AdvancedClassIs {
			if (playerClass == null || playerClass == PlayerClass.ALL || playerClass.isStartingClass()) {
				throw new IllegalArgumentException("playerClass must be a concrete advanced class");
			}
		}
	}

	/** 匹配快照中的玩家性别；未知事实安全失败。Matches captured gender; unknown facts fail closed. */
	record GenderIs(Gender gender) implements QuestCondition {
		public GenderIs {
			if (gender == null || gender == Gender.DUMMY) {
				throw new IllegalArgumentException("gender must be MALE or FEMALE");
			}
		}
	}

	/** Matches whether the player is in a regular group (not merely an alliance). */
	record PlayerInGroup(boolean expected) implements QuestCondition {
		public PlayerInGroup() {
			this(true);
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

	/** Matches an authoritative AttackNpc callback while the target is below a strict HP percentage. */
	record NpcHpBelowPercent(int npcId, int percent) implements QuestCondition {
		public NpcHpBelowPercent {
			if (npcId <= 0) {
				throw new IllegalArgumentException("npcId must be positive");
			}
			if (percent < 0 || percent > 100) {
				throw new IllegalArgumentException("percent must be between 0 and 100");
			}
		}
	}

	/** Matches a captured balance before a currency debit branch. */
	record CurrencyAtLeast(QuestRewardKind kind, long amount) implements QuestCondition {
		public CurrencyAtLeast {
			if (kind == null || !kind.isCurrency()) {
				throw new IllegalArgumentException("kind must be a supported currency");
			}
			if (amount <= 0) {
				throw new IllegalArgumentException("amount must be positive");
			}
		}
	}

	/** Matches a captured currency balance strictly below the required amount. */
	record CurrencyBelow(QuestRewardKind kind, long amount) implements QuestCondition {
		public CurrencyBelow {
			if (kind == null || !kind.isCurrency()) {
				throw new IllegalArgumentException("kind must be a supported currency");
			}
			if (amount <= 0) {
				throw new IllegalArgumentException("amount must be positive");
			}
		}
	}

	/** Matches whether any listed equipment set has exactly the requested equipped-part count. */
	record EquipmentSetEquipped(Set<Integer> setIds, int count, boolean expected) implements QuestCondition {
		public EquipmentSetEquipped {
			if (setIds == null || setIds.isEmpty() || setIds.stream().anyMatch(id -> id == null || id <= 0)) {
				throw new IllegalArgumentException("setIds must contain positive ids");
			}
			if (count < 0) {
				throw new IllegalArgumentException("count must be non-negative");
			}
			setIds = Set.copyOf(setIds);
		}

		public EquipmentSetEquipped(Set<Integer> setIds, int count) {
			this(setIds, count, true);
		}
	}

	/** Matches when the captured current DP equals the captured maximum DP. */
	record DpAtMax() implements QuestCondition {
	}

	/**
	 * Matches the number of times the quest has been completed so far
	 * (for example the ninth completion unlocking an extra event reward).
	 */
	record CompleteCountIs(int value, boolean expected) implements QuestCondition {
		public CompleteCountIs {
			if (value < 0) {
				throw new IllegalArgumentException("value must be non-negative");
			}
		}

		public CompleteCountIs(int value) {
			this(value, true);
		}
	}

	/**
	 * Matches whether the current game event still contains this quest,
	 * mirroring {@code EventService.checkQuestIsActive}. Unknown facts fail closed.
	 */
	record EventActive(boolean expected) implements QuestCondition {
		public EventActive() {
			this(true);
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

	record HasItem(int itemId, int count, boolean expected) implements QuestCondition {
		public HasItem {
			if (itemId <= 0 || count <= 0) {
				throw new IllegalArgumentException("item id and count must be positive");
			}
		}

		public HasItem(int itemId, int count) {
			this(itemId, count, true);
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
