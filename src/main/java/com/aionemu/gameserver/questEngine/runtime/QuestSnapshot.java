package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.questEngine.model.QuestStatus;
import com.aionemu.gameserver.questEngine.definition.QuestRewardKind;
import com.aionemu.gameserver.questEngine.definition.QuestPvpKillFacts;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 提供给纯条件求值的不可变事实快照。
 * Immutable fact snapshot supplied to pure condition evaluation.
 *
 * <p>背包与货币余额携带显式捕获标志：事实未被捕获的快照（例如玩家登出）
 * 会将 {@code inventoryCaptured()}/{@code currenciesCaptured()} 报告为 false，
 * 并使 {@link #itemCount}/{@link #balance} 失败关闭而非返回虚构的零。
 * 捕获标志为 true 的空 map 表示「已知为零」，绝不会与「未捕获」混淆。
 * Inventory and currency balances carry an explicit capture flag: a snapshot
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
		Boolean eventActive, Set<Integer> activeQuestIds, Race race,
		Map<Integer, Boolean> eventActivities) {

	/** 匹配玩家种族/外部事件事实之前规范形状的兼容构造器。 / Compatibility constructor matching the canonical shape before player-race/external-event facts. */
	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies,
			boolean inventoryCaptured, boolean currenciesCaptured, int interactionObjectId,
			int targetObjectId, int worldId, int instanceId, float x, float y, float z, byte heading,
			QuestCraftSnapshot craftFacts, QuestPvpKillFacts pvpFacts, QuestStartEligibility startEligibility,
			PlayerClass startingClass, PlayerClass playerClass, Gender gender, boolean targetlessDialog,
			QuestWorldFacts worldFacts, QuestTeamFacts teamFacts, int completeCount, Set<Integer> completedQuestIds,
			QuestEquipmentFacts equipmentFacts, Integer maxDp, QuestMembershipFacts membershipFacts,
			Boolean eventActive, Set<Integer> activeQuestIds) {
		this(playerId, questId, status, packedVariables, inventory, currencies, inventoryCaptured,
			currenciesCaptured, interactionObjectId, targetObjectId, worldId, instanceId, x, y, z, heading,
			craftFacts, pvpFacts, startEligibility, startingClass, playerClass, gender, targetlessDialog,
			worldFacts, teamFacts, completeCount, completedQuestIds, equipmentFacts, maxDp, membershipFacts,
			eventActive, activeQuestIds, null, null);
	}

	/** 匹配事件活动前规范记录形状的兼容构造器。 / Compatibility constructor matching the pre-event-active canonical record shape. */
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
			null, null, null, null);
	}

	/** 早于完成任务事实的调用方的兼容构造器。 / Compatibility constructor for callers that predate completed-quest facts. */
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
			worldFacts, teamFacts, completeCount, null, null, null, null, null, null, null, null);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies,
			boolean inventoryCaptured, boolean currenciesCaptured, int interactionObjectId,
			int targetObjectId, int worldId, int instanceId, float x, float y, float z, byte heading,
			QuestCraftSnapshot craftFacts, QuestPvpKillFacts pvpFacts) {
		this(playerId, questId, status, packedVariables, inventory, currencies, inventoryCaptured,
			currenciesCaptured, interactionObjectId, targetObjectId, worldId, instanceId, x, y, z, heading,
			craftFacts, pvpFacts, null, null, null, null, false, null, null, 0, null, null, null, null, null, null,
			null, null);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies,
			boolean inventoryCaptured, boolean currenciesCaptured, int interactionObjectId,
			int targetObjectId, int worldId, int instanceId, float x, float y, float z, byte heading) {
		this(playerId, questId, status, packedVariables, inventory, currencies, inventoryCaptured,
			currenciesCaptured, interactionObjectId, targetObjectId, worldId, instanceId, x, y, z, heading,
			null, null, null, null, null, null, false, null, null, 0, null, null, null, null, null, null, null, null);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies) {
		this(playerId, questId, status, packedVariables, inventory, currencies, true, true, 0,
			0, 0, 0, 0f, 0f, 0f, (byte) 0, null, null, null, null, null, null, false, null, null, 0, null, null, null, null, null, null,
			null, null);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory) {
		this(playerId, questId, status, packedVariables, inventory, Map.of(), true, true, 0,
			0, 0, 0, 0f, 0f, 0f, (byte) 0, null, null, null, null, null, null, false, null, null, 0, null, null, null, null, null, null,
			null, null);
	}

	public QuestSnapshot(int playerId, int questId, QuestStatus status, int packedVariables,
			Map<Integer, Integer> inventory, Map<QuestRewardKind, Long> currencies,
			boolean inventoryCaptured, boolean currenciesCaptured, int interactionObjectId) {
		this(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId,
			0, 0, 0, 0f, 0f, 0f, (byte) 0, null, null, null, null, null, null, false, null, null, 0, null, null, null, null, null, null,
			null, null);
	}

	public QuestSnapshot withInteractionObjectId(int objectId) {
		if (objectId < 0) {
			throw new IllegalArgumentException("objectId must be non-negative");
		}
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, objectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive, activeQuestIds, race,
			eventActivities);
	}

	public QuestSnapshot withTargetObjectId(int objectId) {
		if (objectId < 0) {
			throw new IllegalArgumentException("objectId must be non-negative");
		}
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, objectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive, activeQuestIds, race,
			eventActivities);
	}

	public QuestSnapshot withCraftFacts(QuestCraftSnapshot facts) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, Objects.requireNonNull(facts, "facts"), pvpFacts,
			startEligibility, startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts,
			completeCount, completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive,
			activeQuestIds, race, eventActivities);
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
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive, activeQuestIds, race,
			eventActivities);
	}

	public QuestSnapshot withStartEligibility(QuestStartEligibility eligibility) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts,
			Objects.requireNonNull(eligibility, "eligibility"), startingClass, playerClass, gender, targetlessDialog,
			worldFacts, teamFacts, completeCount, completedQuestIds, equipmentFacts, maxDp, membershipFacts,
			eventActive, activeQuestIds, race, eventActivities);
	}

	public QuestSnapshot withStartingClass(PlayerClass startingClass) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive, activeQuestIds, race,
			eventActivities);
	}

	public QuestSnapshot withPlayerClass(PlayerClass playerClass) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, Objects.requireNonNull(playerClass, "playerClass"), gender, targetlessDialog,
			worldFacts, teamFacts, completeCount, completedQuestIds, equipmentFacts, maxDp, membershipFacts,
			eventActive, activeQuestIds, race, eventActivities);
	}

	public QuestSnapshot withGender(Gender gender) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, Objects.requireNonNull(gender, "gender"), targetlessDialog, worldFacts,
			teamFacts, completeCount, completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive, activeQuestIds);
	}

	public QuestSnapshot withTargetlessDialog() {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, true, worldFacts, teamFacts, completeCount, completedQuestIds,
			equipmentFacts, maxDp, membershipFacts, eventActive, activeQuestIds, race, eventActivities);
	}

	public QuestSnapshot withWorldFacts(QuestWorldFacts facts) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, facts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive, activeQuestIds, race,
			eventActivities);
	}

	public QuestSnapshot withTeamFacts(QuestTeamFacts facts) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts,
			Objects.requireNonNull(facts, "facts"), completeCount, completedQuestIds, equipmentFacts, maxDp,
			membershipFacts, eventActive, activeQuestIds, race, eventActivities);
	}

	public QuestSnapshot withCompleteCount(int count) {
		if (count < 0) {
			throw new IllegalArgumentException("completeCount must be non-negative");
		}
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, count,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive, activeQuestIds, race,
			eventActivities);
	}

	/** 捕获玩家已完成任务 ID 以用于前置条件求值。 / Captures the player's completed quest ids for prerequisite evaluation. */
	public QuestSnapshot withCompletedQuestIds(Set<Integer> questIds) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			Objects.requireNonNull(questIds, "questIds"), equipmentFacts, maxDp, membershipFacts, eventActive,
			activeQuestIds, race, eventActivities);
	}

	/** 捕获已装备套装事实以用于装备依赖的对话条件。 / Captures equipped item-set facts for equipment-dependent dialog conditions. */
	public QuestSnapshot withEquipmentFacts(QuestEquipmentFacts facts) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, Objects.requireNonNull(facts, "facts"), maxDp, membershipFacts, eventActive,
			activeQuestIds, race, eventActivities);
	}

	/** 捕获类型化会员权限以用于权限依赖的任务路由。 / Captures typed membership permissions for permission-dependent quest routes. */
	public QuestSnapshot withMembershipFacts(QuestMembershipFacts facts) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, Objects.requireNonNull(facts, "facts"), eventActive,
			activeQuestIds, race, eventActivities);
	}

	/** 捕获玩家的最大 DP 以用于 {@code dp-at-max} 条件。 / Captures the player's maximum DP for a {@code dp-at-max} condition. */
	public QuestSnapshot withMaxDp(int value) {
		if (value < 0) {
			throw new IllegalArgumentException("maxDp must be non-negative");
		}
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, value, membershipFacts, eventActive, activeQuestIds, race,
			eventActivities);
	}

	/** 捕获当前事件服务是否包含此任务。 / Captures whether the current event service includes this quest. */
	public QuestSnapshot withEventActive(boolean active) {
		return withEventActive(Boolean.valueOf(active));
	}

	/** 捕获事件活动事实；{@code null} 表示来源不可用。 / Captures an event-activity fact; {@code null} means the source was unavailable. */
	public QuestSnapshot withEventActive(Boolean active) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, active, activeQuestIds, race,
			eventActivities);
	}

	/** 返回此快照是否捕获了事件活动事实。 / Returns whether event-active facts were captured for this snapshot. */
	public boolean eventActiveCaptured() {
		return eventActive != null;
	}

	/** 捕获玩家进行中（START/REWARD）任务 ID 以用于前置条件求值。 / Captures the player's in-progress (START/REWARD) quest ids for prerequisite evaluation. */
	public QuestSnapshot withActiveQuestIds(Set<Integer> questIds) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive,
			Objects.requireNonNull(questIds, "questIds"), race, eventActivities);
	}

	/** 捕获玩家的权威种族。 / Captures the player's authoritative race. */
	public QuestSnapshot withRace(Race race) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive, activeQuestIds,
			Objects.requireNonNull(race, "race"), eventActivities);
	}

	/** 为显式引用的任务 ID 捕获事件服务成员资格。 / Captures event-service membership for explicitly referenced quest ids. */
	public QuestSnapshot withEventActivities(Map<Integer, Boolean> activities) {
		return new QuestSnapshot(playerId, questId, status, packedVariables, inventory, currencies,
			inventoryCaptured, currenciesCaptured, interactionObjectId, targetObjectId,
			worldId, instanceId, x, y, z, heading, craftFacts, pvpFacts, startEligibility,
			startingClass, playerClass, gender, targetlessDialog, worldFacts, teamFacts, completeCount,
			completedQuestIds, equipmentFacts, maxDp, membershipFacts, eventActive, activeQuestIds, race,
			Objects.requireNonNull(activities, "activities"));
	}

	/** 返回显式捕获的事件活动事实，不可用时返回 null。 / Returns an explicitly captured event-activity fact, or null when unavailable. */
	public Boolean eventActivity(int requiredQuestId) {
		if (requiredQuestId <= 0) {
			throw new IllegalArgumentException("requiredQuestId must be positive");
		}
		return eventActivities == null ? null : eventActivities.get(requiredQuestId);
	}

	/** 此快照是否捕获了进行中任务事实。 / Whether active-quest facts were captured for this snapshot. */
	public boolean activeQuestsCaptured() {
		return activeQuestIds != null;
	}

	/** 仅当捕获的事实包含请求的进行中任务时返回 true。 / Returns true only when captured facts contain the requested in-progress quest. */
	public boolean hasActiveQuest(int requiredQuestId) {
		return activeQuestIds != null && activeQuestIds.contains(requiredQuestId);
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
		if (activeQuestIds != null) {
			activeQuestIds = Set.copyOf(activeQuestIds);
			if (activeQuestIds.stream().anyMatch(id -> id == null || id <= 0)) {
				throw new IllegalArgumentException("active quest ids must be positive");
			}
		}
		if (race == Race.PC_ALL) {
			throw new IllegalArgumentException("race must be ELYOS or ASMODIANS");
		}
		if (eventActivities != null) {
			eventActivities = Map.copyOf(eventActivities);
			if (eventActivities.entrySet().stream().anyMatch(entry -> entry.getKey() == null
					|| entry.getKey() <= 0 || entry.getValue() == null)) {
				throw new IllegalArgumentException("event activities contain an invalid quest fact");
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

	/** 此快照是否捕获了完成任务事实。 / Whether completed-quest facts were captured for this snapshot. */
	public boolean completedQuestsCaptured() {
		return completedQuestIds != null;
	}

	/** 仅当捕获的事实包含请求的已完成任务时返回 true。 / Returns true only when captured facts contain the requested completed quest. */
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
		// GOLD 与 KINAH 是同一持久化基纳字段的两个线上名称。 / GOLD and KINAH are two wire names for the same persistent kinah field.
		if (kind == QuestRewardKind.GOLD || kind == QuestRewardKind.KINAH) {
			return currencies.getOrDefault(kind == QuestRewardKind.GOLD
				? QuestRewardKind.KINAH : QuestRewardKind.GOLD, 0L);
		}
		if (kind == QuestRewardKind.CP || kind == QuestRewardKind.ABYSS_OP) {
			// 这些线上货币还没有捕获的生产余额来源。不要把缺失来源解释为已知的零余额。
			// These wire currencies have no captured production balance source yet.
			// Do not interpret an absent source as a known zero balance.
			throw new IllegalStateException("currency facts are not captured for " + kind);
		}
		return 0L;
	}

	public boolean craftFactsCaptured() {
		return craftFacts != null;
	}

	/** 返回是否捕获了最大 DP 事实。 / Returns whether maximum-DP facts were captured. */
	public boolean maxDpCaptured() {
		return maxDp != null;
	}

	/** 将捕获的当前 DP 余额与捕获的最大 DP 匹配。 / Matches a captured current DP balance against the captured maximum DP. */
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

	/** 世界事实可能缺失；调用方做 null 检查（缺失表示「未捕获」）。 / World facts may be absent; callers null-check (absence means "not captured"). */
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
