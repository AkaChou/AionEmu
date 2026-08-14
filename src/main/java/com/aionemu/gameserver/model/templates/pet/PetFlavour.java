package com.aionemu.gameserver.model.templates.pet;

import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.services.toypet.PetFeedCalculator;
import com.aionemu.gameserver.services.toypet.PetFeedProgress;
import com.aionemu.gameserver.services.toypet.PetHungryLevel;

/**
 * 宠物口味模板（静态数据/XML）。
 * Pet flavour template (static data / XML).
 *
 * @author Rolandas
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PetFlavour", propOrder = { "food" })
public class PetFlavour {

	@XmlElement(required = true)
	protected List<PetRewards> food;

	@XmlAttribute(required = true)
	protected int id;

	@XmlAttribute(name = "full_count")
	protected int fullCount = 1;

	@XmlAttribute(name = "loved_limit")
	protected int lovedFoodLimit = 0;

	@XmlAttribute(name = "cd", required = true)
	protected int cooldown = 0;

	/** 返回食物奖励组列表 / Returns the food */
	public List<PetRewards> getFood() {
		if (food == null) {
			food = new ArrayList<PetRewards>();
		}
		return this.food;
	}

	/**
	 * 按物品 ID 返回匹配的食物类型，无匹配返回空。
	 * Returns a food group for the itemId. Null if doesn't match.
	 *
	 * @param itemId 物品 ID / item id
	 * @return 匹配的食物类型 / matching food type
	 */
	public FoodType getFoodType(int itemId) {
		for (PetRewards rewards : getFood()) {
			if (DataManager.ITEM_GROUPS_DATA.isFood(itemId, rewards.getType())) {
				return rewards.getType();
			}
		}
		return null;
	}

	/**
	 * 若满足条件返回奖励详情，否则为空；自动更新进度。
	 * Returns reward details if earned, otherwise null. Updates progress automatically.
	 *
	 * @param progress 喂食进度 / feeding progress
	 * @param foodType 食物类型 / food type
	 * @param itemLevel 物品等级 / item level
	 * @param playerLevel 玩家等级 / player level
	 * @return 奖励结果，未满足条件时为空 / reward result, null if not earned
	 */
	public PetFeedResult processFeedResult(PetFeedProgress progress, FoodType foodType, int itemLevel,
			int playerLevel) {
		return processFeedResult(progress, foodType, itemLevel, playerLevel, 1);
	}

	/** 处理喂食结果 / Process feed result */
	public PetFeedResult processFeedResult(PetFeedProgress progress, FoodType foodType, int itemLevel,
			int playerLevel, float feedingRate) {
		PetRewards rewardGroup = null;
		for (PetRewards rewards : getFood()) {
			if (rewards.getType() == foodType) {
				rewardGroup = rewards;
				break;
			}
		}
		if (rewardGroup == null)
			return null;

		int maxFeedCount = 1;
		if (rewardGroup.isLoved()) {
			progress.setIsLovedFeeded();
		} else {
			maxFeedCount = fullCount;
		}

		PetFeedCalculator.updatePetFeedProgress(progress, itemLevel, maxFeedCount, feedingRate);
		if (progress.getHungryLevel() != PetHungryLevel.FULL)
			return null;

		return PetFeedCalculator.getReward(maxFeedCount, rewardGroup, progress, playerLevel);
	}

	/**
	 * 判断指定物品是否为喜爱的食物。
	 * Checks whether the item is loved food.
	 *
	 * @param foodType 食物类型 / food type
	 * @param itemId 物品 ID / item id
	 * @return 是否为喜爱食物 / Whether loved food
	 */
	public boolean isLovedFood(FoodType foodType, int itemId) {
		PetRewards rewardGroup = null;
		for (PetRewards rewards : getFood()) {
			if (rewards.getType() == foodType) {
				rewardGroup = rewards;
				break;
			}
		}
		if (rewardGroup == null) {
			return false;
		}
		return rewardGroup.isLoved();
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回饱食次数 / Returns the full count */
	public int getFullCount() {
		return fullCount;
	}

	/** 返回喜爱的食物上限 / Returns the loved food limit */
	public int getLovedFoodLimit() {
		return lovedFoodLimit;
	}

	/** 返回冷却时间 / Returns the cooldown */
	public int getCooldDown() {
		return cooldown;
	}
}
