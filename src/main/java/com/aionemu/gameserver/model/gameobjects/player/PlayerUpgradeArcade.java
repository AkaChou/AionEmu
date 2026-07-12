package com.aionemu.gameserver.model.gameobjects.player;

/**
 * 玩家升级街机游戏对象。
 * Player Upgrade Arcade game object.
 *
 * @author Ranastic
 */
public class PlayerUpgradeArcade {
	private int frenzyPoints = 0;
	private int frenzyCount = 0;
	private int frenzyLevel = 1;
	private int failedLevel = 1;
	private boolean isFrenzy = false;
	private boolean reTry = false;
	private boolean failed = false;

	/** 返回 frenzy points / Returns the frenzy points */
	public int getFrenzyPoints() {
		return frenzyPoints;
	}

	/** 设置 frenzy points / Sets the frenzy points */
	public void setFrenzyPoints(int frenzyPoints) {
		this.frenzyPoints = frenzyPoints;
	}

	/** 返回 frenzy count / Returns the frenzy count */
	public int getFrenzyCount() {
		return frenzyCount;
	}

	/** 设置 frenzy count / Sets the frenzy count */
	public void setFrenzyCount(int frenzyCount) {
		this.frenzyCount = frenzyCount;
	}

	/** 返回 frenzy level / Returns the frenzy level */
	public int getFrenzyLevel() {
		return frenzyLevel;
	}

	/** 设置 frenzy level / Sets the frenzy level */
	public void setFrenzyLevel(int frenzyLevel) {
		this.frenzyLevel = frenzyLevel;
	}

	/** 返回失败等级 / Returns the failed level*/
	public int getFailedLevel() {
		return failedLevel;
	}

	/** 设置失败等级 / Sets the failed level*/
	public void setFailedLevel(int failedLevel) {
		this.failedLevel = failedLevel;
	}

	/**
	 * @return Whether frenzy
	 */
	public boolean isFrenzy() {
		return isFrenzy;
	}

	/** 设置 frenzy / Sets the frenzy */
	public void setFrenzy(boolean isFrenzy) {
		this.isFrenzy = isFrenzy;
	}

	/**
	 * @return 是否重试。 / Whether re try
	  */
	public boolean isReTry() {
		return reTry;
	}

	/** 设置 re try / Sets the re try */
	public void setReTry(boolean reTry) {
		this.reTry = reTry;
	}

	/** 是否失败 / Whether failed*/
	public boolean isFailed() {
		return failed;
	}

	/** 设置失败 / Sets the failed*/
	public void setFailed(boolean failed) {
		this.failed = failed;
	}

	/** 重置。 / Reset. */
	public void reset() {
		this.isFrenzy = false;
		this.failed = false;
		this.frenzyLevel = 1;
		this.failedLevel = 1;
		this.reTry = false;
	}
}
