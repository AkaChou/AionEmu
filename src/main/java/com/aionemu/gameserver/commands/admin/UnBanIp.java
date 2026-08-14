package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 解除 IP 封禁的管理员命令。
 * Admin command to lift an IP ban.
 *
 * @author Watson
 */
public class UnBanIp extends AdminCommand {

	/**
	 * 构造 unbanip 命令。
	 * Creates the unbanip command.
	 */
	public UnBanIp() {
		super("unbanip");
	}

	/**
	 * 向登录服发送 IP 解封请求。
	 * Sends an IP unban request to the login server.
	 *
	 * @param player 执行 GM / Admin player
	 * @param params &lt;mask&gt;。
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(player, "Syntax: //unbanip <mask>");
			return;
		}

		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendBanPacket((byte) 2, 0, params[0], -1, player.getObjectId());
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //unbanip <mask>");
	}
}
