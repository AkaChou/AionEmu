package com.aionemu.gameserver.model.instance.playerreward;

/**
 * 术古 EmperorVault 玩家奖励，用于副本相关逻辑。
 * Shugo Emperor Vault Player Reward for instance logic.
 */

public class ShugoEmperorVaultPlayerReward extends InstancePlayerReward {
	private int scoreAP;
	private int rustedVaultKey;
	private boolean isRewarded = false;

	public ShugoEmperorVaultPlayerReward(Integer object) {
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

	/** 返回 score ap / Returns the score ap */
	public int getScoreAP() {
		return scoreAP;
	}

	/** 设置 score ap / Sets the score ap */
	public void setScoreAP(int ap) {
		this.scoreAP = ap;
	}

	/** 返回 rusted vault key / Returns the rusted vault key */
	public int getRustedVaultKey() {
		return rustedVaultKey;
	}

	/** 设置 rusted vault key / Sets the rusted vault key */
	public void setRustedVaultKey(int rustedVaultKey) {
		this.rustedVaultKey = rustedVaultKey;
	}
}
