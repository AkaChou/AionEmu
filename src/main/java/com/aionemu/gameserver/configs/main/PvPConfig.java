package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * PvP 击杀奖励、连杀与限制相关配置。
 * PvP kill rewards, killing spree and restriction related configuration.
 *
 * @author Rinzler (Encom)
 */
public class PvPConfig {

	/**
	 * 每日可获得完整 AP 的最大击杀数。
	 * Max daily PvP kills allowed for full AP.
	 */
	@Property(key = "gameserver.pvp.maxkills", defaultValue = "5")
	public static int MAX_DAILY_PVP_KILLS;

	/**
	 * 是否启用 PvP 点券奖励。
	 * Whether PvP toll rewards are enabled.
	 */
	@Property(key = "gameserver.pvp.toll.rewarding.enable", defaultValue = "false")
	public static boolean ENABLE_TOLL_REWARD;
	/**
	 * PvP 点券奖励触发概率。
	 * Chance to grant PvP toll reward.
	 */
	@Property(key = "gameserver.pvp.toll.reward.chance", defaultValue = "50")
	public static int TOLL_CHANCE;
	/**
	 * PvP 点券奖励数量。
	 * Quantity of PvP toll reward.
	 */
	@Property(key = "gameserver.pvp.toll.reward.quantity", defaultValue = "5")
	public static int TOLL_QUANTITY;

	/**
	 * 是否启用连杀系统。
	 * Whether killing spree system is enabled.
	 */
	@Property(key = "gameserver.pvp.killingspree.enable", defaultValue = "false")
	public static boolean ENABLE_KILLING_SPREE_SYSTEM;
	/**
	 * Spree 连杀所需击杀数。
	 * Kill count required for Spree.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.spree", defaultValue = "5")
	public static int SPREE_KILL_COUNT;
	/**
	 * Carnage 连杀所需击杀数。
	 * Kill count required for Carnage.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.carnage", defaultValue = "10")
	public static int CARNAGE_KILL_COUNT;
	/**
	 * Genocide 连杀所需击杀数。
	 * Kill count required for Genocide.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.genocide", defaultValue = "15")
	public static int GENOCIDE_KILL_COUNT;
	/**
	 * Rampage 连杀所需击杀数。
	 * Kill count required for Rampage.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.rampage", defaultValue = "20")
	public static int RAMPAGE_KILL_COUNT;
	/**
	 * Dominating 连杀所需击杀数。
	 * Kill count required for Dominating.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.dominating", defaultValue = "25")
	public static int DOMINATING_KILL_COUNT;
	/**
	 * Unstoppable 连杀所需击杀数。
	 * Kill count required for Unstoppable.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.unstoppable", defaultValue = "30")
	public static int UNSTOPPABLE_KILL_COUNT;
	/**
	 * Insane Monster 连杀所需击杀数。
	 * Kill count required for Insane Monster.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.uniquemonster", defaultValue = "35")
	public static int INSANEMONSTER_KILL_COUNT;
	/**
	 * Godlike 连杀所需击杀数。
	 * Kill count required for Godlike.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.godlike", defaultValue = "40")
	public static int GODLIKE_KILL_COUNT;
	/**
	 * Wicked Sick 连杀所需击杀数。
	 * Kill count required for Wicked Sick.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.wickedsick", defaultValue = "45")
	public static int WICKEDSICK_KILL_COUNT;
	/**
	 * Muthafakaaas 连杀所需击杀数。
	 * Kill count required for Muthafakaaas.
	 */
	@Property(key = "gameserver.pvp.raw.killcount.muthafakaaas", defaultValue = "50")
	public static int MUTHAFAKAAAS_KILL_COUNT;

	/**
	 * 连杀奖励 1 的数量。
	 * Quantity of spree reward item 1.
	 */
	@Property(key = "gameserver.pvp.spree_reward1.count", defaultValue = "10")
	public static int SPREE_REWARD_COUNT1;
	/**
	 * 连杀奖励 2 的数量。
	 * Quantity of spree reward item 2.
	 */
	@Property(key = "gameserver.pvp.spree_reward2.count", defaultValue = "10")
	public static int SPREE_REWARD_COUNT2;
	/**
	 * 连杀奖励 1 的物品 ID。
	 * Item ID of spree reward 1.
	 */
	@Property(key = "gameserver.pvp.spree_reward1.item", defaultValue = "166020000")
	public static int SPREE_REWARD_ITEM1;
	/**
	 * 连杀奖励 2 的物品 ID。
	 * Item ID of spree reward 2.
	 */
	@Property(key = "gameserver.pvp.spree_reward2.item", defaultValue = "166030005")
	public static int SPREE_REWARD_ITEM2;

	/**
	 * 是否启用 PvP 荣耀点奖励。
	 * Whether PvP glory point rewards are enabled.
	 */
	@Property(key = "gameserver.enable.gp.reward", defaultValue = "false")
	public static boolean ENABLE_GP_REWARD;
	/**
	 * 是否启用 PvP 荣耀点损失。
	 * Whether PvP glory point loss is enabled.
	 */
	@Property(key = "gameserver.enable.gp.lose", defaultValue = "false")
	public static boolean ENABLE_GP_LOSE;
	/**
	 * 是否使用固定荣耀点损失值。
	 * Whether fixed glory point loss is used.
	 */
	@Property(key = "gameserver.enable.gp.lose.fixed", defaultValue = "false")
	public static boolean ENABLE_GP_FIXED_LOSE;
	/**
	 * 固定荣耀点损失数值。
	 * Fixed glory point loss amount.
	 */
	@Property(key = "gameserver.gp.lose", defaultValue = "100")
	public static int GP_LOSE;

	/**
	 * 连续击杀时间限制（秒，0 表示不限制）。
	 * Chain-kill time restriction in seconds (0 disables).
	 */
	@Property(key = "gameserver.pvp.chainkill.time.restriction", defaultValue = "0")
	public static int CHAIN_KILL_TIME_RESTRICTION;
	/**
	 * 连续击杀次数限制。
	 * Chain-kill number restriction.
	 */
	@Property(key = "gameserver.pvp.chainkill.number.restriction", defaultValue = "30")
	public static int CHAIN_KILL_NUMBER_RESTRICTION;
	/**
	 * 允许获得奖励的最大等级差。
	 * Max authorized level difference for PvP rewards.
	 */
	@Property(key = "gameserver.pvp.max.leveldiff.restriction", defaultValue = "9")
	public static int MAX_AUTHORIZED_LEVEL_DIFF;

	/**
	 * 是否启用 PvP 勋章奖励。
	 * Whether PvP medal rewards are enabled.
	 */
	@Property(key = "gameserver.pvp.medal.rewarding.enable", defaultValue = "false")
	public static boolean ENABLE_MEDAL_REWARDING;
	/**
	 * PvP 勋章奖励触发概率。
	 * Chance to grant PvP medal reward.
	 */
	@Property(key = "gameserver.pvp.medal.reward.chance", defaultValue = "10")
	public static float MEDAL_REWARD_CHANCE;
	/**
	 * Genocide 特殊奖励类型。
	 * Special reward type for Genocide spree.
	 */
	@Property(key = "gameserver.pvp.special_reward.type", defaultValue = "0")
	public static int GENOCIDE_SPECIAL_REWARDING;
	/**
	 * 特殊奖励触发概率。
	 * Chance to grant special PvP reward.
	 */
	@Property(key = "gameserver.pvp.special_reward.chance", defaultValue = "2")
	public static float SPECIAL_REWARD_CHANCE;
}
