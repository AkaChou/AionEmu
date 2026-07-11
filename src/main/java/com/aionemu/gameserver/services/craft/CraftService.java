package com.aionemu.gameserver.services.craft;

import lombok.extern.slf4j.Slf4j;
import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.StaticObject;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.recipe.Component;
import com.aionemu.gameserver.model.templates.recipe.ComponentElement;
import com.aionemu.gameserver.model.templates.recipe.RecipeTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_AETHERFORGING_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.questEngine.QuestEngine;
import com.aionemu.gameserver.questEngine.model.QuestEnv;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.services.item.ItemService.ItemUpdatePredicate;
import com.aionemu.gameserver.skillengine.task.CraftingTask;
import com.aionemu.gameserver.skillengine.task.MorphingTask;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 制作服务，处理玩家配方制作、材料消耗、经验结算与以太锻造流程。
 * Craft service handling player recipe crafting, material consumption, experience settlement and aetherforging.
 */
@Slf4j(topic = "CRAFT_LOG")
public class CraftService {

	/**
	 * 完成制作：发放产物、结算经验，并处理限次配方与冷却。
	 * Finish crafting: grant product, settle experience, handle limited recipes and cooldown.
	 *
	 * 玩家 / Player
	 * Recipe template
	 * @param critCount 暴击次数（决定连击产物） / Crit count (selects combo product)
	 * @param bonus 经验加成百分比 / Experience bonus percent
	 */
	public static void finishCrafting(final Player player, RecipeTemplate recipetemplate, int critCount, int bonus) {
		if (recipetemplate.getMaxProductionCount() != null) {
			player.getRecipeList().deleteRecipe(player, recipetemplate.getId());
			if (critCount == 0) {
				GameEngineServices.questEngine().onFailCraft(new QuestEnv(null, player, 0, 0), recipetemplate.getComboProduct(1) == null ? 0 : recipetemplate.getComboProduct(1));
			}
		}
		int xpReward = (int) ((0.008 * (recipetemplate.getSkillpoint() + 100) * (recipetemplate.getSkillpoint() + 100) + 60));
		xpReward = xpReward + (xpReward * bonus / 100);
		int productItemId = critCount > 0 ? recipetemplate.getComboProduct(critCount) : recipetemplate.getProductid();
		ItemService.addItem(player, productItemId, recipetemplate.getQuantity(), new ItemUpdatePredicate() {
			@Override
			public boolean changeItem(Item item) {
				if (item.getItemTemplate().isWeapon() || item.getItemTemplate().isArmor()) {
					item.setItemCreator(player.getName());
				}
				/**
				 * 仅高阶守护者：由符文部落物品幻化获得的物品。
				 * High Daeva-only items obtainable by morphing Rune Tribe items. Items from the
				 * Rune Tribe Set that can be purchased with Ancient Coins, can be Morphed into
				 * stronger versions. Morph product will already be +5.
				 * http://www.aionpowerbook.com/powerbook/Rune_Hero%27s_Set
				 */
				// 关联制作“变形物质”= skillId: 40009 / To do linked craft "Morphing Substance" = skillId: "40009"
				// 仅此制作可将高阶守护者物品强化 +5 / Only this craft can enchant item archdaeva + 5
				if (item.isArchDaevaItem()) {
					item.setEnchantLevel(item.getEnchantLevel() + 5);
					item.setPersistentState(PersistentState.UPDATE_REQUIRED);
					PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, item));
				}
				return true;
			}
		});
		ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(productItemId);
		int gainedCraftExp = (int) RewardType.CRAFTING.calcReward(player, xpReward);
		int skillId = recipetemplate.getSkillid();
		if ((skillId == 40001) || (skillId == 40002) || (skillId == 40003) || (skillId == 40004) || (skillId == 40007) || (skillId == 40008) || (skillId == 40010)) {
			if ((player.getSkillList().getSkillLevel(skillId) >= 500) && (recipetemplate.getSkillpoint() < 465)) {
				// 如此基础的制作不影响技能等级，大师。 / Such basic crafting doesn't affect your skill level, Master.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DONT_GET_COMBINE_EXP_GRAND_MASTER);
			} else if ((player.getSkillList().getSkillLevel(skillId) >= 400) && (recipetemplate.getSkillpoint() < 365)) {
				// 你已是专家，低等级制作不再提升技能等级。 / Your skill level does not increase with low level crafting as you are an Expert.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DONT_GET_COMBINE_EXP);
			} else {
				if (player.getSkillList().addSkillXp(player, recipetemplate.getSkillid(), gainedCraftExp, recipetemplate.getSkillpoint())) {
					player.getCommonData().addExp(xpReward, RewardType.CRAFTING);
				} else {
					// %0 技能难度过低，等级不再提升。 / The skill level for the %0 skill does not increase as the difficulty is too low.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DONT_GET_PRODUCTION_EXP(new DescriptionId(DataManager.SKILL_DATA.getSkillTemplate(recipetemplate.getSkillid()).getNameId())));
				}
			}
		}
		if (recipetemplate.getCraftDelayId() != null) {
			player.getCraftCooldownList().addCraftCooldown(recipetemplate.getCraftDelayId(), recipetemplate.getCraftDelayTime());
		}
	}

	/**
	 * 开始制作（默认制作数量为 1）。
	 * Start crafting (default craft count is 1).
	 *
	 * 玩家 / Player
	 * Recipe id
	 * @param targetObjId 目标工作台对象 ID / Target workbench object id
	 * Craft type
	 */
	public static void startCrafting(Player player, int recipeId, int targetObjId, int craftType) {
		startCrafting(player, recipeId, targetObjId, craftType, 1);
	}

	/**
	 * 开始制作流程，创建对应的制作/变形任务。
	 * Start the crafting process and create the matching crafting/morphing task.
	 *
	 * 玩家 / Player
	 * Recipe id
	 * @param targetObjId 目标工作台对象 ID / Target workbench object id
	 * Craft type
	 * Craft count
	 */
	public static void startCrafting(Player player, int recipeId, int targetObjId, int craftType, int craftCount) {
		RecipeTemplate recipeTemplate = DataManager.RECIPE_DATA.getRecipeTemplateById(recipeId);
		int skillId = recipeTemplate.getSkillid();
		VisibleObject target = player.getKnownList().getObject(targetObjId);
		ItemTemplate itemTemplate = DataManager.ITEM_DATA.getItemTemplate(recipeTemplate.getProductid());
		if (recipeTemplate.getDp() != null) {
			player.getCommonData().addDp(-recipeTemplate.getDp());
		}
		if (skillId == 40009) {
			player.setCraftingTask(new MorphingTask(player, (StaticObject) target, recipeTemplate, craftCount));
		} else {
			int skillLvlDiff = player.getSkillList().getSkillLevel(skillId) - recipeTemplate.getSkillpoint();
			player.setCraftingTask(new CraftingTask(player, (StaticObject) target, recipeTemplate, skillLvlDiff, craftType == 1 ? 15 : 0, craftCount));
		}
		player.getCraftingTask().start();
	}

	/**
	 * 停止以太锻造（构造中断观察器，用于打断流程）。
	 * Stop aetherforging (build abort observer used to interrupt the process).
	 *
	 * 玩家 / Player
	 * Recipe id
	 */
	public static void stopAetherforging(final Player player, int recipeId) {
		final RecipeTemplate recipeTemplate = DataManager.RECIPE_DATA.getRecipeTemplateById(recipeId);
		final ItemUseObserver observer = new ItemUseObserver() {
			@Override
			public void abort() {
				player.getController().cancelTask(TaskId.ITEM_USE);
				player.getObserveController().removeObserver(this);
				PacketSendUtility.broadcastPacket(player, new SM_AETHERFORGING_ANIMATION(player, recipeTemplate.getId(), 0, 1), true);
			}
		};
	}

	/**
	 * 开始以太锻造（默认制作数量为 1）。
	 * Start aetherforging (default craft count is 1).
	 *
	 * 玩家 / Player
	 * Recipe id
	 * Craft type
	 */
	public static void startAetherforging(final Player player, int recipeId, int craftType) {
		startAetherforging(player, recipeId, craftType, 1);
	}

	/**
	 * 开始以太锻造：播放动画、延迟结算产物与经验。
	 * Start aetherforging: play animation, then settle product and experience after delay.
	 *
	 * 玩家 / Player
	 * Recipe id
	 * Craft type
	 * Craft count
	 */
	public static void startAetherforging(final Player player, int recipeId, int craftType, int craftCount) {
		final RecipeTemplate recipeTemplate = DataManager.RECIPE_DATA.getRecipeTemplateById(recipeId);
		final int productCount = Math.max(1, craftCount);
		PacketSendUtility.broadcastPacket(player, new SM_AETHERFORGING_ANIMATION(player, recipeTemplate.getId(), 3000, 0), true);
		final ItemUseObserver observer = new ItemUseObserver() {
			@Override
			public void abort() {
				player.getController().cancelTask(TaskId.ITEM_USE);
				player.getObserveController().removeObserver(this);
				PacketSendUtility.broadcastPacket(player, new SM_AETHERFORGING_ANIMATION(player, recipeTemplate.getId(), 0, 1), true);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				int xpReward = (int) ((2 * (recipeTemplate.getSkillpoint() + 100) * (recipeTemplate.getSkillpoint() + 100) + 60));
				ItemService.addItem(player, recipeTemplate.getProductid(), (long) recipeTemplate.getQuantity() * productCount, new ItemUpdatePredicate(ItemAddType.AETHERFORGING, ItemUpdateType.INC_ITEM_COLLECT));
				if (Rnd.get(1, 10) == 10 && player.getSkillList().getSkillLevel(40011) != 300) {
					player.getObserveController().removeObserver(observer);
					player.getCommonData().addExp(xpReward, RewardType.CRAFTING);
					player.getSkillList().addSkill(player, 40011, player.getSkillList().getSkillLevel(40011) + 1);
				}
				player.getObserveController().removeObserver(observer);
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_CRAFT_SUCCESS_GETEXP);
				PacketSendUtility.sendPacket(player, new SM_AETHERFORGING_ANIMATION(player, recipeTemplate.getId(), 0, 2));
			}
		}, 3000));
	}

	/**
	 * 按配方校验并扣除指定材料（单次）。
	 * Validate recipe and consume the specified material once.
	 *
	 * 玩家 / Player
	 * Recipe id
	 * Material item id
	 * @param materialsCount 材料数量参数（保留） / Material count parameter (reserved)
	 */
	public static void checkComponents(Player player, int recipeId, int itemId, int materialsCount) {
		RecipeTemplate recipeTemplate = DataManager.RECIPE_DATA.getRecipeTemplateById(recipeId);
		if (recipeTemplate.getComponent() != null) {
			for (Component a : recipeTemplate.getComponent()) {
				for (ComponentElement b : a.getComponents()) {
					if (b.getItemid().equals(itemId)) {
						player.getInventory().decreaseByItemId(itemId, b.getQuantity());
						return;
					}
				}
			}
		}
	}

	/**
	 * 按请求数量校验并扣除材料，返回可制作次数。
	 * Validate and consume materials by requested quantity; return craftable count.
	 *
	 * 玩家 / Player
	 * Recipe id
	 * Material item id
	 * Requested quantity
	 *
	 * @return 可制作次数，失败返回 0 / Craftable count, or 0 on failure
	 */
	public static int checkComponents(Player player, int recipeId, int itemId, long requestedCount) {
		RecipeTemplate recipeTemplate = DataManager.RECIPE_DATA.getRecipeTemplateById(recipeId);
		if (recipeTemplate.getComponent() != null) {
			for (Component a : recipeTemplate.getComponent()) {
				for (ComponentElement b : a.getComponents()) {
					if (b.getItemid().equals(itemId)) {
						int craftCount = getCraftCount(b.getQuantity(), requestedCount);
						if (craftCount > 0 && player.getInventory().decreaseByItemId(itemId, (long) b.getQuantity() * craftCount)) {
							return craftCount;
						}
						return 0;
					}
				}
			}
		}
		return 0;
	}

	/**
	 * 根据单次所需与请求总量计算制作次数。
	 * Compute craft count from required-per-craft and requested total quantity.
	 *
	 * @param requiredQuantity 单次所需数量 / Quantity required per craft
	 * Requested total quantity
	 * Craft count
	 */
	static int getCraftCount(int requiredQuantity, long requestedQuantity) {
		if (requiredQuantity < 1 || requestedQuantity < requiredQuantity) {
			return 0;
		}
		long count = requestedQuantity / requiredQuantity;
		return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
	}

	/**
	 * 按技能 ID 返回对应的加成需求物品 ID。
	 * Return the bonus-required item id for the given craft skill id.
	 *
	 * Craft skill id
	 *
	 * @param skillId @return 加成物品 ID，未匹配返回 0 / Bonus item id, or 0 if unmatched
	 */
	private static int getBonusReqItem(int skillId) {
		switch (skillId) {
		case 40001: // Cooking.
			return 169401081;
		case 40002: // Weaponsmithing.
			return 169401076;
		case 40003: // Armorsmithing.
			return 169401077;
		case 40004: // Tailoring.
			return 169401078;
		case 40007: // Alchemy.
			return 169401080;
		case 40008: // Handicrafting.
			return 169401079;
		case 40010: // Menuisier.
			return 169401082;
		}
		return 0;
	}
}
