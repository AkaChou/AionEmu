package com.aionemu.gameserver.utils.rates;

import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.configs.main.RateConfig;

/**
 * 普通会员游戏倍率，读取 {@link RateConfig}/{@link CraftConfig} 的默认（非 PREMIUM/VIP）配置。
 * Regular membership-tier game rates backed by default (non-PREMIUM/VIP) {@link RateConfig}/{@link CraftConfig} values.
 */
public class RegularRates extends Rates {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGroupXpRate() {
		return RateConfig.GROUPXP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getDropRate() {
		return RateConfig.DROP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getApNpcRate() {
		return RateConfig.AP_NPC_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getApPlayerGainRate() {
		return RateConfig.AP_PLAYER_GAIN_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getXpPlayerGainRate() {
		return RateConfig.XP_PLAYER_GAIN_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getApPlayerLossRate() {
		return RateConfig.AP_PLAYER_LOSS_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGpPlayerLossRate() {
		return RateConfig.GP_PLAYER_LOSS_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestKinahRate() {
		return RateConfig.QUEST_KINAH_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestXpRate() {
		return RateConfig.QUEST_XP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestApRate() {
		return RateConfig.QUEST_AP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestGpRate() {
		return RateConfig.QUEST_GP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestAbyssOpRate() {
		return RateConfig.QUEST_ABYSS_OP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestExpBoostRate() {
		return RateConfig.QUEST_EXP_BOOST_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getXpRate() {
		return RateConfig.XP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getBookXpRate() {
		return RateConfig.BOOK_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getCraftingXPRate() {
		return RateConfig.CRAFTING_XP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGatheringXPRate() {
		return RateConfig.GATHERING_XP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getGatheringCountRate() {
		return RateConfig.GATHERING_COUNT_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getDpNpcRate() {
		return RateConfig.DP_NPC_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getDpPlayerRate() {
		return RateConfig.DP_PLAYER_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getCraftCritRate() {
		return CraftConfig.CRAFT_CRIT_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getComboCritRate() {
		return CraftConfig.CRAFT_COMBO_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getDisciplineRewardRate() {
		return RateConfig.PVP_ARENA_DISCIPLINE_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getChaosRewardRate() {
		return RateConfig.PVP_ARENA_CHAOS_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getHarmonyRewardRate() {
		return RateConfig.PVP_ARENA_HARMONY_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGloryRewardRate() {
		return RateConfig.PVP_ARENA_GLORY_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getTollRewardRate() {
		return RateConfig.TOLL_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGpPlayerGainRate() {
		return RateConfig.GP_PLAYER_GAIN_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getPetFeedingRate() {
		return RateConfig.PET_FEEDING_RATE;
	}
}
