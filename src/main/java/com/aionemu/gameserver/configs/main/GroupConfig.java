package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 队伍与联盟相关配置。
 * Player group and alliance related configuration.
 */
public class GroupConfig {

	/**
	 * 离队后移除计时（秒）。
	 * Group member remove time in seconds.
	 */
	@Property(key = "gameserver.playergroup.removetime", defaultValue = "600")
	public static int GROUP_REMOVE_TIME;

	/**
	 * 队伍最大有效距离。
	 * Maximum group distance.
	 */
	@Property(key = "gameserver.playergroup.maxdistance", defaultValue = "100")
	public static int GROUP_MAX_DISTANCE;

	/**
	 * 是否允许邀请对立阵营加入队伍。
	 * Whether inviting the other faction to group is allowed.
	 */
	@Property(key = "gameserver.group.inviteotherfaction", defaultValue = "false")
	public static boolean GROUP_INVITEOTHERFACTION;

	/**
	 * 离盟后移除计时（秒）。
	 * Alliance member remove time in seconds.
	 */
	@Property(key = "gameserver.playeralliance.removetime", defaultValue = "600")
	public static int ALLIANCE_REMOVE_TIME;

	/**
	 * 是否允许邀请对立阵营加入联盟。
	 * Whether inviting the other faction to alliance is allowed.
	 */
	@Property(key = "gameserver.playeralliance.inviteotherfaction", defaultValue = "false")
	public static boolean ALLIANCE_INVITEOTHERFACTION;
}
