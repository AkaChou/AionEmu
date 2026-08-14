package com.aionemu.gameserver.model.instance.playerreward;

/**
 * Smoldering 玩家奖励，用于副本相关逻辑。
 * Smoldering Player Reward for instance logic.
 */

public class SmolderingPlayerReward extends InstancePlayerReward {
	private int smolderingKey;
	private boolean isRewarded = false;

	public SmolderingPlayerReward(Integer object) {
		super(object);
	}

	/**
	 * @return 是否已奖励 / whether rewarded
	 */
	public boolean isRewarded() {
		return isRewarded;
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}

	/** 返回 smoldering key / Returns the smoldering key */
	public int getSmolderingKey() {
		return smolderingKey;
	}

	/** 设置 smoldering key / Sets the smoldering key */
	public void setSmolderingKey(int smolderingKey) {
		this.smolderingKey = smolderingKey;
	}
}
