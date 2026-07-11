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
 * 宠物 Flavour 模板（静态数据/XML）。
 * XML template. / XML template.
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

	/** 返回食物 / Returns the food*/
	public List<PetRewards> getFood() {
		if (food == null) {
			food = new ArrayList<PetRewards>();
		}
		return this.food;
	}

	/**
	 * 返回 foodgroup 用于 itemId. 空若 doesn ' tmatch。 / Returns a food group for the itemId. Null if doesn't match
	 *
	 * @param itemId
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
	 * 返回 rewarddetails 若 earned , otherwise 空 . 更新 progressautomatically。 / Returns reward details if earned, otherwise null. Updates progress automatically
	 *
	 * @param progress
	 * @param foodType
	 * @return
	 */
	public PetFeedResult processFeedResult(PetFeedProgress progress, FoodType foodType, int itemLevel,
			int playerLevel) {
		return processFeedResult(progress, foodType, itemLevel, playerLevel, 1);
	}

	/** Process feed result / Process feed result */
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
	 * @return Whether loved food / Whether loved food
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

	/** 返回 full count / Returns the full count */
	public int getFullCount() {
		return fullCount;
	}

	/** 返回 loved food limit / Returns the loved food limit */
	public int getLovedFoodLimit() {
		return lovedFoodLimit;
	}

	/** 返回 coold down / Returns the coold down */
	public int getCooldDown() {
		return cooldown;
	}
}
