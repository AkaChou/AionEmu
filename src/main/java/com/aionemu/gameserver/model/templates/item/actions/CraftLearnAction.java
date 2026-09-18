package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.recipe.RecipeTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 制作 Learn 动作模板（静态数据/XML）。
 * XML template.
 *
 * @author ATracer, MrPoke, KID
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "CraftLearnAction")
public class CraftLearnAction extends AbstractItemAction {
	@XmlAttribute
	protected int recipeid;

	/** 执行 / act. */
	@Override
	public void act(Player player, Item parentItem, Item targetItem) {
		player.getController().cancelUseItem();
		if (player.getInventory().decreaseByObjectId(parentItem.getObjectId(), 1)) {
			if (addRecipe(player, recipeid, false)) {
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_USE_ITEM(new DescriptionId(parentItem.getItemTemplate().getNameId())));
				PacketSendUtility.sendPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						parentItem.getObjectId(), parentItem.getItemTemplate().getTemplateId()));
			}
		}
	}

	/**
	 * @return 是否允许执行。 / Whether act
	  */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		return validateNewRecipe(player, recipeid) != null;
	}

	/** 返回配方 ID / Returns the recipe id */
	public int getRecipeId() {
		return recipeid;
	}

	/**
	 * 校验玩家是否可学习指定配方。
	 * Validates whether the player can learn the given recipe.
	 *
	 * @param player 玩家 / player
	 * @param recipeId 配方 ID / recipe id
	 * @return 合法时返回配方模板，否则返回 null / recipe template if valid, otherwise null
	 */
	private static RecipeTemplate validateNewRecipe(Player player, int recipeId) {
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
	 * @param player 玩家 / player
	 * @param recipeId 配方 ID / recipe id
	 * @param useValidation 是否执行校验 / whether to run validation
	 * @return 添加成功返回 true / true if the recipe was added
	 */
	private static boolean addRecipe(Player player, int recipeId, boolean useValidation) {
		RecipeTemplate template = useValidation ? validateNewRecipe(player, recipeId)
				: DataManager.RECIPE_DATA.getRecipeTemplateById(recipeId);
		if (template == null) {
			return false;
		}
		player.getRecipeList().addRecipe(player, template);
		return true;
	}
}
