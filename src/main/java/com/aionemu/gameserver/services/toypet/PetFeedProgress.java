package com.aionemu.gameserver.services.toypet;

/**
 * 宠物喂养进度，记录积分、消耗次数与饥饿等级。
 * Pet feed progress tracking points, consumption counts and hunger level.
 *
 * @author Rolandas
 */
public final class PetFeedProgress {

	private int totalPoints = 0;
	private short regularConsumed = 0;
	private short lovedConsumed = 0;
	private PetHungryLevel hungryLevel = PetHungryLevel.HUNGRY;
	private short lovedFoodMax = 0;
	private boolean lovedFeeded = false;

	/**
	 * 以喜爱食物上限初始化喂养进度。
	 * Initialize feed progress with loved-food limit.
	 *
	 * @param lovedFoodLimit 喜爱食物上限 / Loved food limit
	 */
	public PetFeedProgress(short lovedFoodLimit) {
		lovedFoodMax = (short) (lovedFoodLimit & 0x3F);
	}

	/**
	 * 返回累计喂养积分。
	 * Returns total feed points.
	 *
	 * Total points
	 */
	public int getTotalPoints() {
		return totalPoints;
	}

	/**
	 * 设置累计喂养积分（14 位掩码）。
	 * Set total feed points (14-bit mask).
	 *
	 * Points
	 */
	public void setTotalPoints(int points) {
		totalPoints = points & 0x3FFF;
	}

	/**
	 * 返回当前饥饿等级。
	 * Returns current hunger level.
	 *
	 * Hungry level
	 */
	public PetHungryLevel getHungryLevel() {
		return hungryLevel;
	}

	/**
	 * 设置饥饿等级。
	 * Set hunger level.
	 *
	 * @param level 饥饿等级 / Hungry level
	 */
	public void setHungryLevel(PetHungryLevel level) {
		hungryLevel = level;
	}

	/**
	 * 返回普通食物已喂次数。
	 * Returns regular food consumption count.
	 *
	 * @return 普通食物次数 / Regular count
	 */
	public int getRegularCount() {
		return regularConsumed & 0xFF;
	}

	/**
	 * 设置普通食物已喂次数。
	 * Set regular food consumption count.
	 *
	 * Count
	 */
	public void setRegularCount(short count) {
		regularConsumed = count;
	}

	/**
	 * 返回剩余可喂喜爱食物次数。
	 * Returns remaining loved-food feed count.
	 *
	 * Remaining count
	 */
	public int getLovedFoodRemaining() {
		return lovedFoodMax - lovedConsumed;
	}

	/**
	 * 是否处于喜爱食物喂养流程。
	 * Whether currently in loved-food feeding flow.
	 *
	 * @return 是否喜爱喂养 / Loved feeded flag
	 */
	public boolean isLovedFeeded() {
		return lovedFeeded;
	}

	/**
	 * 标记进入喜爱食物喂养状态。
	 * Mark that loved-food feeding has started.
	 */
	public void setIsLovedFeeded() {
		lovedFeeded = true;
	}

	/**
	 * 增加一次喂养计数。
	 * Increment feed consumption count.
	 *
	 * @param lovedFood 是否喜爱食物 / Whether loved food
	 */
	public void incrementCount(boolean lovedFood) {
		if (lovedFood) {
			lovedConsumed++;
		} else {
			regularConsumed++;
		}
	}

	/**
	 * 重置进度：喜爱喂养仅清标记，否则清积分与普通计数。
	 * Reset progress: clear loved flag only, or clear points and regular count.
	 */
	public void reset() {
		if (lovedFeeded)
			lovedFeeded = false;
		else {
			totalPoints = 0;
			regularConsumed = 0;
		}
	}

	/**
	 * 将进度编码为协议包用整型值。
	 * Encode progress into the integer value used by packets.
	 *
	 * @return 编码后的数据 / Encoded data
	 */
	public int getDataForPacket() {
		int value = getRegularCount() & 0xFF;
		value <<= 14;
		value |= totalPoints >> 2;
		value <<= 6;
		value |= lovedConsumed & 0x3F;
		value <<= 4; // 未知 / unk
		return value;
	}

	/**
	 * 从存档/协议整型还原进度。
	 * Restore progress from a saved/packet integer value.
	 *
	 * Saved data
	 */
	public void setData(int savedData) {
		savedData >>= 4; // drop unk
		lovedConsumed = (short) (savedData & 0x3F);
		savedData >>= 6;
		totalPoints = (savedData & 0x3FFF) << 2;
		savedData >>= 14;
		regularConsumed = (short) (savedData & 0xFF);
	}
}
