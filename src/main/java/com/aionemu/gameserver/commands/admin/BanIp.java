package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员 IP 封禁命令：按 IP 掩码封禁并通知登录服。
 * Admin IP-ban command: bans an IP mask and notifies the login server.
 *
 * @author Watson
 */
public class BanIp extends AdminCommand {

	/**
	 * 注册 {@code //banip} 命令。
	 * Registers the {@code //banip} command.
	 */
	public BanIp() {
		super("banip");
	}

	/**
	 * 执行 IP 封禁：解析掩码与时长后发送封禁包。
	 * Executes IP ban: parses mask and duration, then sends the ban packet.
	 *
	 * @param params 参数：IP 掩码、时长（分钟） / ip mask, duration in minutes
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(player, "Syntax: //banip <mask> [time in minutes]");
			return;
		}

		String mask = params[0];

		int time = 0; // Default: infinity
		if (params.length > 1) {
			try {
				time = Integer.parseInt(params[1]);
			}
			catch (NumberFormatException e) {
				onFail(player, e.getMessage());
				return;
			}
		}

		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendBanPacket((byte) 2, 0, mask, time, player.getObjectId());
	}

	/**
	 * 参数错误时输出 {@code //banip} 用法。
	 * Prints {@code //banip} usage on invalid arguments.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //banip <mask> [time in minutes]");
	}
}