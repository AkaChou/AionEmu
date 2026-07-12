package com.aionemu.gameserver.model.instance.playerreward;

/**
 * SecretMunitions 工厂玩家奖励，用于副本相关逻辑。
 * Secret Munitions Factory Player Reward for instance logic.
 */

public class SecretMunitionsFactoryPlayerReward extends InstancePlayerReward {
	private int scoreAP;
	private int mechaturerkSecretBox;
	private int mechaturerkSpecialTreasureBox;
	private int mechaturerkNormalTreasureChest;
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

	/** 返回 score ap / Returns the score ap */
	public int getScoreAP() {
		return scoreAP;
	}

	/** 设置 score ap / Sets the score ap */
	public void setScoreAP(int ap) {
		this.scoreAP = ap;
	}

	/** 返回 mechaturerk secret box / Returns the mechaturerk secret box */
	public int getMechaturerkSecretBox() {
		return mechaturerkSecretBox;
	}

	/**
	 * 获取 MechaturerkSpecialTreasureBox。
	 * Returns the mechaturerk special treasure box.
	 */
	public int getMechaturerkSpecialTreasureBox() {
		return mechaturerkSpecialTreasureBox;
	}

	/**
	 * 获取 MechaturerkNormalTreasure 宝箱。
	 * Returns the mechaturerk normal treasure chest.
	 */
	public int getMechaturerkNormalTreasureChest() {
		return mechaturerkNormalTreasureChest;
	}

	/** 设置 mechaturerk secret box / Sets the mechaturerk secret box */
	public void setMechaturerkSecretBox(int mechaturerkSecretBox) {
		this.mechaturerkSecretBox = mechaturerkSecretBox;
	}

	/**
	 * 设置 MechaturerkSpecialTreasureBox。
	 * Sets the mechaturerk special treasure box.
	 */
	public void setMechaturerkSpecialTreasureBox(int mechaturerkSpecialTreasureBox) {
		this.mechaturerkSpecialTreasureBox = mechaturerkSpecialTreasureBox;
	}

	/**
	 * 设置 MechaturerkNormalTreasure 宝箱。
	 * Sets the mechaturerk normal treasure chest.
	 */
	public void setMechaturerkNormalTreasureChest(int mechaturerkNormalTreasureChest) {
		this.mechaturerkNormalTreasureChest = mechaturerkNormalTreasureChest;
	}
}
