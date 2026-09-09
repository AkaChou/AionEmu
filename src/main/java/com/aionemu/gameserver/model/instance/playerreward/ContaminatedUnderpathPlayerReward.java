package com.aionemu.gameserver.model.instance.playerreward;

import lombok.Getter;
import lombok.Setter;

/**
 * ContaminatedUnderpath 玩家奖励，用于副本相关逻辑。
 * Contaminated Underpath Player Reward for instance logic.
 */

public class ContaminatedUnderpathPlayerReward extends InstancePlayerReward {
	/** 返回 score ap / Returns the score ap */
	@Getter
	@Setter
	private int scoreAP;
	/**
	 * 获取 ContaminatedPremium 奖励 Bundle。
	 * Returns the contaminated premium reward bundle.
	 */
	@Getter
	@Setter
	private int contaminatedPremiumRewardBundle;
	/**
	 * 获取 ContaminatedHighest 奖励 Bundle。
	 * Returns the contaminated highest reward bundle.
	 */
	@Getter
	@Setter
	private int contaminatedHighestRewardBundle;
	/**
	 * 获取 ContaminatedUnderpathSpecialPouch。
	 * Returns the contaminated underpath special pouch.
	 */
	@Getter
	@Setter
	private int contaminatedUnderpathSpecialPouch;
	/**
	 * @return 是否已奖励 / whether rewarded
	 */
	@Getter
	private boolean isRewarded = false;

	public ContaminatedUnderpathPlayerReward(Integer object) {
		super(object);
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}
}
