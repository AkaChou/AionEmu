package com.aionemu.gameserver.questEngine.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.aionemu.gameserver.dataholders.RecipeData;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.templates.recipe.RecipeTemplate;

/**
 * 从正式 recipe 静态数据构造制作技能及自动学习配方引用闭包。
 * Builds craft-skill and auto-learn recipe reference closure from formal recipe static data.
 */
public final class QuestGraphCraftSkillReferenceCatalog {

	private final Set<Integer> craftSkillIds;
	private final List<RecipeReference> recipes;

	/**
	 * 创建经过全量校验且顺序稳定的制作引用目录。
	 * Creates a fully validated craft reference catalog with deterministic ordering.
	 */
	private QuestGraphCraftSkillReferenceCatalog(Set<Integer> craftSkillIds, List<RecipeReference> recipes) {
		this.craftSkillIds = Collections.unmodifiableSet(new LinkedHashSet<>(craftSkillIds));
		this.recipes = List.copyOf(recipes);
	}

	/**
	 * 校验全部正式 recipe，并构造不可变制作技能引用目录。
	 * Validates every formal recipe and builds an immutable craft-skill reference catalog.
	 *
	 * @param recipeData 正式配方静态数据 / formal recipe static data
	 * @return 制作技能引用目录 / craft-skill reference catalog
	 */
	public static QuestGraphCraftSkillReferenceCatalog build(RecipeData recipeData) {
		Objects.requireNonNull(recipeData, "recipe data");
		List<RecipeReference> recipes = new ArrayList<>();
		Set<Integer> craftSkillIds = new LinkedHashSet<>();
		for (RecipeTemplate recipe : Objects.requireNonNull(recipeData.getRecipeTemplates(), "recipe templates").values()) {
			if (recipe == null || recipe.getId() == null || recipe.getId() <= 0 || recipe.getSkillid() == null
					|| recipe.getSkillid() <= 0 || recipe.getSkillpoint() == null || recipe.getSkillpoint() < 0
					|| !isSupportedRecipeRace(recipe.getRace())) {
				throw new IllegalArgumentException("Craft recipe reference is invalid");
			}
			craftSkillIds.add(recipe.getSkillid());
			recipes.add(new RecipeReference(recipe.getId(), recipe.getSkillid(), recipe.getSkillpoint(), recipe.getRace(),
				recipe.getAutoLearn() != 0));
		}
		if (recipes.isEmpty() || craftSkillIds.isEmpty()) {
			throw new IllegalArgumentException("Craft recipe references are empty");
		}
		recipes.sort(Comparator.comparingInt(RecipeReference::recipeId));
		Set<Integer> sortedCraftSkillIds = new LinkedHashSet<>();
		craftSkillIds.stream().sorted().forEach(sortedCraftSkillIds::add);
		return new QuestGraphCraftSkillReferenceCatalog(sortedCraftSkillIds, recipes);
	}

	/**
	 * 返回全部被正式 recipe 数据引用的制作技能 ID。
	 * Returns every craft-skill id referenced by formal recipe data.
	 *
	 * @return 不可变制作技能 ID 集合 / immutable craft-skill id set
	 */
	public Set<Integer> craftSkillIds() {
		return craftSkillIds;
	}

	/**
	 * 返回指定玩家阵营、制作技能和目标等级可自动学习的稳定 recipe ID 列表。
	 * Returns stable auto-learn recipe ids for a player race, craft skill, and target level.
	 *
	 * @param race 玩家阵营 / player race
	 * @param craftSkillId 制作技能 ID / craft-skill id
	 * @param targetLevel 目标技能等级 / target skill level
	 * @return 按 recipe ID 升序排列的不可变列表 / immutable recipe-id list sorted ascending
	 */
	public List<Integer> autolearnRecipeIds(Race race, int craftSkillId, int targetLevel) {
		if (race != Race.ELYOS && race != Race.ASMODIANS || craftSkillId <= 0 || targetLevel <= 0
				|| !craftSkillIds.contains(craftSkillId)) {
			throw new IllegalArgumentException("Craft auto-learn reference query is invalid");
		}
		return recipes.stream()
			.filter(RecipeReference::autoLearn)
			.filter(recipe -> recipe.craftSkillId() == craftSkillId && recipe.skillPoint() <= targetLevel)
			.filter(recipe -> recipe.race() == Race.PC_ALL || recipe.race() == race)
			.map(RecipeReference::recipeId)
			.toList();
	}

	/**
	 * 判断 recipe 阵营是否属于正式玩家配方域。
	 * Returns whether a recipe race belongs to the formal player-recipe domain.
	 */
	private static boolean isSupportedRecipeRace(Race race) {
		return race == Race.ELYOS || race == Race.ASMODIANS || race == Race.PC_ALL;
	}

	/**
	 * 保存编译期所需的不可变 recipe 引用字段。
	 * Holds immutable recipe-reference fields required during compilation.
	 */
	private record RecipeReference(int recipeId, int craftSkillId, int skillPoint, Race race, boolean autoLearn) {
	}
}
