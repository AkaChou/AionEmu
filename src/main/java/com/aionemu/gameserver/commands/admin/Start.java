package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 简单的 start 管理员命令（占位/测试）。
 * test).
 */
public class Start extends AdminCommand
{
	/**
	 * 构造 start 命令。
	 * Creates the start command.
	 */
	public Start() {
		super("start");
	}

	/**
	 * 向执行者发送确认消息。
	 * Sends a confirmation message to the invoker.
	 *
	 * 执行 GM / Admin player
	 * Unused
	 */
	@Override
	public void execute(Player player, String... params) {
		PacketSendUtility.sendMessage(player, "Owned !!!");
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 *
	 * 玩家 / Player
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //start ");
	}
}
