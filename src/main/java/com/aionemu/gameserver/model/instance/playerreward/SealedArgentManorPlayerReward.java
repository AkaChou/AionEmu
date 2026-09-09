package com.aionemu.gameserver.model.instance.playerreward;

import lombok.Getter;
import lombok.Setter;

/**
 * SealedArgentManor 玩家奖励，用于副本相关逻辑。
 * Sealed Argent Manor Player Reward for instance logic.
 */

public class SealedArgentManorPlayerReward extends InstancePlayerReward {
	/** 返回 score ap / Returns the score ap */
	@Getter
	@Setter
	private int scoreAP;
	/** 返回 argent manor box / Returns the argent manor box */
	@Getter
	@Setter
	private int argentManorBox;
	/** 返回 lesser argent manor box / Returns the lesser argent manor box */
	@Getter
	@Setter
	private int lesserArgentManorBox;
	/** 返回 greater argent manor box / Returns the greater argent manor box */
	@Getter
	@Setter
	private int greaterArgentManorBox;
	/**
	 * @return 是否已奖励 / whether rewarded
	 */
	@Getter
	private boolean isRewarded = false;

	public SealedArgentManorPlayerReward(Integer object) {
		super(object);
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}
}
