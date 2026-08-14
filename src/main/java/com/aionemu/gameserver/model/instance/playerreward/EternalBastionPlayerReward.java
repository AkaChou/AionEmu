package com.aionemu.gameserver.model.instance.playerreward;

/**
 * EternalBastion 玩家奖励，用于副本相关逻辑。
 * Eternal Bastion Player Reward for instance logic.
 */

public class EternalBastionPlayerReward extends InstancePlayerReward {
	private int scoreAP;
	private int ceramium;
	private int highGradeMaterialBox;
	private int highestGradeMaterialBox;
	private int lowGradeMaterialSupportBundle;
	private int highGradeMaterialSupportBundle;
	private int highestGradeMaterialSupportBundle;
	private boolean isRewarded = false;

	public EternalBastionPlayerReward(Integer object) {
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

	/** 返回 score ap / Returns the score ap */
	public int getScoreAP() {
		return scoreAP;
	}

	/** 设置 score ap / Sets the score ap */
	public void setScoreAP(int ap) {
		this.scoreAP = ap;
	}

	/** 返回 ceramium / Returns the ceramium */
	public int getCeramium() {
		return ceramium;
	}

	/** 返回 high grade material box / Returns the high grade material box */
	public int getHighGradeMaterialBox() {
		return highGradeMaterialBox;
	}

	/** 返回 highest grade material box / Returns the highest grade material box */
	public int getHighestGradeMaterialBox() {
		return highestGradeMaterialBox;
	}

	/**
	 * 获取 LowGrade 材料 SupportBundle。
	 * Returns the low grade material support bundle.
	 */
	public int getLowGradeMaterialSupportBundle() {
		return lowGradeMaterialSupportBundle;
	}

	/**
	 * 获取 HighGrade 材料 SupportBundle。
	 * Returns the high grade material support bundle.
	 */
	public int getHighGradeMaterialSupportBundle() {
		return highGradeMaterialSupportBundle;
	}

	/**
	 * 获取 HighestGrade 材料 SupportBundle。
	 * Returns the highest grade material support bundle.
	 */
	public int getHighestGradeMaterialSupportBundle() {
		return highestGradeMaterialSupportBundle;
	}

	/** 设置 ceramium / Sets the ceramium */
	public void setCeramium(int ceramium) {
		this.ceramium = ceramium;
	}

	/** 设置 high grade material box / Sets the high grade material box */
	public void setHighGradeMaterialBox(int highGradeMaterialBox) {
		this.highGradeMaterialBox = highGradeMaterialBox;
	}

	/** 设置 highest grade material box / Sets the highest grade material box */
	public void setHighestGradeMaterialBox(int highestGradeMaterialBox) {
		this.highestGradeMaterialBox = highestGradeMaterialBox;
	}

	/** 设置 low grade material support bundle / Sets the low grade material support bundle */
	public void setLowGradeMaterialSupportBundle(int lowGradeMaterialSupportBundle) {
		this.lowGradeMaterialSupportBundle = lowGradeMaterialSupportBundle;
	}

	/**
	 * 设置 HighGrade 材料 SupportBundle。
	 * Sets the high grade material support bundle.
	 */
	public void setHighGradeMaterialSupportBundle(int highGradeMaterialSupportBundle) {
		this.highGradeMaterialSupportBundle = highGradeMaterialSupportBundle;
	}

	/**
	 * 设置 HighestGrade 材料 SupportBundle。
	 * Sets the highest grade material support bundle.
	 */
	public void setHighestGradeMaterialSupportBundle(int highestGradeMaterialSupportBundle) {
		this.highestGradeMaterialSupportBundle = highestGradeMaterialSupportBundle;
	}
}
