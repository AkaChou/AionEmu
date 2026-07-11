package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 角色跨服转移相关配置。
 * Player server-transfer related configuration.
 *
 * @author KID
 */
public class PlayerTransferConfig {
	/**
	 * 转移允许携带的最大基纳。
	 * Maximum kinah allowed during transfer.
	 */
	@Property(key = "ptransfer.max.kinah", defaultValue = "0")
	public static long MAX_KINAH;

	/**
	 * 天族转移后绑定点坐标。
	 * Elyos bind point after transfer.
	 */
	@Property(key = "ptransfer.bindpoint.elyos", defaultValue = "210010000 1212.9423 1044.8516 140.75568 32")
	public static String BIND_ELYOS;

	/**
	 * 魔族转移后绑定点坐标。
	 * Asmodian bind point after transfer.
	 */
	@Property(key = "ptransfer.bindpoint.asmo", defaultValue = "220010000 571.0388 2787.3420 299.8750 32")
	public static String BIND_ASMO;

	/**
	 * 是否转移表情数据。
	 * Whether emotions are transferred.
	 */
	@Property(key = "ptransfer.allow.emotions", defaultValue = "true")
	public static boolean ALLOW_EMOTIONS;

	/**
	 * 是否转移动作数据。
	 * Whether motions are transferred.
	 */
	@Property(key = "ptransfer.allow.motions", defaultValue = "true")
	public static boolean ALLOW_MOTIONS;

	/**
	 * 是否转移宏数据。
	 * Whether macros are transferred.
	 */
	@Property(key = "ptransfer.allow.macro", defaultValue = "true")
	public static boolean ALLOW_MACRO;

	/**
	 * 是否转移 NPC 阵营声望。
	 * Whether NPC factions are transferred.
	 */
	@Property(key = "ptransfer.allow.npcfactions", defaultValue = "true")
	public static boolean ALLOW_NPCFACTIONS;

	/**
	 * 是否转移宠物数据。
	 * Whether pets are transferred.
	 */
	@Property(key = "ptransfer.allow.pets", defaultValue = "true")
	public static boolean ALLOW_PETS;

	/**
	 * 是否转移配方数据。
	 * Whether recipes are transferred.
	 */
	@Property(key = "ptransfer.allow.recipes", defaultValue = "true")
	public static boolean ALLOW_RECIPES;

	/**
	 * 是否转移技能数据。
	 * Whether skills are transferred.
	 */
	@Property(key = "ptransfer.allow.skills", defaultValue = "true")
	public static boolean ALLOW_SKILLS;

	/**
	 * 是否转移称号数据。
	 * Whether titles are transferred.
	 */
	@Property(key = "ptransfer.allow.titles", defaultValue = "true")
	public static boolean ALLOW_TITLES;

	/**
	 * 是否转移任务数据。
	 * Whether quests are transferred.
	 */
	@Property(key = "ptransfer.allow.quests", defaultValue = "true")
	public static boolean ALLOW_QUESTS;

	/**
	 * 是否转移背包物品。
	 * Whether inventory is transferred.
	 */
	@Property(key = "ptransfer.allow.inventory", defaultValue = "true")
	public static boolean ALLOW_INV;

	/**
	 * 是否转移仓库物品。
	 * Whether warehouse is transferred.
	 */
	@Property(key = "ptransfer.allow.warehouse", defaultValue = "true")
	public static boolean ALLOW_WAREHOUSE;

	/**
	 * 是否转移印记数据。
	 * Whether stigma data is transferred.
	 */
	@Property(key = "ptransfer.allow.stigma", defaultValue = "true")
	public static boolean ALLOW_STIGMA;

	/**
	 * 是否禁止使用与原角色相同的名称。
	 * Whether same character name is blocked after transfer.
	 */
	@Property(key = "ptransfer.block.samename", defaultValue = "false")
	public static boolean BLOCK_SAMENAME;

	/**
	 * 转移后角色名称前缀。
	 * Name prefix applied after server transfer.
	 */
	@Property(key = "ptransfer.server.name.prefix", defaultValue = "_UNK")
	public static String NAME_PREFIX;

	/**
	 * 再次转移冷却时间（小时）。
	 * Hours before re-transfer is allowed.
	 */
	@Property(key = "ptransfer.retransfer.hours", defaultValue = "0")
	public static int REUSE_HOURS;

	/**
	 * 转移时移除的技能列表。
	 * Skill list removed during transfer.
	 */
	@Property(key = "ptransfer.remove.skills.list", defaultValue = "*")
	public static String REMOVE_SKILL_LIST;
}
