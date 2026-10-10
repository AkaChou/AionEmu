package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 队伍与联盟相关配置，支持 Spring Boot ConfigurationProperties 与静态门面访问。
 * Player group and alliance related configuration supporting Spring Boot ConfigurationProperties and static facade access.
 */
@Component
@ConfigurationProperties(prefix = "gameserver")
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

	private final PlayergroupProperties playergroup = new PlayergroupProperties();
	private final GroupProperties group = new GroupProperties();
	private final PlayerallianceProperties playeralliance = new PlayerallianceProperties();

	public PlayergroupProperties getPlayergroup() {
		return playergroup;
	}

	public GroupProperties getGroup() {
		return group;
	}

	public PlayerallianceProperties getPlayeralliance() {
		return playeralliance;
	}

	public static class PlayergroupProperties {
		public int getRemovetime() {
			return GROUP_REMOVE_TIME;
		}

		public void setRemovetime(int removetime) {
			GROUP_REMOVE_TIME = removetime;
		}

		public int getMaxdistance() {
			return GROUP_MAX_DISTANCE;
		}

		public void setMaxdistance(int maxdistance) {
			GROUP_MAX_DISTANCE = maxdistance;
		}
	}

	public static class GroupProperties {
		public boolean isInviteotherfaction() {
			return GROUP_INVITEOTHERFACTION;
		}

		public void setInviteotherfaction(boolean inviteotherfaction) {
			GROUP_INVITEOTHERFACTION = inviteotherfaction;
		}
	}

	public static class PlayerallianceProperties {
		public int getRemovetime() {
			return ALLIANCE_REMOVE_TIME;
		}

		public void setRemovetime(int removetime) {
			ALLIANCE_REMOVE_TIME = removetime;
		}

		public boolean isInviteotherfaction() {
			return ALLIANCE_INVITEOTHERFACTION;
		}

		public void setInviteotherfaction(boolean inviteotherfaction) {
			ALLIANCE_INVITEOTHERFACTION = inviteotherfaction;
		}
	}
}
