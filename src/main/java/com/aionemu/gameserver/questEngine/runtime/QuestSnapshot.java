package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.definition.QuestPvpKillFacts;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable fact snapshot supplied to pure condition evaluation.
 *
 * <p>Inventory and currency balances carry an explicit capture flag: a snapshot
 * where the facts were not captured (for example a player being logged out)
 * reports {@code inventoryCaptured()}/{@code currenciesCaptured()} as false and
 * makes {@link #itemCount}/{@link #balance} fail closed instead of returning a
 * fabricated zero. An empty map with capture flags true means "known to be
 * zero", which is never confused with "not captured".</p>
 */
public record QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
		Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies,
		boolean inventoryCaptured, boolean currenciesCaptured, int interactionObjectId,
		int targetObjectId, int worldId, int instanceId, float x, float y, float z, byte heading,
		QuestCraftSnapshot craftFacts, QuestPvpKillFacts pvpFacts, QuestStartEligibility startEligibility,
		PlayerClass startingClass, PlayerClass playerClass, Gender gender, boolean targetlessDialog,
		QuestWorldFacts worldFacts, QuestTeamFacts teamFacts, int completeCount, Set<Integer> completedQuestIds,
		QuestEquipmentFacts equipmentFacts, Integer maxDp, QuestMembershipFacts membershipFacts,
		Boolean eventActive) {

	/** Compatibility constructor matching the pre-event-active canonical record shape. */
	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies,
			boolean inventoryCaptured, boolean currenciesCaptured, int interactionObjectId,
			int targetObjectId, int worldId, int instanceId, float x, float y, float z, byte heading,
			QuestCraftSnapshot craftFacts, QuestPvpKillFacts pvpFacts, QuestStartEligibility startEligibility,
			PlayerClass startingClass, PlayerClass playerClass, Gender gender, boolean targetlessDialog,
			QuestWorldFacts worldFacts, QuestTeamFacts teamFacts, int completeCount, Set<Integer> completedQuestIds,
			QuestEquipmentFacts equipmentFacts, Integer maxDp, QuestMembershipFacts membershipFacts) {
		this(playerId, questId, status, packedVariables, inventory, currencies, inventoryCaptured,
			currenciesCaptured, interactionObjectId, targetObjectId, worldId, instanceId, x, y, z, heading,
			craftFacts, pvpFacts, startEligibility, startingClass, playerClass, gender, targetlessDialog,
			worldFacts, teamFacts, completeCount, completedQuestIds, equipmentFacts, maxDp, membershipFacts,
			null);
	}

	/** Compatibility constructor for callers that predate completed-quest facts. */
	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies,
			boolean inventoryCaptured, boolean currenciesCaptured, int interactionObjectId,
			int targetObjectId, int worldId, int instanceId, float x, float y, float z, byte heading,
			QuestCraftSnapshot craftFacts, QuestPvpKillFacts pvpFacts, QuestStartEligibility startEligibility,
			PlayerClass startingClass, PlayerClass playerClass, Gender gender, boolean targetlessDialog,
			QuestWorldFacts worldFacts, QuestTeamFacts teamFacts, int completeCount) {
		this(playerId, questId, status, packedVariables, inventory, currencies, inventoryCaptured,
			currenciesCaptured, interactionObjectId, targetObjectId, worldId, instanceId, x, y, z, heading,
			craftFacts, pvpFacts, startEligibility, startingClass, playerClass, gender, targetlessDialog,
			worldFacts, teamFacts, completeCount, null, null, null, null, null);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies,
			boolean inventoryCaptured, boolean currenciesCaptured, int interactionObjectId,
			int targetObjectId, int worldId, int instanceId, float x, float y, float z, byte heading,
			QuestCraftSnapshot craftFacts, QuestPvpKillFacts pvpFacts) {
		this(playerId, questId, status, packedVariables, inventory, currencies, inventoryCaptured,
			currenciesCaptured, interactionObjectId, targetObjectId, worldId, instanceId, x, y, z, heading,
			craftFacts, pvpFacts, null, null, null, null, false, null, null, 0, null, null, null, null, null);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies,
			boolean inventoryCaptured, boolean currenciesCaptured, int interactionObjectId,
			int targetObjectId, int worldId, int instanceId, float x, float y, float z, byte heading) {
		this(playerId, questId, status, packedVariables, inventory, currencies, inventoryCaptured,
			currenciesCaptured, interactionObjectId, targetObjectId, worldId, instanceId, x, y, z, heading,
			null, null, null, null, null, null, false, null, null, 0, null, null, null, null, null);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies) {
		this(playerId, questId, status, packedVariables, inventory, currencies, true, true, 0,
			0, 0, 0, 0f, 0f, 0f, (byte) 0, null, null, null, null, null, null, false, null, null, 0, null, null, null, null, null);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory) {
		this(playerId, questId, status, packedVariables, inventory, Map.of(), true, true, 0,
			0, 0, 0, 0f, 0f, 0f, (byte) 0, null, null, null, null, null, null, false, null, null, 0, null, null, null, null, null);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies,
			boolean inventoryCaptured, boolean currenciesCaptured, int interactionObjectId) {
		this(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId,
			0, 0, 0, 0f, 0f, 0f, (byte) 0, null, null, null, null, null, null, false, null, null, 0, null, null, null, null, null);
	}

	public QuestSnapshot withInteractionObjectId(int objectId) {
		if (objectId < 0) {
			throw new IllegalArgumentException("objectId must be non-negative");
		}
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, objectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive);
	}

	public QuestSnapshot withTargetObjectId(int objectId) {
		if (objectId < 0) {
			throw new IllegalArgumentException("objectId must be non-negative");
		}
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, objectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive);
	}

	public QuestSnapshot withCraftFacts(QuestCraftSnapshot facts) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, Objects.requireNonNull(facts, "facts"), pvpFacts,
			startEligibility, startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts,
			completeCount, completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive);
	}

	public QuestSnapshot withPvpFacts(QuestPvpKillFacts facts) {
		Objects.requireNonNull(facts, "facts");
		if (facts.recipientId() != playerId) {
			throw new IllegalArgumentException("PvP facts do not belong to this player snapshot");
		}
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, facts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive);
	}

	public QuestSnapshot withStartEligibility(QuestStartEligibility eligibility) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts,
			Objects.requireNonNull(eligibility, "eligibility"), startingClass, playerClass, gender, targetlessDialog,
			worldFacts, teamFacts, completeCount, completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive);
	}

	public QuestSnapshot withStartingClass(PlayerClass startingClass) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive);
	}

	public QuestSnapshot withPlayerClass(PlayerClass playerClass) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, Objects.requireNonNull(playerClass, "playerClass"), gender, targetlessDialog,
			worldFacts, teamFacts, completeCount, completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive);
	}

	public QuestSnapshot withGender(Gender gender) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, Objects.requireNonNull(gender, "gender"), targetlessDialog, worldFacts,
			teamFacts, completeCount, completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive);
	}

	public QuestSnapshot withTargetlessDialog() {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, true, worldFacts, teamFacts, completeCount, completedQuestIds,
			equipmentFacts, maxDp, membershipFacts, eventActive);
	}

	public QuestSnapshot withWorldFacts(QuestWorldFacts facts) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, facts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive);
	}

	public QuestSnapshot withTeamFacts(QuestTeamFacts facts) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts,
			Objects.requireNonNull(facts, "facts"), completeCount, completedQuestIds, equipmentFacts, maxDp,
			membershipFacts, eventActive);
	}

	public QuestSnapshot withCompleteCount(int count) {
		if (count < 0) {
			throw new IllegalArgumentException("completeCount must be non-negative");
		}
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, count,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive);
	}

	/** Captures the player's completed quest ids for prerequisite evaluation. */
	public QuestSnapshot withCompletedQuestIds(Set<Integer> questIds) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			Objects.requireNonNull(questIds, "questIds"), equipmentFacts, maxDp, membershipFacts, eventActive);
	}

	/** Captures equipped item-set facts for equipment-dependent dialog conditions. */
	public QuestSnapshot withEquipmentFacts(QuestEquipmentFacts facts) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, Objects.requireNonNull(facts, "facts"), maxDp, membershipFacts, eventActive);
	}

	/** Captures typed membership permissions for permission-dependent quest routes. */
	public QuestSnapshot withMembershipFacts(QuestMembershipFacts facts) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, Objects.requireNonNull(facts, "facts"), eventActive);
	}

	/** Captures the player's maximum DP for a {@code dp-at-max} condition. */
	public QuestSnapshot withMaxDp(int value) {
		if (value < 0) {
			throw new IllegalArgumentException("maxDp must be non-negative");
		}
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, value, membershipFacts, eventActive);
	}

	/** Captures whether the current event service includes this quest. */
	public QuestSnapshot withEventActive(boolean active) {
		return withEventActive(Boolean.valueOf(active));
	}

	/** Captures an event-activity fact; {@code null} means the source was unavailable. */
	public QuestSnapshot withEventActive(Boolean active) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, active);
	}

	/** Returns whether event-active facts were captured for this snapshot. */
	public boolean eventActiveCaptured() {
		return eventActive != null;
	}

	public QuestSnapshot {
		if (playerId <= 0 || questId <= 0) {
			throw new IllegalArgumentException("playerId and questId must be positive");
		}
		if (completeCount < 0) {
			throw new IllegalArgumentException("completeCount must be non-negative");
		}
		if (maxDp != null && maxDp < 0) {
			throw new IllegalArgumentException("maxDp must be non-negative");
		}
		if (completedQuestIds != null) {
			completedQuestIds = Set.copyOf(completedQuestIds);
			if (completedQuestIds.stream().anyMatch(id -> id == null || id <= 0)) {
				throw new IllegalArgumentException("completed quest ids must be positive");
			}
		}
		if (interactionObjectId < 0 || targetObjectId < 0 || worldId < 0 || instanceId < 0) {
			throw new IllegalArgumentException("object, world, and instance ids must be non-negative");
		}
		if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
			throw new IllegalArgumentException("position coordinates must be finite");
		}
		status = Objects.requireNonNull(status, "status");
		if (inventory == null) {
			inventoryCaptured = false;
			inventory = Map.of();
		} else {
			inventory = Map.copyOf(inventory);
			if (inventory.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getKey() <= 0
					|| entry.getValue() == null || entry.getValue() < 0)) {
				throw new IllegalArgumentException("inventory snapshot contains an invalid item count");
			}
		}
		if (currencies == null) {
			currenciesCaptured = false;
			currencies = Map.of();
		} else {
			currencies = Map.copyOf(currencies);
			if (currencies.entrySet().stream().anyMatch(entry -> entry.getKey() == null || entry.getValue() == null
					|| entry.getValue() < 0 || !entry.getKey().isCurrency())) {
				throw new IllegalArgumentException("currency snapshot contains an invalid balance");
			}
		}
	}

	public boolean inventoryCaptured() {
		return inventoryCaptured;
	}

	public boolean currenciesCaptured() {
		return currenciesCaptured;
	}

	/** Whether completed-quest facts were captured for this snapshot. */
	public boolean completedQuestsCaptured() {
		return completedQuestIds != null;
	}

	/** Returns true only when captured facts contain the requested completed quest. */
	public boolean hasCompletedQuest(int requiredQuestId) {
		return completedQuestIds != null && completedQuestIds.contains(requiredQuestId);
	}

	public int itemCount(int itemId) {
		if (!inventoryCaptured) {
			throw new IllegalStateException("inventory facts are not captured in this snapshot");
		}
		return inventory.getOrDefault(itemId, 0);
	}

	public long balance(QuestRewardKind kind) {
		if (!currenciesCaptured) {
			throw new IllegalStateException("currency facts are not captured in this snapshot");
		}
		if (kind == null || !kind.isCurrency()) {
			throw new IllegalArgumentException("currency balance requires a currency reward kind");
		}
		Long exact = currencies.get(kind);
		if (exact != null) {
			return exact;
		}
		// GOLD and KINAH are two wire names for the same persistent kinah field.
		if (kind == QuestRewardKind.GOLD || kind == QuestRewardKind.KINAH) {
			return currencies.getOrDefault(kind == QuestRewardKind.GOLD
				? QuestRewardKind.KINAH : QuestRewardKind.GOLD, 0L);
		}
		if (kind == QuestRewardKind.CP || kind == QuestRewardKind.ABYSS_OP) {
			// These wire currencies have no captured production balance source yet.
			// Do not interpret an absent source as a known zero balance.
			throw new IllegalStateException("currency facts are not captured for " + kind);
		}
		return 0L;
	}

	public boolean craftFactsCaptured() {
		return craftFacts != null;
	}

	/** Returns whether maximum-DP facts were captured. */
	public boolean maxDpCaptured() {
		return maxDp != null;
	}

	/** Matches a captured current DP balance against the captured maximum DP. */
	public boolean dpAtMax() {
		if (maxDp == null) {
			return false;
		}
		try {
			return balance(QuestRewardKind.DP) == maxDp;
		} catch (IllegalStateException unknownFacts) {
			return false;
		}
	}

	public boolean pvpFactsCaptured() {
		return pvpFacts != null;
	}

	public QuestStartEligibility startEligibility() {
		if (startEligibility == null) {
			throw new IllegalStateException("quest start eligibility is not captured in this snapshot");
		}
		return startEligibility;
	}

	public QuestPvpKillFacts pvpFacts() {
		if (pvpFacts == null) {
			throw new IllegalStateException("PvP facts are not captured in this snapshot");
		}
		return pvpFacts;
	}

	/** World facts may be absent; callers null-check (absence means "not captured"). */
	public QuestWorldFacts worldFacts() {
		return worldFacts;
	}

	public boolean recipeKnown(int recipeId) {
		if (craftFacts == null) {
			throw new IllegalStateException("craft facts are not captured in this snapshot");
		}
		return craftFacts.recipeKnown(recipeId);
	}

	public boolean canGrantCraftSkill(int skillId, int targetLevel) {
		if (craftFacts == null) {
			throw new IllegalStateException("craft facts are not captured in this snapshot");
		}
		return craftFacts.canGrantCraftSkill(skillId, targetLevel);
	}
}
