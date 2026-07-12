package com.aionemu.gameserver.model.instance.playerreward;

/**
 * ContaminatedUnderpath 玩家奖励，用于副本相关逻辑。
 * Contaminated Underpath Player Reward for instance logic.
 */

public class ContaminatedUnderpathPlayerReward extends InstancePlayerReward {
	private int scoreAP;
	private int contaminatedPremiumRewardBundle;
	private int contaminatedHighestRewardBundle;
	private int contaminatedUnderpathSpecialPouch;
	private boolean isRewarded = false;

	public ContaminatedUnderpathPlayerReward(Integer object) {
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
	 * 获取 ContaminatedPremium 奖励 Bundle。
	 * Returns the contaminated premium reward bundle.
	 */
	public int getContaminatedPremiumRewardBundle() {
		return contaminatedPremiumRewardBundle;
	}

	/**
	 * 获取 ContaminatedHighest 奖励 Bundle。
	 * Returns the contaminated highest reward bundle.
	 */
	public int getContaminatedHighestRewardBundle() {
		return contaminatedHighestRewardBundle;
	}

	/**
	 * 获取 ContaminatedUnderpathSpecialPouch。
	 * Returns the contaminated underpath special pouch.
	 */
	public int getContaminatedUnderpathSpecialPouch() {
		return contaminatedUnderpathSpecialPouch;
	}

	/**
	 * 设置 ContaminatedPremium 奖励 Bundle。
	 * Sets the contaminated premium reward bundle.
	 */
	public void setContaminatedPremiumRewardBundle(int contaminatedPremiumRewardBundle) {
		this.contaminatedPremiumRewardBundle = contaminatedPremiumRewardBundle;
	}

	/**
	 * 设置 ContaminatedHighest 奖励 Bundle。
	 * Sets the contaminated highest reward bundle.
	 */
	public void setContaminatedHighestRewardBundle(int contaminatedHighestRewardBundle) {
		this.contaminatedHighestRewardBundle = contaminatedHighestRewardBundle;
	}

	/**
	 * 设置 ContaminatedUnderpathSpecialPouch。
	 * Sets the contaminated underpath special pouch.
	 */
	public void setContaminatedUnderpathSpecialPouch(int contaminatedUnderpathSpecialPouch) {
		this.contaminatedUnderpathSpecialPouch = contaminatedUnderpathSpecialPouch;
	}
}
