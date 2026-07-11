package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 制作与采集相关配置。
 * Crafting and gathering related configuration.
 */
public class CraftConfig {
	/**
	 * 是否允许制作技能不受等级限制提升。
	 * Whether craft skills can level up without level restrictions.
	 */
	@Property(key = "gameserver.craft.skills.unrestricted.levelup.enable", defaultValue = "false")
	public static boolean UNABLE_CRAFT_SKILLS_UNRESTRICTED_LEVELUP;
	/**
	 * 专家制作技能数量上限。
	 * Maximum expert crafting skills.
	 */
	@Property(key = "gameserver.craft.max.expert.skills", defaultValue = "2")
	public static int MAX_EXPERT_CRAFTING_SKILLS;
	/**
	 * 大师制作技能数量上限。
	 * Maximum master crafting skills.
	 */
	@Property(key = "gameserver.craft.max.master.skills", defaultValue = "1")
	public static int MAX_MASTER_CRAFTING_SKILLS;
	/**
	 * 普通玩家制作暴击率。
	 * Craft critical rate for regular players.
	 */
	@Property(key = "gameserver.craft.critical.rate.regular", defaultValue = "15")
	public static int CRAFT_CRIT_RATE;
	/**
	 * 高级会员制作暴击率。
	 * Craft critical rate for premium members.
	 */
	@Property(key = "gameserver.craft.critical.rate.premium", defaultValue = "15")
	public static int PREMIUM_CRAFT_CRIT_RATE;
	/**
	 * VIP 会员制作暴击率。
	 * Craft critical rate for VIP members.
	 */
	@Property(key = "gameserver.craft.critical.rate.vip", defaultValue = "15")
	public static int VIP_CRAFT_CRIT_RATE;
	/**
	 * 普通玩家制作连击率。
	 * Craft combo rate for regular players.
	 */
	@Property(key = "gameserver.craft.combo.rate.regular", defaultValue = "25")
	public static int CRAFT_COMBO_RATE;
	/**
	 * 高级会员制作连击率。
	 * Craft combo rate for premium members.
	 */
	@Property(key = "gameserver.craft.combo.rate.premium", defaultValue = "25")
	public static int PREMIUM_CRAFT_COMBO_RATE;
	/**
	 * VIP 会员制作连击率。
	 * Craft combo rate for VIP members.
	 */
	@Property(key = "gameserver.craft.combo.rate.vip", defaultValue = "25")
	public static int VIP_CRAFT_COMBO_RATE;
	/**
	 * 是否启用制作任务检查（硬编码默认关闭）。
	 * Whether craft task check is enabled (hardcoded default false).
	 */
	public static boolean CRAFT_CHECK_TASK = false;
	/**
	 * 紫色暴击概率。
	 * Chance for purple craft critical.
	 */
	@Property(key = "gameserver.craft.chance.purple.crit", defaultValue = "1")
	public static int CRAFT_CHANCE_PURPLE_CRIT;
	/**
	 * 蓝色暴击概率。
	 * Chance for blue craft critical.
	 */
	@Property(key = "gameserver.craft.chance.blue.crit", defaultValue = "10")
	public static int CRAFT_CHANCE_BLUE_CRIT;
	/**
	 * 瞬间完成概率。
	 * Chance for instant craft completion.
	 */
	@Property(key = "gameserver.craft.chance.instant", defaultValue = "100")
	public static int CRAFT_CHANCE_INSTANT;
	/**
	 * 是否启用采集保护。
	 * Whether gathering protection is enabled.
	 */
	@Property(key = "gameserver.protection.gather.enable", defaultValue = "true")
	public static boolean PROTECTION_GATHER_ENABLE;
}
