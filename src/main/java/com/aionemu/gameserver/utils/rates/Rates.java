package com.aionemu.gameserver.utils.rates;

/**
 * 游戏倍率抽象：按会员等级提供经验、掉落、任务与竞技场等收益倍率。
 * Abstract game rates: XP, drop, quest and arena multipliers by membership tier.
 */
public abstract class Rates {
	/**
	 * 组队经验倍率。
	 * Group XP rate.
	 *
	 * rate
	 */
	public abstract float getGroupXpRate();

	/**
	 * 基础经验倍率。
	 * Base XP rate.
	 *
	 * rate
	 */
	public abstract float getXpRate();

	/**
	 * 书籍经验倍率。
	 * Book XP rate.
	 *
	 * rate
	 */
	public abstract float getBookXpRate();

	/**
	 * 击杀 NPC 获得 AP 倍率。
	 * AP gain rate from NPCs.
	 *
	 * rate
	 */
	public abstract float getApNpcRate();

	/**
	 * 对玩家击杀获得 AP 倍率。
	 * AP gain rate from player kills.
	 *
	 * rate
	 */
	public abstract float getApPlayerGainRate();

	/**
	 * 对玩家击杀获得 GP 倍率。
	 * GP gain rate from player kills.
	 *
	 * rate
	 */
	public abstract float getGpPlayerGainRate();

	/**
	 * 对玩家击杀获得经验倍率。
	 * XP gain rate from player kills.
	 *
	 * rate
	 */
	public abstract float getXpPlayerGainRate();

	/**
	 * 死亡丢失 AP 倍率。
	 * AP loss rate on death.
	 *
	 * rate
	 */
	public abstract float getApPlayerLossRate();

	/**
	 * 死亡丢失 GP 倍率。
	 * GP loss rate on death.
	 *
	 * rate
	 */
	public abstract float getGpPlayerLossRate();

	/**
	 * 采集经验倍率。
	 * Gathering XP rate.
	 *
	 * rate
	 */
	public abstract float getGatheringXPRate();

	/**
	 * 采集次数倍率。
	 * Gathering count rate.
	 *
	 * rate
	 */
	public abstract int getGatheringCountRate();

	/**
	 * 制作经验倍率。
	 * Crafting XP rate.
	 *
	 * rate
	 */
	public abstract float getCraftingXPRate();

	/**
	 * 掉落倍率。
	 * Drop rate.
	 *
	 * rate
	 */
	public abstract float getDropRate();

	/**
	 * 任务经验倍率。
	 * Quest XP rate.
	 *
	 * rate
	 */
	public abstract float getQuestXpRate();

	/**
	 * 任务基纳倍率。
	 * Quest Kinah rate.
	 *
	 * rate
	 */
	public abstract float getQuestKinahRate();

	/**
	 * 任务 AP 倍率。
	 * Quest AP rate.
	 *
	 * rate
	 */
	public abstract float getQuestApRate();

	/**
	 * 任务 GP 倍率。
	 * Quest GP rate.
	 *
	 * rate
	 */
	public abstract float getQuestGpRate();

	/**
	 * 欧比斯行动任务倍率。
	 * Quest abyss operation rate.
	 *
	 * rate
	 */
	public abstract float getQuestAbyssOpRate();

	/**
	 * 任务经验加成倍率。
	 * Quest experience boost rate.
	 *
	 * rate
	 */
	public abstract float getQuestExpBoostRate();

	/**
	 * 击杀 NPC 获得 DP 倍率。
	 * DP gain rate from NPCs.
	 *
	 * rate
	 */
	public abstract float getDpNpcRate();

	/**
	 * 对玩家获得 DP 倍率。
	 * DP gain rate from players.
	 *
	 * rate
	 */
	public abstract float getDpPlayerRate();

	/**
	 * 制作暴击率。
	 * Craft critical rate.
	 *
	 * rate
	 */
	public abstract int getCraftCritRate();

	/**
	 * 制作连击暴击率。
	 * Craft combo critical rate.
	 *
	 * rate
	 */
	public abstract int getComboCritRate();

	/**
	 * 孤独竞技场奖励倍率。
	 * Discipline arena reward rate.
	 *
	 * rate
	 */
	public abstract float getDisciplineRewardRate();

	/**
	 * 混沌竞技场奖励倍率。
	 * Chaos arena reward rate.
	 *
	 * rate
	 */
	public abstract float getChaosRewardRate();

	/**
	 * 合作竞技场奖励倍率。
	 * Harmony arena reward rate.
	 *
	 * rate
	 */
	public abstract float getHarmonyRewardRate();

	/**
	 * 荣耀竞技场奖励倍率。
	 * Glory arena reward rate.
	 *
	 * rate
	 */
	public abstract float getGloryRewardRate();

	/**
	 * Toll 奖励倍率。
	 * Toll reward rate.
	 *
	 * rate
	 */
	public abstract float getTollRewardRate();

	/**
	 * 宠物喂养倍率。
	 * Pet feeding rate.
	 *
	 * rate
	 */
	public abstract float getPetFeedingRate();

	/**
	 * 按会员等级返回对应倍率实现。
	 * Returns the rates implementation for the membership tier.
	 *
	 * @param membership 会员等级（0/1 普通，2 高级，3+ VIP） / membership tier (0/1 regular, 2 premium, 3+ VIP)
	 * rates instance
	 */
	public static Rates getRatesFor(byte membership) {
		switch (membership) {
		case 0:
		case 1:
			return new RegularRates();
		case 2:
			return new PremiumRates();
		case 3:
			return new VipRates();
		default:
			return new VipRates();
		}
	}
}
