package com.aionemu.gameserver.model.instance.playerreward;

import lombok.Getter;
import lombok.Setter;

/**
 * SecretMunitions 工厂玩家奖励，用于副本相关逻辑。
 * Secret Munitions Factory Player Reward for instance logic.
 */

@Getter
@Setter
public class SecretMunitionsFactoryPlayerReward extends InstancePlayerReward {
	/** 返回 score ap / Returns the score ap */
	private int scoreAP;
	/** 返回 mechaturerk secret box / Returns the mechaturerk secret box */
	private int mechaturerkSecretBox;
	/**
	 * 获取 MechaturerkSpecialTreasureBox。
	 * Returns the mechaturerk special treasure box.
	 */
	private int mechaturerkSpecialTreasureBox;
	/**
	 * 获取 MechaturerkNormalTreasure 宝箱。
	 * Returns the mechaturerk normal treasure chest.
	 */
	private int mechaturerkNormalTreasureChest;
	private boolean isRewarded = false;

	public SecretMunitionsFactoryPlayerReward(Integer object) {
		super(object);
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}
}
