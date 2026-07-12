package com.aionemu.gameserver.model.instance.playerreward;

/**
 * ID 活动 Def 玩家奖励，用于副本相关逻辑。
 * ID Event Def Player Reward for instance logic.
 */

public class IDEventDefPlayerReward extends InstancePlayerReward {
	private int scoreAP;
	private boolean isRewarded = false;
	private int wrapCashIDEventDefLiveSRank;
	private int wrapCashIDEventDefLiveARank;
	private int wrapCashIDEventDefLiveBRank;

	public IDEventDefPlayerReward(Integer object) {
		super(object);
	}

	/**
	 * @return Whether rewarded
	 */
	public boolean isRewarded() {
		return isRewarded;
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}

	/** 返回 score ap / Returns the score ap */
	public int getScoreAP() {
		return scoreAP;
	}

	/** 设置 score ap / Sets the score ap */
	public void setScoreAP(int ap) {
		this.scoreAP = ap;
	}

	/**
	 * 获取 WrapCashID 活动 DefLiveS 军阶。
	 * Returns the wrap cash id event def live s rank.
	 */
	public int getWrapCashIDEventDefLiveSRank() {
		return wrapCashIDEventDefLiveSRank;
	}

	/**
	 * 获取 WrapCashID 活动 DefLiveA 军阶。
	 * Returns the wrap cash id event def live a rank.
	 */
	public int getWrapCashIDEventDefLiveARank() {
		return wrapCashIDEventDefLiveARank;
	}

	/**
	 * 获取 WrapCashID 活动 DefLiveB 军阶。
	 * Returns the wrap cash id event def live b rank.
	 */
	public int getWrapCashIDEventDefLiveBRank() {
		return wrapCashIDEventDefLiveBRank;
	}

	/** 设置 wrap cash id event def live s rank / Sets the wrap cash id event def live s rank */
	public void setWrapCashIDEventDefLiveSRank(int wrapCashIDEventDefLiveSRank) {
		this.wrapCashIDEventDefLiveSRank = wrapCashIDEventDefLiveSRank;
	}

	/** 设置 wrap cash id event def live a rank / Sets the wrap cash id event def live a rank */
	public void setWrapCashIDEventDefLiveARank(int wrapCashIDEventDefLiveARank) {
		this.wrapCashIDEventDefLiveARank = wrapCashIDEventDefLiveARank;
	}

	/** 设置 wrap cash id event def live b rank / Sets the wrap cash id event def live b rank */
	public void setWrapCashIDEventDefLiveBRank(int wrapCashIDEventDefLiveBRank) {
		this.wrapCashIDEventDefLiveBRank = wrapCashIDEventDefLiveBRank;
	}
}
