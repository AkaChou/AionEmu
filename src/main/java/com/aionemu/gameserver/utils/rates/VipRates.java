package com.aionemu.gameserver.utils.rates;

import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.configs.main.RateConfig;

/**
 * VIP 会员游戏倍率，读取 {@link RateConfig}/{@link CraftConfig} 的 VIP_* 配置。
 * VIP membership-tier game rates backed by VIP_* {@link RateConfig}/{@link CraftConfig} values.
 */
public class VipRates extends Rates {
	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getXpRate() {
		return RateConfig.VIP_XP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGroupXpRate() {
		return RateConfig.VIP_GROUPXP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getBookXpRate() {
		return RateConfig.VIP_BOOK_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestXpRate() {
		return RateConfig.VIP_QUEST_XP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGatheringXPRate() {
		return RateConfig.VIP_GATHERING_XP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getGatheringCountRate() {
		return RateConfig.VIP_GATHERING_COUNT_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getCraftingXPRate() {
		return RateConfig.VIP_CRAFTING_XP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getDropRate() {
		return RateConfig.VIP_DROP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestKinahRate() {
		return RateConfig.VIP_QUEST_KINAH_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestApRate() {
		return RateConfig.VIP_QUEST_AP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGpPlayerLossRate() {
		return RateConfig.VIP_GP_PLAYER_LOSS_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestGpRate() {
		return RateConfig.VIP_QUEST_GP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestAbyssOpRate() {
		return RateConfig.VIP_QUEST_ABYSS_OP_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getQuestExpBoostRate() {
		return RateConfig.VIP_QUEST_EXP_BOOST_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getApPlayerGainRate() {
		return RateConfig.VIP_AP_PLAYER_GAIN_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getXpPlayerGainRate() {
		return RateConfig.VIP_XP_PLAYER_GAIN_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getApPlayerLossRate() {
		return RateConfig.VIP_AP_PLAYER_LOSS_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getApNpcRate() {
		return RateConfig.VIP_AP_NPC_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getDpNpcRate() {
		return RateConfig.VIP_DP_NPC_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getDpPlayerRate() {
		return RateConfig.VIP_DP_PLAYER_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getCraftCritRate() {
		return CraftConfig.VIP_CRAFT_CRIT_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getComboCritRate() {
		return CraftConfig.VIP_CRAFT_COMBO_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getDisciplineRewardRate() {
		return RateConfig.VIP_PVP_ARENA_DISCIPLINE_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getChaosRewardRate() {
		return RateConfig.VIP_PVP_ARENA_CHAOS_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getHarmonyRewardRate() {
		return RateConfig.VIP_PVP_ARENA_HARMONY_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGloryRewardRate() {
		return RateConfig.VIP_PVP_ARENA_GLORY_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getTollRewardRate() {
		return RateConfig.VIP_TOLL_REWARD_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getGpPlayerGainRate() {
		return RateConfig.VIP_GP_PLAYER_GAIN_RATE;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getPetFeedingRate() {
		return RateConfig.VIP_PET_FEEDING_RATE;
	}
}
