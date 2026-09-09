package com.aionemu.gameserver.model.atreian_bestiary;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * AB 条目，用于艾特里亚图鉴相关逻辑。
 * AB Entry for atreian bestiary logic.
 *
 * @author Ranastic
 */

@AllArgsConstructor
public class ABEntry {
	@Getter
	private final int id;
	@Getter
	private final int killCount;
	@Getter
	private final int level;
	private final int claimReward;

	/** Claim Reward Level / Claim Reward Level */
	public int claimRewardLevel() {
		return claimReward;
	}
}
