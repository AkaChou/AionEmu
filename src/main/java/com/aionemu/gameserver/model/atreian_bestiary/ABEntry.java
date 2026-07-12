package com.aionemu.gameserver.model.atreian_bestiary;

import lombok.Getter;

/**
 * AB 条目，用于艾特里亚图鉴相关逻辑。
 * AB Entry for atreian bestiary logic.
 *
 * @author Ranastic
 */

public class ABEntry {
	@Getter
	private int id;
	@Getter
	private int killCount;
	@Getter
	private int level;
	private int claimReward;

	public ABEntry(int id, int killCount, int level, int claimReward) {
		this.id = id;
		this.killCount = killCount;
		this.level = level;
		this.claimReward = claimReward;
	}

	/** Claim Reward Level / Claim Reward Level */
	public int claimRewardLevel() {
		return claimReward;
	}
}
