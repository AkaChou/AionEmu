package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.configs.main.MembershipConfig;

/**
 * 玩家展示标签域。
 * Player display-tag domain.
 *
 * <p>该类型只服务 {@link Player}：根据账号会员配置、特殊账号名和管理员访问等级生成展示标签。
 * 公开门面仍保留在 {@link Player}，此处为无状态静态策略。
 * This type only serves {@link Player}: it builds display tags from membership configuration,
 * special account names and admin access levels. The public facade stays on {@link Player};
 * this is a stateless static policy.</p>
 */
final class PlayerTags {

	private PlayerTags() {
	}

	/**
	 * 返回玩家自定义展示标签。
	 * Returns the player's custom display tag.
	 *
	 * @param player 玩家 / player
	 * @param isForChatCommands 是否用于聊天命令 / whether the tag is for chat commands
	 * @return 展示标签 / display tag
	 */
	static String getCustomTag(Player player, boolean isForChatCommands) {
		String customTag = getAccountTag(player) != "%s" ? getAccountTag(player) : getAccessTag(player);
		String customTagForChatCommands = customTag != "%s"
				? customTag.substring(0, customTag.indexOf("%"))
				: "";
		return isForChatCommands ? customTagForChatCommands : customTag;
	}

	private static String getAccessTag(Player player) {
		String accessTag = "%s";
		switch (player.getClientConnection().getAccount().getAccessLevel()) {
		case 1:
			accessTag = AdminConfig.ADMIN_TAG_1;
			break;
		case 2:
			accessTag = AdminConfig.ADMIN_TAG_2;
			break;
		case 3:
			accessTag = AdminConfig.ADMIN_TAG_3;
			break;
		case 4:
			accessTag = AdminConfig.ADMIN_TAG_4;
			break;
		case 5:
			accessTag = AdminConfig.ADMIN_TAG_5;
			break;
		default:
			accessTag = "%s";
		}
		return accessTag;
	}

	private static String getAccountTag(Player player) {
		String accountName = player.getClientConnection().getAccount().getName();
		String accountTag = "%s";
		if (accountName.equalsIgnoreCase("Aion")) {
			accountTag = MembershipConfig.PLAYER_TAG_30;
		}
		if (accountName.equalsIgnoreCase("5.8")) {
			accountTag = MembershipConfig.PLAYER_TAG_34;
		}
		return accountTag;
	}
}
