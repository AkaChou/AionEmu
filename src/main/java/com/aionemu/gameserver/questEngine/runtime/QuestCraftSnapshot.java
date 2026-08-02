package com.aionemu.gameserver.questEngine.runtime;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Immutable recipe and crafting-skill facts used by pure quest condition evaluation. */
public record QuestCraftSnapshot(Set<Integer> knownRecipes, Map<Integer, Integer> craftingSkillLevels,
		int maxRecipes, int maxExpertSkills, int maxMasterSkills) {
	private static final Set<Integer> PROMOTION_SLOT_SKILLS = Set.of(
		40001, 40002, 40003, 40004, 40007, 40008, 40010);
	public QuestCraftSnapshot {
		knownRecipes = Set.copyOf(Objects.requireNonNull(knownRecipes, "knownRecipes"));
		craftingSkillLevels = Map.copyOf(Objects.requireNonNull(craftingSkillLevels, "craftingSkillLevels"));
		if (knownRecipes.stream().anyMatch(id -> id == null || id <= 0)) {
			throw new IllegalArgumentException("known recipe ids must be positive");
		}
		if (craftingSkillLevels.entrySet().stream().anyMatch(entry -> entry.getKey() == null
				|| entry.getKey() <= 0 || entry.getValue() == null || entry.getValue() < 0)) {
			throw new IllegalArgumentException("crafting skill facts are invalid");
		}
		if (maxRecipes <= 0 || maxExpertSkills < 0 || maxMasterSkills < 0) {
			throw new IllegalArgumentException("craft limits are invalid");
		}
	}

	public boolean recipeKnown(int recipeId) {
		return knownRecipes.contains(recipeId);
	}

	/**
	 * Mirrors the current expert/master slot rules. Replaying a grant that already converged is always eligible.
	 */
	public boolean canGrantCraftSkill(int skillId, int targetLevel) {
		int currentLevel = craftingSkillLevels.getOrDefault(skillId, 0);
		if (currentLevel >= targetLevel) {
			return true;
		}
		if (targetLevel == 400) {
			long occupied = craftingSkillLevels.entrySet().stream()
				.filter(entry -> PROMOTION_SLOT_SKILLS.contains(entry.getKey()) && entry.getValue() >= 400).count();
			return occupied < maxExpertSkills;
		}
		if (targetLevel == 500) {
			long occupied = craftingSkillLevels.entrySet().stream()
				.filter(entry -> PROMOTION_SLOT_SKILLS.contains(entry.getKey()) && entry.getValue() >= 500).count();
			return occupied < maxMasterSkills;
		}
		return true;
	}
}
