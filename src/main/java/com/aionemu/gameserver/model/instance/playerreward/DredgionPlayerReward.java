package com.aionemu.gameserver.model.instance.playerreward;

/**
 * 无畏舰玩家奖励，用于副本相关逻辑。
 * Dredgion Player Reward for instance logic.
 */

public class DredgionPlayerReward extends InstancePlayerReward {
	private int zoneCaptured;

	public DredgionPlayerReward(Integer object) {
		super(object);
	}

	/** 占领区域 / Capture Zone */
	public void captureZone() {
		zoneCaptured++;
	}

	/** 返回 zone captured / Returns the zone captured */
	public int getZoneCaptured() {
		return zoneCaptured;
	}
}
