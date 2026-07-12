package com.aionemu.gameserver.model.instance.playerreward;

/**
 * SealedArgentManor 玩家奖励，用于副本相关逻辑。
 * Sealed Argent Manor Player Reward for instance logic.
 */

public class SealedArgentManorPlayerReward extends InstancePlayerReward {
	private int scoreAP;
	private int argentManorBox;
	private int lesserArgentManorBox;
	private int greaterArgentManorBox;
	private boolean isRewarded = false;

	public SealedArgentManorPlayerReward(Integer object) {
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

	/** 返回 argent manor box / Returns the argent manor box */
	public int getArgentManorBox() {
		return argentManorBox;
	}

	/** 返回 lesser argent manor box / Returns the lesser argent manor box */
	public int getLesserArgentManorBox() {
		return lesserArgentManorBox;
	}

	/** 返回 greater argent manor box / Returns the greater argent manor box */
	public int getGreaterArgentManorBox() {
		return greaterArgentManorBox;
	}

	/** 设置 argent manor box / Sets the argent manor box */
	public void setArgentManorBox(int argentManorBox) {
		this.argentManorBox = argentManorBox;
	}

	/** 设置 lesser argent manor box / Sets the lesser argent manor box */
	public void setLesserArgentManorBox(int lesserArgentManorBox) {
		this.lesserArgentManorBox = lesserArgentManorBox;
	}

	/** 设置 greater argent manor box / Sets the greater argent manor box */
	public void setGreaterArgentManorBox(int greaterArgentManorBox) {
		this.greaterArgentManorBox = greaterArgentManorBox;
	}
}
