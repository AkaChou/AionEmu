package com.aionemu.gameserver.commands.admin;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 解除账号/IP 封禁的管理员命令。
 * Admin command to lift account or IP bans.
 *
 * @author Watson
 */
public class UnBan extends AdminCommand {

	/**
	 * 构造 unban 命令。
	 * Creates the unban command.
	 */
	public UnBan() {
		super("unban");
	}

	/**
	 * 按角色名查找账号并对账号/IP/全量解除封禁。
	 * Resolves account by character name and unbans account/IP/full.
	 *
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(admin, "Syntax: //unban <player> [account|ip|full]");
			return;
		}

		// 被封禁玩家须离线，故从数据库取其账号 ID / Banned player must be offline, so get his account ID from database
		String name = Util.convertName(params[0]);
		int accountId = DAOManager.getDAO(PlayerDAO.class).getAccountIdByName(name);
		if (accountId == 0) {
			PacketSendUtility.sendMessage(admin, "Player " + name + " was not found!");
			PacketSendUtility.sendMessage(admin, "Syntax: //unban <player> [account|ip|full]");
			return;
		}

		byte type = 3; // Default: full
		if (params.length > 1) {
			// 智能匹配 / Smart Matching
			String stype = params[1].toLowerCase();
			if (("account").startsWith(stype))
				type = 1;
			else if (("ip").startsWith(stype))
				type = 2;
			else if (("full").startsWith(stype))
				type = 3;
			else {
				PacketSendUtility.sendMessage(admin, "Syntax: //unban <player> [account|ip|full]");
				return;
			}
		}

		// 发送时间 -1 以解封 / Sends time -1 to unban
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendBanPacket(type, accountId, "", -1, admin.getObjectId());
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //unban <player> [account|ip|full]");
	}
}
