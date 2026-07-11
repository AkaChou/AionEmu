package com.aionemu.gameserver.configs.main;

import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.commons.configuration.Property;

/**
 * 军团创建、升级、仓库与邀请相关配置。
 * Legion creation, upgrade, warehouse and invite related configuration.
 */
@Slf4j
public class LegionConfig {
	/**
	 * 军团名称正则表达式。
	 * Regular expression for legion names.
	 */
	@Property(key = "gameserver.legion.pattern", defaultValue = "[a-zA-Z ]{2,32}")
	public static Pattern LEGION_NAME_PATTERN;
	/**
	 * 军团自我介绍正则表达式。
	 * Regular expression for legion self-intro text.
	 */
	@Property(key = "gameserver.legion.self.intro.pattern", defaultValue = ".{1,32}")
	public static Pattern SELF_INTRO_PATTERN;
	/**
	 * 军团昵称正则表达式。
	 * Regular expression for legion nicknames.
	 */
	@Property(key = "gameserver.legion.nick.name.pattern", defaultValue = ".{1,10}")
	public static Pattern NICKNAME_PATTERN;
	/**
	 * 军团公告正则表达式。
	 * Regular expression for legion announcements.
	 */
	@Property(key = "gameserver.legion.announcement.pattern", defaultValue = ".{1,256}")
	public static Pattern ANNOUNCEMENT_PATTERN;
	/**
	 * 军团解散等待时间（秒）。
	 * Legion disband wait time in seconds.
	 */
	@Property(key = "gameserver.legion.disband.time", defaultValue = "86400")
	public static int LEGION_DISBAND_TIME;
	/**
	 * 创建军团所需基纳。
	 * Kinah required to create a legion.
	 */
	@Property(key = "gameserver.legion.creation.required.kinah", defaultValue = "10000")
	public static int LEGION_CREATE_REQUIRED_KINAH;
	/**
	 * 军团徽记所需基纳。
	 * Kinah required for a legion emblem.
	 */
	@Property(key = "gameserver.legion.emblem.required.kinah", defaultValue = "10000")
	public static int LEGION_EMBLEM_REQUIRED_KINAH;
	/**
	 * 是否启用公会任务升级要求。
	 * Whether guild task requirements are enabled for upgrades.
	 */
	@Property(key = "gameserver.legion.task.requirement.enable", defaultValue = "true")
	public static boolean ENABLE_GUILD_TASK_REQ;
	/**
	 * 升至 2 级所需基纳。
	 * Kinah required to reach legion level 2.
	 */
	@Property(key = "gameserver.legion.level2.required.kinah", defaultValue = "100000")
	public static int LEGION_LEVEL2_REQUIRED_KINAH;
	/**
	 * 升至 3 级所需基纳。
	 * Kinah required to reach legion level 3.
	 */
	@Property(key = "gameserver.legion.level3.required.kinah", defaultValue = "1000000")
	public static int LEGION_LEVEL3_REQUIRED_KINAH;
	/**
	 * 升至 4 级所需基纳。
	 * Kinah required to reach legion level 4.
	 */
	@Property(key = "gameserver.legion.level4.required.kinah", defaultValue = "2000000")
	public static int LEGION_LEVEL4_REQUIRED_KINAH;
	/**
	 * 升至 5 级所需基纳。
	 * Kinah required to reach legion level 5.
	 */
	@Property(key = "gameserver.legion.level5.required.kinah", defaultValue = "6000000")
	public static int LEGION_LEVEL5_REQUIRED_KINAH;
	/**
	 * 升至 6 级所需基纳。
	 * Kinah required to reach legion level 6.
	 */
	@Property(key = "gameserver.legion.level6.required.kinah", defaultValue = "50000000")
	public static int LEGION_LEVEL6_REQUIRED_KINAH;
	/**
	 * 升至 7 级所需基纳。
	 * Kinah required to reach legion level 7.
	 */
	@Property(key = "gameserver.legion.level7.required.kinah", defaultValue = "75000000")
	public static int LEGION_LEVEL7_REQUIRED_KINAH;
	/**
	 * 升至 8 级所需基纳。
	 * Kinah required to reach legion level 8.
	 */
	@Property(key = "gameserver.legion.level8.required.kinah", defaultValue = "100000000")
	public static int LEGION_LEVEL8_REQUIRED_KINAH;
	/**
	 * 升至 2 级所需成员数。
	 * Members required to reach legion level 2.
	 */
	@Property(key = "gameserver.legion.level2.required.members", defaultValue = "10")
	public static int LEGION_LEVEL2_REQUIRED_MEMBERS;
	/**
	 * 升至 3 级所需成员数。
	 * Members required to reach legion level 3.
	 */
	@Property(key = "gameserver.legion.level3.required.members", defaultValue = "20")
	public static int LEGION_LEVEL3_REQUIRED_MEMBERS;
	/**
	 * 升至 4 级所需成员数。
	 * Members required to reach legion level 4.
	 */
	@Property(key = "gameserver.legion.level4.required.members", defaultValue = "30")
	public static int LEGION_LEVEL4_REQUIRED_MEMBERS;
	/**
	 * 升至 5 级所需成员数。
	 * Members required to reach legion level 5.
	 */
	@Property(key = "gameserver.legion.level5.required.members", defaultValue = "40")
	public static int LEGION_LEVEL5_REQUIRED_MEMBERS;
	/**
	 * 升至 6 级所需成员数。
	 * Members required to reach legion level 6.
	 */
	@Property(key = "gameserver.legion.level6.required.members", defaultValue = "50")
	public static int LEGION_LEVEL6_REQUIRED_MEMBERS;
	/**
	 * 升至 7 级所需成员数。
	 * Members required to reach legion level 7.
	 */
	@Property(key = "gameserver.legion.level7.required.members", defaultValue = "60")
	public static int LEGION_LEVEL7_REQUIRED_MEMBERS;
	/**
	 * 升至 8 级所需成员数。
	 * Members required to reach legion level 8.
	 */
	@Property(key = "gameserver.legion.level8.required.members", defaultValue = "70")
	public static int LEGION_LEVEL8_REQUIRED_MEMBERS;
	/**
	 * 升至 2 级所需贡献值。
	 * Contribution required to reach legion level 2.
	 */
	@Property(key = "gameserver.legion.level2.required.contribution", defaultValue = "0")
	public static int LEGION_LEVEL2_REQUIRED_CONTRIBUTION;
	/**
	 * 升至 3 级所需贡献值。
	 * Contribution required to reach legion level 3.
	 */
	@Property(key = "gameserver.legion.level3.required.contribution", defaultValue = "20000")
	public static int LEGION_LEVEL3_REQUIRED_CONTRIBUTION;
	/**
	 * 升至 4 级所需贡献值。
	 * Contribution required to reach legion level 4.
	 */
	@Property(key = "gameserver.legion.level4.required.contribution", defaultValue = "100000")
	public static int LEGION_LEVEL4_REQUIRED_CONTRIBUTION;
	/**
	 * 升至 5 级所需贡献值。
	 * Contribution required to reach legion level 5.
	 */
	@Property(key = "gameserver.legion.level5.required.contribution", defaultValue = "500000")
	public static int LEGION_LEVEL5_REQUIRED_CONTRIBUTION;
	/**
	 * 升至 6 级所需贡献值。
	 * Contribution required to reach legion level 6.
	 */
	@Property(key = "gameserver.legion.level6.required.contribution", defaultValue = "25000000")
	public static int LEGION_LEVEL6_REQUIRED_CONTRIBUTION;
	/**
	 * 升至 7 级所需贡献值。
	 * Contribution required to reach legion level 7.
	 */
	@Property(key = "gameserver.legion.level7.required.contribution", defaultValue = "12500000")
	public static int LEGION_LEVEL7_REQUIRED_CONTRIBUTION;
	/**
	 * 升至 8 级所需贡献值。
	 * Contribution required to reach legion level 8.
	 */
	@Property(key = "gameserver.legion.level8.required.contribution", defaultValue = "62500000")
	public static int LEGION_LEVEL8_REQUIRED_CONTRIBUTION;
	/**
	 * 1 级军团最大成员数。
	 * Max members for legion level 1.
	 */
	@Property(key = "gameserver.legion.level1.max.members", defaultValue = "30")
	public static int LEGION_LEVEL1_MAX_MEMBERS;
	/**
	 * 2 级军团最大成员数。
	 * Max members for legion level 2.
	 */
	@Property(key = "gameserver.legion.level2.max.members", defaultValue = "60")
	public static int LEGION_LEVEL2_MAX_MEMBERS;
	/**
	 * 3 级军团最大成员数。
	 * Max members for legion level 3.
	 */
	@Property(key = "gameserver.legion.level3.max.members", defaultValue = "90")
	public static int LEGION_LEVEL3_MAX_MEMBERS;
	/**
	 * 4 级军团最大成员数。
	 * Max members for legion level 4.
	 */
	@Property(key = "gameserver.legion.level4.max.members", defaultValue = "120")
	public static int LEGION_LEVEL4_MAX_MEMBERS;
	/**
	 * 5 级军团最大成员数。
	 * Max members for legion level 5.
	 */
	@Property(key = "gameserver.legion.level5.max.members", defaultValue = "150")
	public static int LEGION_LEVEL5_MAX_MEMBERS;
	/**
	 * 6 级军团最大成员数。
	 * Max members for legion level 6.
	 */
	@Property(key = "gameserver.legion.level6.max.members", defaultValue = "180")
	public static int LEGION_LEVEL6_MAX_MEMBERS;
	/**
	 * 7 级军团最大成员数。
	 * Max members for legion level 7.
	 */
	@Property(key = "gameserver.legion.level7.max.members", defaultValue = "210")
	public static int LEGION_LEVEL7_MAX_MEMBERS;
	/**
	 * 8 级军团最大成员数。
	 * Max members for legion level 8.
	 */
	@Property(key = "gameserver.legion.level8.max.members", defaultValue = "240")
	public static int LEGION_LEVEL8_MAX_MEMBERS;
	/**
	 * 是否启用军团仓库。
	 * Whether legion warehouse is enabled.
	 */
	@Property(key = "gameserver.legion.warehouse", defaultValue = "true")
	public static boolean LEGION_WAREHOUSE;
	/**
	 * 是否允许邀请其他阵营成员。
	 * Whether inviting members of the other faction is allowed.
	 */
	@Property(key = "gameserver.legion.invite.other.faction", defaultValue = "false")
	public static boolean LEGION_INVITEOTHERFACTION;
	/**
	 * 1 级军团仓库格数。
	 * Legion warehouse slots at level 1.
	 */
	@Property(key = "gameserver.legion.warehouse.level1.slots", defaultValue = "24")
	public static int LWH_LEVEL1_SLOTS;
	/**
	 * 2 级军团仓库格数。
	 * Legion warehouse slots at level 2.
	 */
	@Property(key = "gameserver.legion.warehouse.level2.slots", defaultValue = "32")
	public static int LWH_LEVEL2_SLOTS;
	/**
	 * 3 级军团仓库格数。
	 * Legion warehouse slots at level 3.
	 */
	@Property(key = "gameserver.legion.warehouse.level3.slots", defaultValue = "40")
	public static int LWH_LEVEL3_SLOTS;
	/**
	 * 4 级军团仓库格数。
	 * Legion warehouse slots at level 4.
	 */
	@Property(key = "gameserver.legion.warehouse.level4.slots", defaultValue = "48")
	public static int LWH_LEVEL4_SLOTS;
	/**
	 * 5 级军团仓库格数。
	 * Legion warehouse slots at level 5.
	 */
	@Property(key = "gameserver.legion.warehouse.level5.slots", defaultValue = "56")
	public static int LWH_LEVEL5_SLOTS;
	/**
	 * 6 级军团仓库格数。
	 * Legion warehouse slots at level 6.
	 */
	@Property(key = "gameserver.legion.warehouse.level6.slots", defaultValue = "64")
	public static int LWH_LEVEL6_SLOTS;
	/**
	 * 7 级军团仓库格数。
	 * Legion warehouse slots at level 7.
	 */
	@Property(key = "gameserver.legion.warehouse.level7.slots", defaultValue = "72")
	public static int LWH_LEVEL7_SLOTS;
	/**
	 * 8 级军团仓库格数。
	 * Legion warehouse slots at level 8.
	 */
	@Property(key = "gameserver.legion.warehouse.level8.slots", defaultValue = "80")
	public static int LWH_LEVEL8_SLOTS;
}
