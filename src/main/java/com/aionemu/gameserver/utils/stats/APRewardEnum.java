package com.aionemu.gameserver.utils.stats;

import java.util.NoSuchElementException;

/**
 * 按欧比斯军衔给出 PvE AP 奖励百分比
 * PvE AP reward percentage by abyss rank
 *
 * @author Source
 */
public enum APRewardEnum {

	/** 9 级士兵 100% / Grade 9 Soldier 100% */
	GRADE9_SOLDIER(1, 100f),
	/** 8 级士兵 100% / Grade 8 Soldier 100% */
	GRADE8_SOLDIER(2, 100f),
	/** 7 级士兵 100% / Grade 7 Soldier 100% */
	GRADE7_SOLDIER(3, 100f),
	/** 6 级士兵 93.75% / Grade 6 Soldier 93.75% */
	GRADE6_SOLDIER(4, 93.75f),
	/** 5 级士兵 87.5% / Grade 5 Soldier 87.5% */
	GRADE5_SOLDIER(5, 87.5f),
	/** 4 级士兵 84.75% / Grade 4 Soldier 84.75% */
	GRADE4_SOLDIER(6, 84.75f),
	/** 3 级士兵 81.25% / Grade 3 Soldier 81.25% */
	GRADE3_SOLDIER(7, 81.25f),
	/** 2 级士兵 62.5% / Grade 2 Soldier 62.5% */
	GRADE2_SOLDIER(8, 62.5f),
	/** 1 级士兵 37.5% / Grade 1 Soldier 37.5% */
	GRADE1_SOLDIER(9, 37.5f),
	/** 1 星军官 31.25% / 1-Star Officer 31.25% */
	STAR1_OFFICER(10, 31.25f),
	/** 2 星军官 31.25% / 2-Star Officer 31.25% */
	STAR2_OFFICER(11, 31.25f),
	/** 3 星军官 18.75% / 3-Star Officer 18.75% */
	STAR3_OFFICER(12, 18.75f),
	/** 4 星军官 18.75% / 4-Star Officer 18.75% */
	STAR4_OFFICER(13, 18.75f),
	/** 5 星军官 12.5% / 5-Star Officer 12.5% */
	STAR5_OFFICER(14, 12.5f),
	/** 将军 6.25% / General 6.25% */
	GENERAL(15, 6.25f),
	/** 大将军 6.25% / Great General 6.25% */
	GREAT_GENERAL(16, 6.25f),
	/** 司令 6.25% / Commander 6.25% */
	COMMANDER(17, 6.25f),
	/** 最高司令 6.25% / Supreme Commander 6.25% */
	SUPREME_COMMANDER(18, 6.25f);

	/** 玩家军衔 ID / Player rank id */
	private int playerRank;

	/** AP 奖励百分比 / AP reward percent */
	private float rewardPercent;

	/**
	 * 构造 AP 奖励条目
	 * Construct an AP reward entry
	 *
	 * Rank id
	 * @param rewardPercent 奖励百分比 / Reward percent
	 */
	private APRewardEnum(int playerRank, float rewardPercent) {
		this.playerRank = playerRank;
		this.rewardPercent = rewardPercent;
	}

	/**
	 * 获取该军衔的 AP 奖励百分比
	 * Get AP reward percent for this rank
	 *
	 * @return 奖励百分比 / Reward percent
	 */
	public float rewardPercent() {
		return rewardPercent;
	}

	/**
	 * 按军衔返回 AP 奖励百分比（越界取边界值）
	 * Return AP reward percent by rank (clamped to bounds)
	 *
	 * @param playerRank 当前欧比斯军衔 / Current abyss rank
	 * @return AP 奖励百分比 / AP reward percentage
	 */
	public static float apReward(int playerRank) {
		if (playerRank < GRADE9_SOLDIER.playerRank) {
			return GRADE9_SOLDIER.rewardPercent;
		}
		if (playerRank > SUPREME_COMMANDER.playerRank) {
			return SUPREME_COMMANDER.rewardPercent;
		}

		for (APRewardEnum apReward : values()) {
			if (apReward.playerRank == playerRank) {
				return apReward.rewardPercent;
			}
		}
		throw new NoSuchElementException("AP reward for such rank was not found");
	}
}
