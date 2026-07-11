package com.aionemu.gameserver.utils.chathandlers;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 玩家聊天命令，基于权限位鉴权。
 * Player chat command authorized via permission bits.
 *
 * @author synchro2
 */
public abstract class PlayerCommand extends ChatCommand {

	/**
	 * 以给定别名构造玩家命令。
	 * Construct a player command with the given alias.
	 *
	 * @param alias 命令别名 / Command alias
	 */
	public PlayerCommand(String alias) {
		super(alias);
	}

	/**
	 * 校验玩家是否拥有所需权限。
	 * Check whether the player has the required permission.
	 *
	 * @param player 玩家 / Player
	 * @return 有权限则为 true / True if allowed
	 */
	@Override
	public boolean checkLevel(Player player) {
		return player.havePermission(getLevel());
	}

	/**
	 * 处理玩家命令：鉴权后拆分参数并执行。
	 * Process a player command: authorize, split args and execute.
	 *
	 * 玩家 / Player
	 * @param text 去掉前缀后的命令文本 / Command text without prefix
	 * @return 是否已处理 / Whether handled
	 */
	@Override
	boolean process(Player player, String text) {
		if (!checkLevel(player)) {
			PacketSendUtility.sendMessage(player, "You not have permission for use this command.");
			return true;
		}

		boolean success = false;
		if (text.length() == getAlias().length()) {
			success = this.run(player, EMPTY_PARAMS);
		} else {
			success = this.run(player, text.substring(getAlias().length() + 1).split(" "));
		}
		return success;
	}
}
