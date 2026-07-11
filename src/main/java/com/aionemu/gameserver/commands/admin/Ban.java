package com.aionemu.gameserver.commands.admin;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 管理员封禁命令：按账号/IP/全量封禁玩家。
 * Admin ban command: bans a player by account, IP, or full.
 *
 * @author Watson
 */
public class Ban extends AdminCommand {

	/**
	 * 注册 {@code //ban} 命令。
	 * Registers the {@code //ban} command.
	 */
	public Ban() {
		super("ban");
	}

	/**
	 * 执行封禁：解析目标与类型后通知登录服。
	 * Executes ban: resolves target and type, then notifies the login server.
	 *
	 * admin
	 * @param params 参数：玩家名、类型、时长 / player name, type, duration
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(admin, "Syntax: //ban <player> [account|ip|full] [time in minutes]");
			return;
		}

		// 需要获取玩家账号 ID / We need to get player's account ID
		String name = Util.convertName(params[0]);
		int accountId = 0;
		String accountIp = "";

		// 首先尝试在世界中查找玩家 / First, try to find player in the World
		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(name);
		if (player != null) {
			accountId = player.getClientConnection().getAccount().getId();
			accountIp = player.getClientConnection().getIP();
		}

		// 其次，尝试从数据库获取离线玩家账号 ID / Second, try to get account ID of offline player from database
		if (accountId == 0)
			accountId = DAOManager.getDAO(PlayerDAO.class).getAccountIdByName(name);

		// 第三，失败 / Third, fail
		if (accountId == 0) {
			PacketSendUtility.sendMessage(admin, "Player " + name + " was not found!");
			PacketSendUtility.sendMessage(admin, "Syntax: //ban <player> [account|ip|full] [time in minutes]");
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
				PacketSendUtility.sendMessage(admin, "Syntax: //ban <player> [account|ip|full] [time in minutes]");
				return;
			}
		}

		int time = 0; // Default: infinity
		if (params.length > 2) {
			try {
				time = Integer.parseInt(params[2]);
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "Syntax: //ban <player> [account|ip|full] [time in minutes]");
				return;
			}
		}

		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendBanPacket(type, accountId, accountIp, time, admin.getObjectId());
	}

	/**
	 * 参数错误时输出 {@code //ban} 用法。
	 * Prints {@code //ban} usage on invalid arguments.
	 *
	 * admin
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //ban <player> [account|ip|full] [time in minutes]");
	}
}