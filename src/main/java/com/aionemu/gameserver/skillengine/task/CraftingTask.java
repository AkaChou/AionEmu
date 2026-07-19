package com.aionemu.gameserver.skillengine.task;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.StaticObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.item.ItemCategory;
import com.aionemu.gameserver.model.templates.item.ItemQuality;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.recipe.RecipeTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CRAFT_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_CRAFT_UPDATE;
import com.aionemu.gameserver.services.craft.CraftService;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 制作任务：按配方推进成功/失败进度，处理连击暴击与批量制作。
 * Crafting task: advances success/failure for a recipe, handling combo crits and multi-craft.
 */
public class CraftingTask extends AbstractCraftTask {

	/**
	 * 配方模板。
	 * Recipe template.
	 */
	protected RecipeTemplate recipeTemplate;

	/**
	 * 基础产物物品模板。
	 * Base product item template.
	 */
	protected ItemTemplate itemTemplate;

	/**
	 * 实际产物物品模板（可能因连击暴击变化）。
	 * Actual product template (may change with combo crit).
	 */
	protected ItemTemplate itemTemplateReal;

	/**
	 * 当前连击暴击计数。
	 * Current combo-crit count.
	 */
	protected int critCount;

	/**
	 * 是否触发普通连击暴击。
	 * Whether a normal combo crit was rolled.
	 */
	protected boolean crit = false;

	/**
	 * 是否触发紫色连击暴击。
	 * Whether a purple combo crit was rolled.
	 */
	protected boolean purpleCrit = false;

	/**
	 * 最大连击暴击次数（配方 combo 产物数）。
	 * Max combo-crit count (recipe combo product size).
	 */
	protected int maxCritCount;

	/**
	 * 制作加成值。
	 * Crafting bonus value.
	 */
	private int bonus;

	/**
	 * 剩余制作次数。
	 * Remaining craft attempts.
	 */
	private int remainingCrafts;

	/**
	 * 构造单次制作任务。
	 * Creates a single-craft task.
	 *
	 * requesting player
	 * @param responder 制作台等静态对象 / craft station static object
	 * recipe template
	 * @param skillLvlDiff 技能等级差 / skill level difference
	 * @param bonus 制作加成 / craft bonus
	 */
	public CraftingTask(Player requestor, StaticObject responder, RecipeTemplate recipeTemplate, int skillLvlDiff, int bonus) {
		this(requestor, responder, recipeTemplate, skillLvlDiff, bonus, 1);
	}

	/**
	 * 构造可批量制作的任务。
	 * Creates a craft task with optional multi-craft count.
	 *
	 * requesting player
	 * @param responder 制作台等静态对象 / craft station static object
	 * recipe template
	 * @param skillLvlDiff 技能等级差 / skill level difference
	 * @param bonus 制作加成 / craft bonus
	 * craft attempt count
	 */
	public CraftingTask(Player requestor, StaticObject responder, RecipeTemplate recipeTemplate, int skillLvlDiff, int bonus, int craftCount) {
		super(requestor, responder, skillLvlDiff);
		this.recipeTemplate = recipeTemplate;
		this.maxCritCount = recipeTemplate.getComboProductSize();
		this.bonus = bonus;
		this.remainingCrafts = Math.max(1, craftCount);
	}

	/**
	 * 计算一次尝试后的剩余制作次数。
	 * Computes remaining craft attempts after one try.
	 *
	 * @param remainingCrafts 当前剩余次数 / current remaining count
	 * @return 尝试后剩余次数 / remaining after attempt
	 */
	static int getRemainingCraftsAfterAttempt(int remainingCrafts) {
		return Math.max(0, remainingCrafts - 1);
	}

	/**
	 * 按物品品质重置成功/失败进度上限。
	 * Resets success/failure caps based on item quality.
	 */
	private void craftSetup() {
		this.itemQuality = this.itemTemplateReal.getItemQuality();
		currentSuccessValue = 0;
		currentFailureValue = 0;
		maxSuccessValue = (int) Math.round((this.itemQuality.getQualityId() + 3) * 3.5) * 5;
		maxFailureValue = (int) Math.round((this.itemQuality.getQualityId() + 3) * 5.25) * 5;
	}

	/**
	 * 分析本 tick 暴击与进度增量。
	 * Analyzes this tick's crit and progress increments.
	 */
	@Override
	protected void analyzeInteraction() {
		int critVal = (int) (Rnd.get(55000) / (skillLvlDiff + 1));
		if (critVal < CraftConfig.CRAFT_CHANCE_BLUE_CRIT) {
			critType = CraftCritType.BLUE;
		} else if ((critVal < CraftConfig.CRAFT_CHANCE_INSTANT)
				&& (this.itemQuality.getQualityId() < ItemQuality.EPIC.getQualityId())) {
			critType = CraftCritType.INSTANT;
			currentSuccessValue = maxSuccessValue;
			return;
		}
		if (CraftConfig.CRAFT_CHECK_TASK) {
			if (this.task == null) {
				return;
			}
		}
		double mod = Math.sqrt((double) skillLvlDiff / 450f) * 100f + Rnd.nextGaussian() * 10f;
		mod -= (double) this.itemQuality.getQualityId() / 2;
		if (mod < 0) {
			currentFailureValue -= (int) mod;
		} else {
			currentSuccessValue += (int) mod;
		}
		if (currentSuccessValue >= maxSuccessValue) {
			currentSuccessValue = maxSuccessValue;
		} else if (currentFailureValue >= maxFailureValue) {
			currentFailureValue = maxFailureValue;
		}
	}

	/**
	 * 失败完成：发送失败更新与动画。
	 * Failure finish: sends failure update and animation.
	 */
	@Override
	protected void onFailureFinish() {
		PacketSendUtility.sendPacket(requestor, new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplate, currentSuccessValue, currentFailureValue, 6));
		PacketSendUtility.broadcastPacket(requestor, new SM_CRAFT_ANIMATION(requestor.getObjectId(), responder.getObjectId(), 0, 3), true);
	}

	/**
	 * 成功完成：处理连击暴击升级或结算产物。
	 * Success finish: handles combo-crit upgrades or final product settlement.
	 *
	 * @return true 表示整次任务结束 / true if the whole task ends
	 */
	@Override
	protected boolean onSuccessFinish() {
		if (this.checkCrit() && recipeTemplate.getComboProduct(critCount) != null) {
			if (purpleCrit) {
				critCount++;
			}
			craftSetup();
			PacketSendUtility.sendPacket(requestor, new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplateReal, maxSuccessValue, maxFailureValue, 3));
			return false;
		} else {
			if (critCount > 0 && this.checkCrit()) {
				PacketSendUtility.broadcastPacket(requestor, new SM_CRAFT_ANIMATION(requestor.getObjectId(), responder.getObjectId(), 0, 2), true);
				PacketSendUtility.sendPacket(requestor, new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplateReal, currentSuccessValue, currentFailureValue, 5));
				if (!CraftService.finishCrafting(requestor, recipeTemplate, critCount, bonus)) {
					return true;
				}
				return finishCraftAttempt();
			}
			PacketSendUtility.broadcastPacket(requestor, new SM_CRAFT_ANIMATION(requestor.getObjectId(), responder.getObjectId(), 0, 2), true);
			PacketSendUtility.sendPacket(requestor, new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplateReal, currentSuccessValue, currentFailureValue, 5));
			if (!CraftService.finishCrafting(requestor, recipeTemplate, critCount, bonus)) {
				return true;
			}
			return finishCraftAttempt();
		}
	}

	/**
	 * 完成一次制作尝试：递减剩余次数，必要时开始下一次。
	 * Finishes one craft attempt: decrements remaining count or starts the next craft.
	 *
	 * @return true 表示全部次数完成 / true if all attempts are done
	 */
	protected boolean finishCraftAttempt() {
		remainingCrafts = getRemainingCraftsAfterAttempt(remainingCrafts);
		if (remainingCrafts == 0) {
			return true;
		}
		startNextCraft();
		return false;
	}

	/**
	 * 开始下一次批量制作。
	 * Starts the next multi-craft attempt.
	 */
	protected void startNextCraft() {
		critCount = 0;
		crit = false;
		purpleCrit = false;
		critType = CraftCritType.NONE;
		itemTemplateReal = itemTemplate;
		craftSetup();
		setupCrit();
		PacketSendUtility.sendPacket(requestor, new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplate, maxSuccessValue, maxFailureValue, 0));
		PacketSendUtility.broadcastPacket(requestor, new SM_CRAFT_ANIMATION(requestor.getObjectId(), responder.getObjectId(), recipeTemplate.getSkillid(), 0), true);
		PacketSendUtility.broadcastPacket(requestor, new SM_CRAFT_ANIMATION(requestor.getObjectId(), responder.getObjectId(), recipeTemplate.getSkillid(), 1), true);
	}

	/**
	 * 向客户端发送制作进度更新。
	 * Sends craft progress update to the client.
	 */
	@Override
	protected void sendInteractionUpdate() {
		PacketSendUtility.sendPacket(requestor, new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplate, currentSuccessValue, currentFailureValue, this.critType.getPacketId()));
		if (this.critType == CraftCritType.PURPLE) {
			this.critType = CraftCritType.NONE;
		}
	}

	/**
	 * 中止制作：发包并清理玩家制作任务。
	 * Aborts crafting: packets and clears the player's craft task.
	 */
	@Override
	protected void onInteractionAbort() {
		PacketSendUtility.sendPacket(requestor, new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplate, 0, 0, 4));
		PacketSendUtility.broadcastPacket(requestor, new SM_CRAFT_ANIMATION(requestor.getObjectId(), responder.getObjectId(), 0, 2), true);
		requestor.setCraftingTask(null);
		stop(true);
	}

	/**
	 * 交互结束：清理玩家制作任务引用。
	 * Interaction finish: clears the player's craft task reference.
	 */
	@Override
	protected void onInteractionFinish() {
		requestor.setCraftingTask(null);
	}

	/**
	 * 交互开始：加载产物模板、处理任务配方删除并启动动画。
	 * Interaction start: loads product templates, handles quest recipe removal, starts animation.
	 */
	@Override
	protected void onInteractionStart() {
		this.itemTemplate = DataManager.ITEM_DATA.getItemTemplate(recipeTemplate.getProductid());
		this.itemTemplateReal = this.itemTemplate;
		craftSetup();
		if ((this.recipeTemplate.getMaxProductionCount() != null) && (this.itemTemplateReal.getCategory() == ItemCategory.QUEST)) {
			this.requestor.getRecipeList().deleteRecipe(this.requestor, this.recipeTemplate.getId());
			onInteractionAbort();
		}
		setupCrit();
		PacketSendUtility.sendPacket(requestor, new SM_CRAFT_UPDATE(recipeTemplate.getSkillid(), itemTemplate, maxSuccessValue, maxFailureValue, 0));
		this.onInteraction();
		PacketSendUtility.broadcastPacket(requestor, new SM_CRAFT_ANIMATION(requestor.getObjectId(), responder.getObjectId(), recipeTemplate.getSkillid(), 0), true);
		PacketSendUtility.broadcastPacket(requestor, new SM_CRAFT_ANIMATION(requestor.getObjectId(), responder.getObjectId(), recipeTemplate.getSkillid(), 1), true);
	}

	/**
	 * 按概率与房屋加成预掷连击暴击。
	 * Pre-rolls combo crits using rates and house bonuses.
	 */
	private void setupCrit() {
		int chance = requestor.getRates().getCraftCritRate();
		if (maxCritCount > 0) {
			if (critCount > 0 && maxCritCount > 1) {
				chance = requestor.getRates().getComboCritRate();
				House house = requestor.getActiveHouse();
				if (house != null) {
					switch (house.getHouseType()) {
					case ESTATE:
					case PALACE:
						chance += 5;
						break;
					default:
						break;
					}
				}
			}
			if ((critCount < maxCritCount) && (Rnd.get(100) < chance)) {
				critCount++;
				crit = true;
			}
			if ((critCount > 0 && critCount <= maxCritCount && maxCritCount != 1) && (Rnd.get(100) < chance)) {
				purpleCrit = true;
			}
		}
	}

	/**
	 * 执行一次制作交互 tick。
	 * Performs one crafting interaction tick.
	 *
	 * @return true 表示任务应停止 / true if the task should stop
	 */
	@Override
	protected boolean onInteraction() {
		if (currentSuccessValue == maxSuccessValue) {
			return onSuccessFinish();
		}
		if (currentFailureValue == maxFailureValue) {
			onFailureFinish();
			return finishCraftAttempt();
		}
		analyzeInteraction();
		sendInteractionUpdate();
		return false;
	}

	/**
	 * 检查并应用连击暴击产物切换。
	 * Checks and applies combo-crit product switching.
	 *
	 * @return 是否发生了暴击产物切换 / true if a crit product switch occurred
	 */
	private boolean checkCrit() {
		if (crit) {
			crit = false;
			this.itemTemplateReal = DataManager.ITEM_DATA.getItemTemplate(recipeTemplate.getComboProduct(critCount));
			return true;
		}
		if (purpleCrit) {
			purpleCrit = false;
			this.itemTemplateReal = DataManager.ITEM_DATA.getItemTemplate(recipeTemplate.getComboProduct(critCount));
			return true;
		}
		return false;
	}
}
