package com.aionemu.gameserver.model.instance.playerreward;

import lombok.Getter;
import lombok.Setter;

/**
 * FissureOfOblivion 玩家奖励，用于副本相关逻辑。
 * Fissure Of Oblivion Player Reward for instance logic.
 */

@Getter
@Setter
public class FissureOfOblivionPlayerReward extends InstancePlayerReward {
	/** 返回 frozen marble of memory / Returns the frozen marble of memory */
	private int frozenMarbleOfMemory;
	/**
	 * @return 是否已奖励 / whether rewarded
	 */
	private boolean isRewarded = false;

	public FissureOfOblivionPlayerReward(Integer object) {
		super(object);
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}
}
