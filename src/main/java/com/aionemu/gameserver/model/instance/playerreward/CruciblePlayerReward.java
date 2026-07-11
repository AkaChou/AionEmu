package com.aionemu.gameserver.model.instance.playerreward;

/**
 * Crucible 玩家奖励，用于副本相关逻辑。
 * Crucible Player Reward for instance logic.
 */

public class CruciblePlayerReward extends InstancePlayerReward {
	private int insignia;
	private int spawnPosition;
	private boolean isRewarded = false;
	private boolean isPlayerLeave = false;
	private boolean isPlayerDefeated = false;

	public CruciblePlayerReward(Integer object) {
		super(object);
	}

	/**
	 * @return Whether rewarded / Whether rewarded
	 */
	public boolean isRewarded() {
		return isRewarded;
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}

	/** 设置 insignia / Sets the insignia */
	public void setInsignia(int insignia) {
		this.insignia = insignia;
	}

	/** 返回 insignia / Returns the insignia */
	public int getInsignia() {
		return insignia;
	}

	/** 设置刷新点坐标。 / Sets the spawn position. */
	public void setSpawnPosition(int spawnPosition) {
		this.spawnPosition = spawnPosition;
	}

	/** 获取刷新点坐标。 / Returns the spawn position. */
	public int getSpawnPosition() {
		return spawnPosition;
	}

	/** 是否玩家离开 / Whether player leave*/
	public boolean isPlayerLeave() {
		return isPlayerLeave;
	}

	/** 设置玩家离开 / Sets the player leave*/
	public void setPlayerLeave() {
		isPlayerLeave = true;
	}

	/** 设置 player defeated / Sets the player defeated */
	public void setPlayerDefeated(boolean value) {
		isPlayerDefeated = value;
	}

	/** 是否 player defeated / Whether player defeated */
	public boolean isPlayerDefeated() {
		return isPlayerDefeated;
	}
}
