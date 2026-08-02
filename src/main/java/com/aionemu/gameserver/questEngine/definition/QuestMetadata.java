package com.aionemu.gameserver.questEngine.definition;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Collections;

/** Immutable metadata shared by every execution representation. */
public record QuestMetadata(
	String name,
	int displayNameId,
	int minLevel,
	int maxLevel,
	Set<String> permittedRaces,
	String category,
	RepeatPolicy repeatPolicy,
	Set<Integer> prerequisites,
	List<QuestItemRequirement> itemRequirements,
	List<QuestReward> rewards,
	List<QuestDrop> drops,
	Set<String> permittedClasses,
	String permittedGender,
	int rank,
	int maxCountLimitedQuest,
	int countRecoverLimitedQuest,
	boolean cannotShare,
	boolean cannotGiveup,
	boolean bountyReward,
	int useClassReward,
	Integer combineSkill,
	Integer combineSkillPoint,
	boolean timer,
	Set<String> repeatCycles,
	int npcFactionId,
	String mentorType,
	String targetType,
	int titleId,
	List<QuestItemRequirement> inventoryItems,
	List<QuestItemRequirement> questWorkItems,
	List<QuestReward> extendedRewards,
	List<QuestBonus> bonuses,
	List<QuestKill> kills,
	List<QuestStartCondition> startConditions,
	Map<String, List<QuestReward>> classRewards) {

	/** Backward-compatible constructor for the initial definition DSL. */
	public QuestMetadata(String name, int displayNameId, int minLevel, int maxLevel,
		Set<String> permittedRaces, String category, RepeatPolicy repeatPolicy,
		Set<Integer> prerequisites, List<QuestItemRequirement> itemRequirements,
		List<QuestReward> rewards, List<QuestDrop> drops) {
		this(name, displayNameId, minLevel, maxLevel, permittedRaces, category, repeatPolicy,
			prerequisites, itemRequirements, rewards, drops, Set.of(), "", 0, 1, 1, false,
			false, false, 0, null, null, false, Set.of(), 0, "NONE", "NONE", 0, List.of(),
			List.of(), List.of(), List.of(), List.of(), List.of(), Map.of());
	}

	public QuestMetadata {
		name = requireText(name, "name");
		if (displayNameId < 0) {
			throw new IllegalArgumentException("displayNameId must be non-negative");
		}
		if (minLevel < 0 || maxLevel < minLevel) {
			throw new IllegalArgumentException("invalid quest level range");
		}
		permittedRaces = copySet(permittedRaces, "permittedRaces");
		category = requireText(category, "category");
		repeatPolicy = Objects.requireNonNull(repeatPolicy, "repeatPolicy");
		prerequisites = copySet(prerequisites, "prerequisites");
		if (prerequisites.stream().anyMatch(id -> id == null || id <= 0)) {
			throw new IllegalArgumentException("prerequisite quest ids must be positive");
		}
		itemRequirements = List.copyOf(Objects.requireNonNull(itemRequirements, "itemRequirements"));
		rewards = List.copyOf(Objects.requireNonNull(rewards, "rewards"));
		drops = List.copyOf(Objects.requireNonNull(drops, "drops"));
		permittedClasses = copySet(permittedClasses, "permittedClasses");
		permittedGender = normalizeText(permittedGender, "permittedGender");
		if (rank < 0) {
			throw new IllegalArgumentException("rank must be non-negative");
		}
		if (maxCountLimitedQuest <= 0 || countRecoverLimitedQuest <= 0) {
			throw new IllegalArgumentException("limited quest counts must be positive");
		}
		if (useClassReward < 0 || useClassReward > 2) {
			throw new IllegalArgumentException("useClassReward must be 0, 1, or 2");
		}
		if (combineSkill != null && combineSkill < 0) {
			throw new IllegalArgumentException("combineSkill must be non-negative");
		}
		if (combineSkillPoint != null && combineSkillPoint < 0) {
			throw new IllegalArgumentException("combineSkillPoint must be non-negative");
		}
		repeatCycles = copySet(repeatCycles, "repeatCycles");
		if (npcFactionId < 0 || titleId < 0) {
			throw new IllegalArgumentException("npcFactionId and titleId must be non-negative");
		}
		mentorType = requireText(mentorType, "mentorType");
		targetType = requireText(targetType, "targetType");
		inventoryItems = List.copyOf(Objects.requireNonNull(inventoryItems, "inventoryItems"));
		questWorkItems = List.copyOf(Objects.requireNonNull(questWorkItems, "questWorkItems"));
		extendedRewards = List.copyOf(Objects.requireNonNull(extendedRewards, "extendedRewards"));
		bonuses = List.copyOf(Objects.requireNonNull(bonuses, "bonuses"));
		kills = List.copyOf(Objects.requireNonNull(kills, "kills"));
		startConditions = List.copyOf(Objects.requireNonNull(startConditions, "startConditions"));
		classRewards = copyClassRewards(classRewards);
	}

	public static QuestMetadata minimal(String name, int displayNameId, String category) {
		return new QuestMetadata(name, displayNameId, 0, Integer.MAX_VALUE, Set.of(), category,
			RepeatPolicy.once(), Set.of(), List.of(), List.of(), List.of());
	}

	private static String requireText(String value, String field) {
		Objects.requireNonNull(value, field);
		if (value.isBlank()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return value;
	}

	private static String normalizeText(String value, String field) {
		if (value == null) {
			return "";
		}
		return value.isBlank() ? "" : value;
	}

	private static <T> Set<T> copySet(Set<T> value, String field) {
		return Set.copyOf(Objects.requireNonNull(value, field));
	}

	private static Map<String, List<QuestReward>> copyClassRewards(Map<String, List<QuestReward>> value) {
		Objects.requireNonNull(value, "classRewards");
		Map<String, List<QuestReward>> copy = new java.util.LinkedHashMap<>();
		value.forEach((key, rewards) -> {
			String className = requireText(key, "classRewards key");
			copy.put(className, List.copyOf(Objects.requireNonNull(rewards, "classRewards value")));
		});
		return Collections.unmodifiableMap(copy);
	}
}
