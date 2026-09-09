package com.aionemu.gameserver.model.instance.playerreward;

import lombok.Getter;
import lombok.Setter;

/**
 * Smoldering 玩家奖励，用于副本相关逻辑。
 * Smoldering Player Reward for instance logic.
 */

public class SmolderingPlayerReward extends InstancePlayerReward {
	/** 返回 smoldering key / Returns the smoldering key */
	@Getter
	@Setter
	private int smolderingKey;
	/**
	 * @return 是否已奖励 / whether rewarded
	 */
	@Getter
	private boolean isRewarded = false;

	public SmolderingPlayerReward(Integer object) {
		super(object);
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}
}
