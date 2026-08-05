package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.definition.QuestPvpKillFacts;

import java.util.Map;
import java.util.Objects;

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
		QuestWorldFacts worldFacts, QuestTeamFacts teamFacts, int completeCount) {

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies,
			boolean inventoryCaptured, boolean currenciesCaptured, int interactionObjectId,
			int targetObjectId, int worldId, int instanceId, float x, float y, float z, byte heading,
			QuestCraftSnapshot craftFacts, QuestPvpKillFacts pvpFacts) {
		this(playerId, questId, status, packedVariables, inventory, currencies, inventoryCaptured,
			currenciesCaptured, interactionObjectId, targetObjectId, worldId, instanceId, x, y, z, heading,
			craftFacts, pvpFacts, null, null, null, null, false, null, null, 0);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies,
			boolean inventoryCaptured, boolean currenciesCaptured, int interactionObjectId,
			int targetObjectId, int worldId, int instanceId, float x, float y, float z, byte heading) {
		this(playerId, questId, status, packedVariables, inventory, currencies, inventoryCaptured,
			currenciesCaptured, interactionObjectId, targetObjectId, worldId, instanceId, x, y, z, heading,
			null, null, null, null, null, null, false, null, null, 0);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies) {
		this(playerId, questId, status, packedVariables, inventory, currencies, true, true, 0,
			0, 0, 0, 0f, 0f, 0f, (byte) 0, null, null, null, null, null, null, false, null, null, 0);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory) {
		this(playerId, questId, status, packedVariables, inventory, Map.of(), true, true, 0,
			0, 0, 0, 0f, 0f, 0f, (byte) 0, null, null, null, null, null, null, false, null, null, 0);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies,
			boolean inventoryCaptured, boolean currenciesCaptured, int interactionObjectId) {
		this(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId,
			0, 0, 0, 0f, 0f, 0f, (byte) 0, null, null, null, null, null, null, false, null, null, 0);
	}

	public QuestSnapshot withInteractionObjectId(int objectId) {
		if (objectId < 0) {
			throw new IllegalArgumentException("objectId must be non-negative");
		}
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, objectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount);
	}

	public QuestSnapshot withTargetObjectId(int objectId) {
		if (objectId < 0) {
			throw new IllegalArgumentException("objectId must be non-negative");
		}
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, objectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount);
	}

	public QuestSnapshot withCraftFacts(QuestCraftSnapshot facts) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, Objects.requireNonNull(facts, "facts"), pvpFacts,
			startEligibility, startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts,
			completeCount);
	}

	public QuestSnapshot withPvpFacts(QuestPvpKillFacts facts) {
		Objects.requireNonNull(facts, "facts");
		if (facts.recipientId() != playerId) {
			throw new IllegalArgumentException("PvP facts do not belong to this player snapshot");
		}
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, facts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount);
	}

	public QuestSnapshot withStartEligibility(QuestStartEligibility eligibility) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts,
			Objects.requireNonNull(eligibility, "eligibility"), startingClass, playerClass, gender, targetlessDialog,
			worldFacts, teamFacts, completeCount);
	}

	public QuestSnapshot withStartingClass(PlayerClass startingClass) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount);
	}

	public QuestSnapshot withPlayerClass(PlayerClass playerClass) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, Objects.requireNonNull(playerClass, "playerClass"), gender, targetlessDialog,
			worldFacts, teamFacts, completeCount);
	}

	public QuestSnapshot withGender(Gender gender) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, Objects.requireNonNull(gender, "gender"), targetlessDialog, worldFacts,
			teamFacts, completeCount);
	}

	public QuestSnapshot withTargetlessDialog() {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, true, worldFacts, teamFacts, completeCount);
	}

	public QuestSnapshot withWorldFacts(QuestWorldFacts facts) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, facts, teamFacts, completeCount);
	}

	public QuestSnapshot withTeamFacts(QuestTeamFacts facts) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts,
			Objects.requireNonNull(facts, "facts"), completeCount);
	}

	public QuestSnapshot withCompleteCount(int count) {
		if (count < 0) {
			throw new IllegalArgumentException("completeCount must be non-negative");
		}
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, count);
	}

	public QuestSnapshot {
		if (playerId <= 0 || questId <= 0) {
			throw new IllegalArgumentException("playerId and questId must be positive");
		}
		if (completeCount < 0) {
			throw new IllegalArgumentException("completeCount must be non-negative");
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
