package com.aionemu.gameserver.model.instance.playerreward;

/**
 * FissureOfOblivion 玩家奖励，用于副本相关逻辑。
 * Fissure Of Oblivion Player Reward for instance logic.
 */

public class FissureOfOblivionPlayerReward extends InstancePlayerReward {
	private int frozenMarbleOfMemory;
	private boolean isRewarded = false;

	public FissureOfOblivionPlayerReward(Integer object) {
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

	/** 返回 frozen marble of memory / Returns the frozen marble of memory */
	public int getFrozenMarbleOfMemory() {
		return frozenMarbleOfMemory;
	}

	/** 设置 frozen marble of memory / Sets the frozen marble of memory */
	public void setFrozenMarbleOfMemory(int frozenMarbleOfMemory) {
		this.frozenMarbleOfMemory = frozenMarbleOfMemory;
	}
}
