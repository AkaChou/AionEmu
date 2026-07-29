package com.aionemu.gameserver.questEngine.graph;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.aionemu.gameserver.dataholders.RecipeData;
import com.aionemu.gameserver.model.templates.recipe.RecipeTemplate;

/** 从正式配方静态数据构造 recipe 引用闭包。 / Builds recipe reference closure from formal recipe static data. */
public final class QuestGraphRecipeReferenceCatalog {

	/** 禁止实例化纯静态目录构造器。 / Prevents instantiation of this static catalog builder. */
	private QuestGraphRecipeReferenceCatalog() {
	}

	/** 构造全部正数配方模板 ID 的不可变引用集合。 / Builds an immutable reference set of all positive recipe-template ids. */
	public static Set<Integer> build(RecipeData recipes) {
		Objects.requireNonNull(recipes, "recipes");
		Set<Integer> recipeIds = new LinkedHashSet<>();
		for (RecipeTemplate recipe : Objects.requireNonNull(recipes.getRecipeTemplates(), "recipe templates").values()) {
			if (recipe == null || recipe.getId() <= 0) {
				throw new IllegalArgumentException("Recipe reference is invalid");
			}
			recipeIds.add(recipe.getId());
		}
		return Set.copyOf(recipeIds);
	}
}
