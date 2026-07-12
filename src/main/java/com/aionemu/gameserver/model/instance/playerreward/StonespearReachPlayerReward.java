package com.aionemu.gameserver.model.instance.playerreward;

/**
 * StonespearReach 玩家奖励，用于副本相关逻辑。
 * Stonespear Reach Player Reward for instance logic.
 */

public class StonespearReachPlayerReward extends InstancePlayerReward {
	private int scoreAP;
	private int ceramium;
	private boolean isRewarded = false;
	private boolean isPlayerLeave = false;

	public StonespearReachPlayerReward(Integer object) {
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

	/** 是否玩家离开 / Whether player leave*/
	public boolean isPlayerLeave() {
		return isPlayerLeave;
	}

	/** 设置玩家离开 / Sets the player leave*/
	public void setPlayerLeave() {
		isPlayerLeave = true;
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

	/** 设置 ceramium / Sets the ceramium */
	public void setCeramium(int ceramium) {
		this.ceramium = ceramium;
	}
}
