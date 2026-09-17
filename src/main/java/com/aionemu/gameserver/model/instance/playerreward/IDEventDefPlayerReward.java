package com.aionemu.gameserver.model.instance.playerreward;

import lombok.Getter;
import lombok.Setter;

/**
 * ID 活动 Def 玩家奖励，用于副本相关逻辑。
 * ID Event Def Player Reward for instance logic.
 */

@Getter
@Setter
public class IDEventDefPlayerReward extends InstancePlayerReward {
	/** 返回 score ap / Returns the score ap */
	private int scoreAP;
	/**
	 * @return 是否已奖励 / whether rewarded
	 */
	private boolean isRewarded = false;
	/**
	 * 获取 WrapCashID 活动 DefLiveS 军阶。
	 * Returns the wrap cash id event def live s rank.
	 */
	private int wrapCashIDEventDefLiveSRank;
	/**
	 * 获取 WrapCashID 活动 DefLiveA 军阶。
	 * Returns the wrap cash id event def live a rank.
	 */
	private int wrapCashIDEventDefLiveARank;
	/**
	 * 获取 WrapCashID 活动 DefLiveB 军阶。
	 * Returns the wrap cash id event def live b rank.
	 */
	private int wrapCashIDEventDefLiveBRank;

	public IDEventDefPlayerReward(Integer object) {
		super(object);
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}
}
