package com.aionemu.gameserver.model.instance.playerreward;

import lombok.Getter;
import lombok.Setter;

/**
 * StonespearReach 玩家奖励，用于副本相关逻辑。
 * Stonespear Reach Player Reward for instance logic.
 */

public class StonespearReachPlayerReward extends InstancePlayerReward {
	/** 返回 score ap / Returns the score ap */
	@Getter
	@Setter
	private int scoreAP;
	/** 返回 ceramium / Returns the ceramium */
	@Getter
	@Setter
	private int ceramium;
	/**
	 * @return 是否已奖励 / whether rewarded
	 */
	@Getter
	private boolean isRewarded = false;
	/** 是否玩家离开 / Whether player leave*/
	@Getter
	private boolean isPlayerLeave = false;

	public StonespearReachPlayerReward(Integer object) {
		super(object);
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}

	/** 设置玩家离开 / Sets the player leave*/
	public void setPlayerLeave() {
		isPlayerLeave = true;
	}
}
