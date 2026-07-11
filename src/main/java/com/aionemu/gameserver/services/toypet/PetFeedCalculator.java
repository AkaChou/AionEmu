package com.aionemu.gameserver.services.toypet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;


import org.apache.commons.lang3.ArrayUtils;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.templates.pet.PetFeedResult;
import com.aionemu.gameserver.model.templates.pet.PetFlavour;
import com.aionemu.gameserver.model.templates.pet.PetRewards;

/**
 * 宠物喂养计算器，预计算积分表并更新喂养进度与奖励。
 * Pet feed calculator precomputing point tables and updating feed progress/rewards.
 *
 * <p>
 * 当前预计算值已乘以 4；协议中为 14 位。最大值 17600 / 4 为 13 位；喂养积分与零售包一致。
 * Current pre-calculated values multiplied by 4; in packet 14 bits. Max value:
 * 17600 / 4 is 13 bits; feed points as in retail packets.
 * </p>
 * <pre>
 * static final byte[][] pointValues = new byte[][] {
 * // 10 25 40 50 100 200 -- feed max count
 * { 0, 0, 0, 0, 0, 0 }, // level 1~5 items (feed points 0)
 * { 80, 200, 320, 400, 800, 1600 }, // level 6~10 items (feed points 8)
 * { 160, 400, 640, 800, 1600, 3200 }, // level 11~15 items (feed points 16)
 * { 240, 600, 960, 1200, 2400, 4800 }, // level 16~20 items (feed points 24)
 * { 320, 800, 1280, 1600, 3200, 6400 }, // level 21~25 items (feed points 32)
 * { 400, 1000, 1600, 2000, 4000, 8000 }, // level 26~30 items (feed points 40)
 * { 480, 1200, 1920, 2400, 4800, 9600 }, // level 31~35 items (feed points 48)
 * { 560, 1400, 2240, 2800, 5600, 11200 }, // level 36~40 items (feed points 56)
 * { 640, 1600, 2560, 3200, 6400, 12800 }, // level 41~45 items (feed points 64)
 * { 720, 1800, 2880, 3600, 7200, 14400 }, // level 46~50 items (feed points 72)
 * { 800, 2000, 3200, 4000, 8000, 16000 }, // level 51~55 items (feed points 80)
 * { 880, 2200, 3520, 4400, 8800, 17600 } // level 56~60 items (feed points 88)
 * };
 * </pre>
 *
 * @author Rolandas
 */
public final class PetFeedCalculator {

	static byte ITEM_MAX_LEVEL = 60;
	static final short[] fullCounts;
	static final byte[] itemLevels;
	static final int[][] pointValues;

	static {
		TreeSet<Short> counts = new TreeSet<Short>();
		for (PetFlavour flavour : DataManager.PET_FEED_DATA.getPetFlavours()) {
			if (flavour.getFullCount() > 0) {
				counts.add((short) (flavour.getFullCount() & 0xFFFF));
			}
		}

		fullCounts = new short[counts.size()];
		int i = 0;
		Iterator<Short> countIter = counts.iterator();
		while (countIter.hasNext()) {
			fullCounts[i++] = countIter.next();
		}
		itemLevels = new byte[ITEM_MAX_LEVEL / 5];
		itemLevels[0] = 5;
		for (int j = 1; j < itemLevels.length; j++) {
			itemLevels[j] = (byte) (itemLevels[j - 1] + 5);
		}
		pointValues = new int[itemLevels.length][fullCounts.length];
		calculate();
	}

	/**
	 * 按物品等级与最大喂养次数预计算积分表。
	 * Calculate point values for each item level and each max feed count.
	 */
	static void calculate() {
		for (byte levelByte : itemLevels) {
			short level = (short) (levelByte & 0xFF);
			if (level < 10) {
				continue;
			}
			int countIndex = 0;
			for (short countByte : fullCounts) {
				short count = (short) (countByte & 0xFF);
				int finalLevel = level;
				if (finalLevel % 5 == 0) {
					finalLevel--;
				}
				int pointLevel = (int) itemLevels[(int) (finalLevel / 5)];
				int feedPoints = Math.max(0, pointLevel - 5) / 5 * 8;
				// System.out.println("ITEM LEVEL: " + level + ", COUNT: " + count + ", STEP: "
				// + feedPoints);
				pointValues[finalLevel / 5][countIndex++] = getPoints(feedPoints, count);
			}
		}
	}

	/**
	 * 计算喂满 maxFeedCount 次后的累计积分。
	 * Formula to calculate pointValues array entries.
	 *
	 * @param feedPoints 单次喂养积分 / Feed points per item
	 * @param maxFeedCount 最大喂养次数 / Max feeding count
	 * Accumulated points after all items are fed
	 */
	static int getPoints(int feedPoints, int maxFeedCount) {
		int points = 0;
		int state = 0;
		int consumed = 0;
		while (consumed < maxFeedCount) {
			boolean needSwitch = false;
			int oldPoints = points;
			if ((state == 0 && consumed > maxFeedCount * 0.5f) || (state == 1 && consumed > maxFeedCount * 0.8f)
					|| (state == 2 && consumed > maxFeedCount * 1.05)) {
				needSwitch = true;
			}
			points += feedPoints;
			if (needSwitch) {
				state++;
				if (state == 1 && consumed <= 0.487f * maxFeedCount || state == 2 && consumed <= 0.78f * maxFeedCount) {
					state--;
					points = oldPoints;
				}
			}
			consumed++;
		}
		return points;
	}

	/**
	 * 按默认喂养倍率 1.0 更新喂养进度。
	 * Update feed progress with default feeding rate 1.0.
	 *
	 * Feed progress
	 * @param itemLevel 食物物品等级 / Food item level
	 * @param maxFeedCount 最大喂养次数 / Max feed count
	 */
	public static void updatePetFeedProgress(PetFeedProgress progress, int itemLevel, int maxFeedCount) {
		updatePetFeedProgress(progress, itemLevel, maxFeedCount, 1);
	}

	/**
	 * 根据食物等级与喂养倍率更新喂养进度与饥饿等级。
	 * Update feed progress and hunger level by food level and feeding rate.
	 *
	 * Feed progress
	 * @param itemLevel 食物物品等级 / Food item level
	 * @param maxFeedCount 最大喂养次数 / Max feed count
	 * Feeding rate
	 */
	public static void updatePetFeedProgress(PetFeedProgress progress, int itemLevel, int maxFeedCount, float feedingRate) {
		float rate = Math.max(0, feedingRate);
		PetHungryLevel currHungryLevel = progress.getHungryLevel();
		if (progress.isLovedFeeded()) { // loved food
			if (progress.getLovedFoodRemaining() == 0) {
				return;
			}
			progress.setHungryLevel(PetHungryLevel.FULL);
			progress.incrementCount(true);
			return;
		}

		int oldPoints = progress.getTotalPoints();
		boolean needSwitch = false;
		float regularCount = progress.getRegularCount() * rate;

		if ((currHungryLevel == PetHungryLevel.HUNGRY && regularCount > maxFeedCount * 0.5f)
				|| (currHungryLevel == PetHungryLevel.CONTENT && regularCount > maxFeedCount * 0.8f)
				|| (currHungryLevel == PetHungryLevel.SEMIFULL && regularCount > maxFeedCount * 1.05)) {
			// 强制切换等级 / forcefully switch level
			needSwitch = true;
		} else {
			int finalLevel = itemLevel;
			if (finalLevel % 5 == 0) {
				finalLevel--;
			}
			byte pointLevel = itemLevels[(int) (finalLevel / 5)];
			byte pointsEarned = (byte) (Math.max(0, pointLevel - 5) / 5 * 8);
			int feedProgress = progress.getTotalPoints() + Math.round(pointsEarned * rate);
			progress.setTotalPoints(feedProgress);
		}

		if (needSwitch) {
			// 仅防止切换等级 / just a prevention to not switch level
			PetHungryLevel nextLevel = progress.getHungryLevel().getNextValue();
			if (nextLevel == PetHungryLevel.CONTENT && regularCount <= 0.487f * maxFeedCount
					|| nextLevel == PetHungryLevel.SEMIFULL && regularCount <= 0.78f * maxFeedCount) {
				progress.setTotalPoints(oldPoints);
			} else {
				progress.setHungryLevel(nextLevel);
			}
		}
		progress.incrementCount(false);
	}

	/**
	 * 在吃饱后按积分与奖励组选取喂养奖励。
	 * Select feed reward from reward group after the pet is full.
	 *
	 * @param fullCount 吃饱所需次数 / Full count
	 * Reward group
	 * Feed progress
	 * Player level
	 *
	 * @return 奖励结果，不可领时为 null / Reward result, or null if none
	 */
	public static PetFeedResult getReward(int fullCount, PetRewards rewardGroup, PetFeedProgress progress,
			int playerLevel) {
		if (progress.getHungryLevel() != PetHungryLevel.FULL || rewardGroup.getResults().size() == 0) {
			return null;
		}
		int pointsIndex = ArrayUtils.indexOf(fullCounts, (short) fullCount);
		if (pointsIndex == ArrayUtils.INDEX_NOT_FOUND) {
			return null;
		}
		if (progress.isLovedFeeded()) { // for cash feed
			if (rewardGroup.getResults().size() == 1) {
				return rewardGroup.getResults().get(0);
			}
			List<PetFeedResult> validRewards = new ArrayList<PetFeedResult>();
			int maxLevel = 0;
			for (PetFeedResult result : rewardGroup.getResults()) {
				int resultLevel = DataManager.ITEM_DATA.getItemTemplate(result.getItem()).getLevel();
				if (resultLevel > playerLevel)
					continue;
				if (resultLevel > maxLevel) {
					maxLevel = resultLevel;
					validRewards.clear();
				}
				validRewards.add(result);
			}
			if (validRewards.size() == 0) {
				return null;
			}
			if (validRewards.size() == 1) {
				return validRewards.get(0);
			}
			return validRewards.get(Rnd.get(validRewards.size()));
		}

		int rewardIndex = 0;
		int totalRewards = rewardGroup.getResults().size();
		for (int row = 1; row < pointValues.length; row++) {
			int[] points = pointValues[row];
			if (points[pointsIndex] <= progress.getTotalPoints()) {
				rewardIndex = Math.round((float) totalRewards / (pointValues.length - 1) * row) - 1;
			}
		}

		// 修复舍入偏差 / Fix rounding discrepancy
		if (rewardIndex < 0) {
			rewardIndex = 0;
		} else if (rewardIndex > rewardGroup.getResults().size() - 1) {
			rewardIndex = rewardGroup.getResults().size() - 1;
		}
		return rewardGroup.getResults().get(rewardIndex);
	}
}
