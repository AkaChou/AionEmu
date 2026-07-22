package com.aionemu.gameserver.configs.administration;

import com.aionemu.commons.configuration.Property;

/**
 * 管理员 / GM 权限与外观相关配置。
 * GM privilege and appearance related configuration.
 */
public class AdminConfig {

	/**
	 * 可使用 GM 特殊技能的最低权限等级。
	 * Minimum access level required for GM special skills.
	 */
	@Property(key = "gameserver.administration.gmskills", defaultValue = "3")
	public static int GM_SPECIAL_SKILLS;

	/**
	 * 判定为 GM 的最低权限等级。
	 * Minimum access level treated as GM.
	 */
	@Property(key = "gameserver.administration.gmlevel", defaultValue = "3")
	public static int GM_LEVEL;

	/**
	 * 可打开 GM 面板的最低权限等级。
	 * Minimum access level required to open the GM panel.
	 */
	@Property(key = "gameserver.administration.gmpanel", defaultValue = "3")
	public static int GM_PANEL;

	/**
	 * 可自由飞行的最低权限等级。
	 * Minimum access level required for free flight.
	 */
	@Property(key = "gameserver.administration.flight.freefly", defaultValue = "3")
	public static int GM_FLIGHT_FREE;

	/**
	 * 可无限飞行的最低权限等级。
	 * Minimum access level required for unlimited flight.
	 */
	@Property(key = "gameserver.administration.flight.unlimited", defaultValue = "3")
	public static int GM_FLIGHT_UNLIMITED;

	/**
	 * 可强制开门的最低权限等级。
	 * Minimum access level required to open doors forcibly.
	 */
	@Property(key = "gameserver.administration.doors.opening", defaultValue = "3")
	public static int DOORS_OPEN;

	/**
	 * 管理员死亡后自动复活的最低权限等级。
	 * Minimum access level for admin auto-resurrection.
	 */
	@Property(key = "gameserver.administration.auto.res", defaultValue = "3")
	public static int ADMIN_AUTO_RES;

	/**
	 * 可查看其他玩家详情的最低权限等级。
	 * Minimum access level to view other players' details.
	 */
	@Property(key = "gameserver.administration.view.player", defaultValue = "3")
	public static int ADMIN_VIEW_DETAILS;

	/**
	 * GM 登录时是否默认隐身。
	 * Whether GMs connect in invisible mode by default.
	 */
	@Property(key = "gameserver.administration.invis.gm.connection", defaultValue = "false")
	public static boolean INVISIBLE_GM_CONNECTION;

	/**
	 * GM 登录时的敌对模式（如 Normal）。
	 * Enmity mode applied when a GM connects (e.g. Normal).
	 */
	@Property(key = "gameserver.administration.enemity.gm.connection", defaultValue = "Normal")
	public static String ENEMITY_MODE_GM_CONNECTION;

	/**
	 * GM 登录时是否默认无敌。
	 * Whether GMs connect invulnerable by default.
	 */
	@Property(key = "gameserver.administration.invul.gm.connection", defaultValue = "false")
	public static boolean INVULNERABLE_GM_CONNECTION;

	/**
	 * GM 登录时是否默认开启特视。
	 * Whether GMs connect with special vision enabled by default.
	 */
	@Property(key = "gameserver.administration.vision.gm.connection", defaultValue = "false")
	public static boolean VISION_GM_CONNECTION;

	/**
	 * GM 登录时是否默认拒收密语。
	 * Whether GMs connect with whisper blocked by default.
	 */
	@Property(key = "gameserver.administration.whisper.gm.connection", defaultValue = "false")
	public static boolean WHISPER_GM_CONNECTION;

	/**
	 * GM 登录时是否默认进入 GM 模式。
	 * Whether GMs connect in GM mode by default.
	 */
	@Property(key = "gameserver.administration.gm.mode.connection", defaultValue = "false")
	public static boolean GM_MODE_CONNECTION;

	/**
	 * 是否启用管理员交易物品限制。
	 * Whether admin trade-item restriction is enabled.
	 */
	@Property(key = "gameserver.administration.trade.item.restriction", defaultValue = "false")
	public static boolean ENABLE_TRADEITEM_RESTRICTION;

	/**
	 * 是否启用管理员名称前缀标签。
	 * Whether admin name tags are enabled.
	 */
	@Property(key = "gameserver.admin.tag.enable", defaultValue = "true")
	public static boolean ADMIN_TAG_ENABLE;

	/**
	 * 权限等级 1 的名称前缀（%s 为角色名）。
	 * Name tag for access level 1 (%s = character name).
	 */
	@Property(key = "gameserver.admin.tag.1", defaultValue = "<Support> %s")
	public static String ADMIN_TAG_1;

	/**
	 * 权限等级 2 的名称前缀（%s 为角色名）。
	 * Name tag for access level 2 (%s = character name).
	 */
	@Property(key = "gameserver.admin.tag.2", defaultValue = "<Jr-GM> %s")
	public static String ADMIN_TAG_2;

	/**
	 * 权限等级 3 的名称前缀（%s 为角色名）。
	 * Name tag for access level 3 (%s = character name).
	 */
	@Property(key = "gameserver.admin.tag.3", defaultValue = "<GM> %s")
	public static String ADMIN_TAG_3;

	/**
	 * 权限等级 4 的名称前缀（%s 为角色名）。
	 * Name tag for access level 4 (%s = character name).
	 */
	@Property(key = "gameserver.admin.tag.4", defaultValue = "<Head-GM> %s")
	public static String ADMIN_TAG_4;

	/**
	 * 权限等级 5 的名称前缀（%s 为角色名）。
	 * Name tag for access level 5 (%s = character name).
	 */
	@Property(key = "gameserver.admin.tag.5", defaultValue = "<Admin> %s")
	public static String ADMIN_TAG_5;

	/**
	 * 登录时全服通告的权限等级列表（* 表示全部）。
	 * Access levels announced on login (* means all).
	 */
	@Property(key = "gameserver.admin.announce.levels", defaultValue = "*")
	public static String ANNOUNCE_LEVEL_LIST;
}
