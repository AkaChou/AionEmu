package com.aionemu.gameserver.model.gameobjects.player;

import java.util.HashSet;
import java.util.Set;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerRecipesDAO;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.recipe.RecipeTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_LEARN_RECIPE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_RECIPE_DELETE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 配方列表。
 * Recipe List game object.
 *
 * @author MrPoke
 */
public class RecipeList {

	private Set<Integer> recipeList = new HashSet<Integer>();

	public RecipeList(HashSet<Integer> recipeList) {
		this.recipeList = recipeList;
	}

	public RecipeList() {
	}

	/** 获取配方列表。 / Returns the recipe list. */
	public Set<Integer> getRecipeList() {
		return recipeList;
	}

	/** 添加配方。 / Adds recipe. */
	public void addRecipe(Player player, RecipeTemplate recipeTemplate) {
		int recipeId = recipeTemplate.getId();
		if (!player.getRecipeList().isRecipePresent(recipeId)) {
			if (DAOManager.getDAO(PlayerRecipesDAO.class).addRecipe(player.getObjectId(), recipeId)) {
				recipeList.add(recipeId);
				PacketSendUtility.sendPacket(player, new SM_LEARN_RECIPE(recipeId));
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_CRAFT_RECIPE_LEARN(recipeId, player.getName()));
			}
		}
	}

	/** 添加配方。 / Adds recipe. */
	public void addRecipe(int playerId, int recipeId) {
		if (DAOManager.getDAO(PlayerRecipesDAO.class).addRecipe(playerId, recipeId)) {
			recipeList.add(recipeId);
		}
	}

	/** 删除配方。 / Deletes recipe. */
	public void deleteRecipe(Player player, int recipeId) {
		if (recipeList.contains(recipeId)) {
			if (DAOManager.getDAO(PlayerRecipesDAO.class).delRecipe(player.getObjectId(), recipeId)) {
				recipeList.remove(recipeId);
				PacketSendUtility.sendPacket(player, new SM_RECIPE_DELETE(recipeId));
			}
		}
	}

	/** Auto Learn Recipe / Auto Learn Recipe */
	public void autoLearnRecipe(Player player, int skillId, int skillLvl) {
		for (RecipeTemplate recipe : DataManager.RECIPE_DATA.getAutolearnRecipes(player.getRace(), skillId, skillLvl)) {
			player.getRecipeList().addRecipe(player, recipe);
		}
	}

	/**
	 * @param recipeId Whether recipe present / Whether recipe present
	 */
	public boolean isRecipePresent(int recipeId) {
		return recipeList.contains(recipeId);
	}

	/** 大小 / size. */
	public int size() {
		return this.recipeList.size();
	}
}
