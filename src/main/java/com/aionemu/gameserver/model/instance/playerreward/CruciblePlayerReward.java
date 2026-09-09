package com.aionemu.gameserver.model.instance.playerreward;

import lombok.Getter;
import lombok.Setter;

/**
 * Crucible 玩家奖励，用于副本相关逻辑。
 * Crucible Player Reward for instance logic.
 */

public class CruciblePlayerReward extends InstancePlayerReward {
	/** 设置 insignia / Sets the insignia */
	@Getter
	@Setter
	private int insignia;
	/** 设置刷新点坐标。 / Sets the spawn position. */
	@Getter
	@Setter
	private int spawnPosition;
	/**
	 * @return 是否已奖励 / whether rewarded
	 */
	@Getter
	private boolean isRewarded = false;
	/** 是否玩家离开 / Whether player leave*/
	@Getter
	private boolean isPlayerLeave = false;
	/** 设置 player defeated / Sets the player defeated */
	@Getter
	@Setter
	private boolean isPlayerDefeated = false;

	public CruciblePlayerReward(Integer object) {
		super(object);
	}

	/** 设置 rewarded / Sets the rewarded */
	public void setRewarded() {
		isRewarded = true;
	}

	/** 设置玩家离开 / Sets the player leave*/
	public void setPlayerLeave() {
		isPlayerLeave = true;
	}
}
