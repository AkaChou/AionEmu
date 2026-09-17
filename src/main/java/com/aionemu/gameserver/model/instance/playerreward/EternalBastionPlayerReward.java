package com.aionemu.gameserver.model.instance.playerreward;

import lombok.Getter;
import lombok.Setter;

/**
 * EternalBastion 玩家奖励，用于副本相关逻辑。
 * Eternal Bastion Player Reward for instance logic.
 */

@Getter
@Setter
public class EternalBastionPlayerReward extends InstancePlayerReward {
	/** 返回 score ap / Returns the score ap */
	private int scoreAP;
	/** 返回 ceramium / Returns the ceramium */
	private int ceramium;
	/** 返回 high grade material box / Returns the high grade material box */
	private int highGradeMaterialBox;
	/** 返回 highest grade material box / Returns the highest grade material box */
	private int highestGradeMaterialBox;
	/**
	 * 获取 LowGrade 材料 SupportBundle。
	 * Returns the low grade material support bundle.
	 */
	private int lowGradeMaterialSupportBundle;
	/**
	 * 获取 HighGrade 材料 SupportBundle。
	 * Returns the high grade material support bundle.
	 */
	private int highGradeMaterialSupportBundle;
	/**
	 * 获取 HighestGrade 材料 SupportBundle。
	 * Returns the highest grade material support bundle.
	 */
	private int highestGradeMaterialSupportBundle;
	/**
	 * @return 是否已奖励 / whether rewarded
	 */
	private boolean isRewarded = false;

	public EternalBastionPlayerReward(Integer object) {
		super(object);
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}
}
