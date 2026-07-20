package com.aionemu.gameserver.model.instance.playerreward;

/**
 * SecretMunitions 工厂玩家奖励，用于副本相关逻辑。
 * Secret Munitions Factory Player Reward for instance logic.
 */

public class SecretMunitionsFactoryPlayerReward extends InstancePlayerReward {
	private boolean isRewarded = false;

	public SecretMunitionsFactoryPlayerReward(Integer object) {
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
}
