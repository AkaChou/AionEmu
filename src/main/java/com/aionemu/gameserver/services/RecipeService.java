package com.aionemu.gameserver.services;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.recipe.RecipeTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 配方服务，校验并学习制造/制作配方。
 * Recipe service that validates and learns crafting recipes.
 */
public class RecipeService {

	/**
	 * 校验玩家是否可学习指定配方。
	 * Validates whether the player can learn the given recipe.
	 *
	 * 玩家 / player
	 * recipe id
	 *
	 * @return 合法时返回配方模板，否则返回 null / recipe template if valid, otherwise null
	 */
	public static RecipeTemplate validateNewRecipe(Player player, int recipeId) {
		if (player.getRecipeList().size() >= 1600) {
			PacketSendUtility.sendMessage(player, "You are unable to have more than 1600 recipes at the same time.");
			return null;
		}
		RecipeTemplate template = DataManager.RECIPE_DATA.getRecipeTemplateById(recipeId);
		if (template == null) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_RECIPEITEM_CANT_USE_NO_RECIPE);
			return null;
		}
		if (template.getRace() != Race.PC_ALL) {
			if (template.getRace() != player.getRace()) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CRAFTRECIPE_RACE_CHECK);
				return null;
			}
		}
		if (player.getRecipeList().isRecipePresent(recipeId)) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CRAFT_RECIPE_LEARNED_ALREADY);
			return null;
		}
		if (!player.getSkillList().isSkillPresent(template.getSkillid())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CRAFT_RECIPE_CANT_LEARN_SKILL(
					DataManager.SKILL_DATA.getSkillTemplate(template.getSkillid()).getNameId()));
			return null;
		}
		if (template.getSkillpoint() > player.getSkillList().getSkillLevel(template.getSkillid())) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CRAFT_RECIPE_CANT_LEARN_SKILLPOINT);
			return null;
		}
		return template;
	}

	/**
	 * 为玩家添加配方，可选择是否先做校验。
	 * Adds a recipe for the player, optionally with validation.
	 *
	 * 玩家 / player
	 * recipe id
	 * @param useValidation 是否执行校验 / whether to run validation
	 * @return 添加成功返回 true / true if the recipe was added
	 */
	public static boolean addRecipe(Player player, int recipeId, boolean useValidation) {
		RecipeTemplate template = null;
		if (useValidation) {
			template = validateNewRecipe(player, recipeId);
		} else {
			template = DataManager.RECIPE_DATA.getRecipeTemplateById(recipeId);
		}
		if (template == null) {
			return false;
		}
		player.getRecipeList().addRecipe(player, template);
		return true;
	}
}
