package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameServerNetworkServices;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 解除 MAC 地址封禁的管理员命令。
 * Admin command to lift a MAC-address ban.
 *
 * @author KID
 */
public class UnBanMac extends AdminCommand {

	/**
	 * 构造 unbanmac 命令。
	 * Creates the unbanmac command.
	 */
	public UnBanMac() {
		super("unbanmac");
	}

	/**
	 * 按 MAC 地址解除封禁。
	 * Unbans the given MAC address.
	 *
	 * @param player 执行 GM / Admin player
	 * @param params &lt;mac&gt;。
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			onFail(player, null);
			return;
		}

		String address = params[0];
		boolean result = GameServerNetworkServices.bannedMacManager().unbanAddress(address,
			"uban;mac=" + address + ", " + player.getObjectId() + "; admin=" + player.getName());
		if (result)
			PacketSendUtility.sendMessage(player, "mac " + address + " has unbanned");
		else
			PacketSendUtility.sendMessage(player, "mac " + address + " is not banned");
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //unbanmac <mac>");
	}
}
