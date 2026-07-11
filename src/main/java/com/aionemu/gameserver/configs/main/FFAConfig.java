package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * FFA（自由混战）活动相关配置。
 * Free-for-all (FFA) event related configuration.
 *
 * @author Rinzler (Encom)
 */
public class FFAConfig {

	/**
	 * 是否启用 FFA 模式。
	 * Whether FFA mode is enabled.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.enabled", defaultValue = "true")
	public static boolean FFA_ENABLED;
	/**
	 * 连杀奖励通行币数量。
	 * Toll quantity rewarded for FFA spree.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree.toll.quantity", defaultValue = "10")
	public static int FFA_SPREE_REWARD_TOLL_QUANTITY;
	/**
	 * 连杀奖励道具 ID。
	 * Item ID rewarded for FFA spree.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree.reward.item", defaultValue = "166030005")
	public static int FFA_SPREE_REWARD_ITEM;

	/**
	 * FFA 连杀公告文案 1。
	 * FFA killing spree announce message 1.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree1", defaultValue = " <is now on a Killing Spree> !")
	public static String FFA_SPREE_1;
	/**
	 * FFA 连杀公告文案 2。
	 * FFA killing spree announce message 2.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree2", defaultValue = " <is now on Rampage> !")
	public static String FFA_SPREE_2;
	/**
	 * FFA 连杀公告文案 3。
	 * FFA killing spree announce message 3.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree3", defaultValue = " <is now Dominating> !")
	public static String FFA_SPREE_3;
	/**
	 * FFA 连杀公告文案 4。
	 * FFA killing spree announce message 4.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree4", defaultValue = " <Unstoppable> !")
	public static String FFA_SPREE_4;
	/**
	 * FFA 连杀公告文案 5。
	 * FFA killing spree announce message 5.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree5", defaultValue = " <CHUUCHUU MUTHAFAKAAASS> !")
	public static String FFA_SPREE_5;
	/**
	 * FFA 连杀公告文案 6。
	 * FFA killing spree announce message 6.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree6", defaultValue = " <is now Getting Crazzyyy> !")
	public static String FFA_SPREE_6;
	/**
	 * FFA 连杀公告文案 7。
	 * FFA killing spree announce message 7.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree7", defaultValue = " <is now GODLIKE> !")
	public static String FFA_SPREE_7;
	/**
	 * FFA 连杀公告文案 8。
	 * FFA killing spree announce message 8.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree8", defaultValue = " <is now on WICKED SICKKKKKK> !")
	public static String FFA_SPREE_8;
	/**
	 * FFA 连杀公告文案 9。
	 * FFA killing spree announce message 9.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree9", defaultValue = " <Really knows how to kill players> !")
	public static String FFA_SPREE_9;
	/**
	 * FFA 连杀公告文案 10。
	 * FFA killing spree announce message 10.
	 */
	@Property(key = "gameserver.pvp.mod.ffa.spree10", defaultValue = " <IS NOW A TRUE PVP FIGHTER> !")
	public static String FFA_SPREE_10;
}
