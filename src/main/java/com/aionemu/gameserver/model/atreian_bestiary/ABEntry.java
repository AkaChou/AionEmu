package com.aionemu.gameserver.model.atreian_bestiary;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * AB 条目，用于艾特里亚图鉴相关逻辑。
 * AB Entry for atreian bestiary logic.
 * @author Ranastic
 */

@Getter
@AllArgsConstructor
public class ABEntry {
	private final int id;
	private final int killCount;
	private final int level;
	private final int claimReward;

	/** Claim Reward Level / Claim Reward Level */
	public int claimRewardLevel() {
		return claimReward;
	}
}
