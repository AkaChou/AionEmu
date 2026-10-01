package com.aionemu.gameserver.questEngine.tablelane;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.recipe.RecipeTemplate;

/**
 * 原生任务车道的**配方端口**：真端表声明的配方学习（CombineTask 接取）与忘记（完成 / 放弃）的
 * 唯一出口，处理器本身不直接触碰配方表或 DAO。
 * <p>
 * 真端 CombineTask helper（{@code FUN_180caac10}）在接取段写配方、在完成/放弃段清配方；本端口把
 * 这两个调用面收敛到与 typed 车道同一条实现（{@code RecipeList}：DB + {@code SM_LEARN_RECIPE} /
 * {@code SM_RECIPE_DELETE} 一并下发），避免 native 车道另造一套。
 * <p>
 * The recipe port of the native quest lane: the single exit for the retail-declared recipe learn
 * (CombineTask accept) and forget (completion / abandon), backed by the same {@code RecipeList}
 * implementation the typed lane uses (DB write plus the learn/delete packets).
 */
public interface NativeRecipePort {

	/** 玩家是否已掌握该配方。 / Whether the player already knows the recipe. */
	boolean holds(Player player, int recipeId);

	/**
	 * 学习配方（已掌握时为幂等成功；配方 id 在真端表里不存在则 fail-closed 返回 false）。
	 * Learns the recipe (idempotent when already known; an unknown retail recipe id fails closed).
	 */
	boolean learn(Player player, int recipeId);

	/** 忘记配方（未掌握时为幂等成功）。 / Forgets the recipe (idempotent when it is not known). */
	boolean forget(Player player, int recipeId);

	/** 生产实现（在线玩家配方表 + 配方静态数据）。 / The live implementation. */
	static NativeRecipePort live() {
		return RecipeListRecipePort.INSTANCE;
	}
}

/** 生产实现：{@code RecipeList}（DB + 学/删包）与配方静态数据。 / Live RecipeList-backed implementation. */
final class RecipeListRecipePort implements NativeRecipePort {

	static final RecipeListRecipePort INSTANCE = new RecipeListRecipePort();

	private RecipeListRecipePort() {
	}

	@Override
	public boolean holds(Player player, int recipeId) {
		return player != null && player.getRecipeList() != null
			&& player.getRecipeList().isRecipePresent(recipeId);
	}

	@Override
	public boolean learn(Player player, int recipeId) {
		if (player == null || player.getRecipeList() == null || recipeId <= 0) {
			return false;
		}
		if (player.getRecipeList().isRecipePresent(recipeId)) {
			return true;
		}
		RecipeTemplate template = DataManager.RECIPE_DATA.getRecipeTemplateById(recipeId);
		if (template == null) {
			// 真端表声明的配方 id 在静态数据里不存在 ⇒ 不落库、不假装学会。 /
			// A retail-declared recipe id missing from the static data is never faked.
			return false;
		}
		player.getRecipeList().addRecipe(player, template);
		return player.getRecipeList().isRecipePresent(recipeId);
	}

	@Override
	public boolean forget(Player player, int recipeId) {
		if (player == null || player.getRecipeList() == null || recipeId <= 0) {
			return false;
		}
		player.getRecipeList().deleteRecipe(player, recipeId);
		return !player.getRecipeList().isRecipePresent(recipeId);
	}
}
