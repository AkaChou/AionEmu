package com.aionemu.gameserver.model.instance.playerreward;

import lombok.Getter;
import lombok.Setter;

/**
 * 术古 EmperorVault 玩家奖励，用于副本相关逻辑。
 * Shugo Emperor Vault Player Reward for instance logic.
 */

@Getter
@Setter
public class ShugoEmperorVaultPlayerReward extends InstancePlayerReward {
	/** 返回 score ap / Returns the score ap */
	private int scoreAP;
	/** 返回 rusted vault key / Returns the rusted vault key */
	private int rustedVaultKey;
	/**
	 * @return 是否已奖励 / whether rewarded
	 */
	private boolean isRewarded = false;

	public ShugoEmperorVaultPlayerReward(Integer object) {
		super(object);
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}
}
