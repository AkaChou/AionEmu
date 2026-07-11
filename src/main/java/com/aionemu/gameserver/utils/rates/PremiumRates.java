package com.aionemu.gameserver.utils.rates;

import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.configs.main.RateConfig;

/**
 * 高级会员（Premium）游戏倍率，读取 {@link RateConfig}/{@link CraftConfig} 的 PREMIUM_* 配置。
 * Premium membership-tier game rates backed by PREMIUM_* {@link RateConfig}/{@link CraftConfig} values.
 */
public class PremiumRates extends Rates {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGroupXpRate() {
		return RateConfig.PREMIUM_GROUPXP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getApNpcRate() {
		return RateConfig.PREMIUM_AP_NPC_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getApPlayerGainRate() {
		return RateConfig.PREMIUM_AP_PLAYER_GAIN_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getXpPlayerGainRate() {
		return RateConfig.PREMIUM_XP_PLAYER_GAIN_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getApPlayerLossRate() {
		return RateConfig.PREMIUM_AP_PLAYER_LOSS_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getDropRate() {
		return RateConfig.PREMIUM_DROP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestKinahRate() {
		return RateConfig.PREMIUM_QUEST_KINAH_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestXpRate() {
		return RateConfig.PREMIUM_QUEST_XP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestApRate() {
		return RateConfig.PREMIUM_QUEST_AP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGpPlayerLossRate() {
		return RateConfig.PREMIUM_GP_PLAYER_LOSS_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestGpRate() {
		return RateConfig.PREMIUM_QUEST_GP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestAbyssOpRate() {
		return RateConfig.PREMIUM_QUEST_ABYSS_OP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestExpBoostRate() {
		return RateConfig.PREMIUM_QUEST_EXP_BOOST_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getXpRate() {
		return RateConfig.PREMIUM_XP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getBookXpRate() {
		return RateConfig.PREMIUM_BOOK_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getCraftingXPRate() {
		return RateConfig.PREMIUM_CRAFTING_XP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGatheringXPRate() {
		return RateConfig.PREMIUM_GATHERING_XP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getGatheringCountRate() {
		return RateConfig.PREMIUM_GATHERING_COUNT_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getDpNpcRate() {
		return RateConfig.PREMIUM_DP_NPC_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getDpPlayerRate() {
		return RateConfig.PREMIUM_DP_PLAYER_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getCraftCritRate() {
		return CraftConfig.PREMIUM_CRAFT_CRIT_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getComboCritRate() {
		return CraftConfig.PREMIUM_CRAFT_COMBO_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getDisciplineRewardRate() {
		return RateConfig.PREMIUM_PVP_ARENA_DISCIPLINE_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getChaosRewardRate() {
		return RateConfig.PREMIUM_PVP_ARENA_CHAOS_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getHarmonyRewardRate() {
		return RateConfig.PREMIUM_PVP_ARENA_HARMONY_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGloryRewardRate() {
		return RateConfig.PREMIUM_PVP_ARENA_GLORY_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getTollRewardRate() {
		return RateConfig.PREMIUM_TOLL_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGpPlayerGainRate() {
		return RateConfig.PREMIUM_GP_PLAYER_GAIN_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getPetFeedingRate() {
		return RateConfig.PREMIUM_PET_FEEDING_RATE;
	}
}
