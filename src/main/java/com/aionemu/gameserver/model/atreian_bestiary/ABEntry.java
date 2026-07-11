package com.aionemu.gameserver.model.atreian_bestiary;

/**
 * AB 条目，用于艾特里亚图鉴相关逻辑。
 * AB Entry for atreian bestiary logic.
 *
 * @author Ranastic
 */

public class ABEntry {
	private int id;
	private int killCount;
	private int level;
	private int claimReward;

	public ABEntry(int id, int killCount, int level, int claimReward) {
		this.id = id;
		this.killCount = killCount;
		this.level = level;
		this.claimReward = claimReward;
	}

	/** 返回 ID / Returns the id */
	public int getId() {
		return id;
	}

	/** 返回 kill count / Returns the kill count */
	public int getKillCount() {
		return killCount;
	}

	/** 获取等级。 / Returns the level. */
	public int getLevel() {
		return level;
	}

	/** Claim Reward Level / Claim Reward Level */
	public int claimRewardLevel() {
		return claimReward;
	}
}
