package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 权限撤销指令；通过登录服将在线玩家的 accesslevel 或 membership 置零。
 * Admin command that clears an online player's accesslevel or membership via the login server.
 *
 * @author Cyrakuse
 * @modified By Aionchs-Wylovech
 */
public class Revoke extends AdminCommand {

	public Revoke() {
		super("revoke");
	}

	/**
	 * 执行该管理指令。
	 * Executes this admin command.
	 *
	 * @param admin 执行指令的管理员 / admin executing the command
	 * command arguments
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length != 2) {
			PacketSendUtility.sendMessage(admin, "syntax //revoke <characterName> <accesslevel | membership>");
			return;
		}

		int type = 0;
		if (params[1].toLowerCase().equals("accesslevel")) {
			type = 1;
		}
		else if (params[1].toLowerCase().equals("membership")) {
			type = 2;
		}
		else {
			PacketSendUtility.sendMessage(admin, "syntax //revoke <characterName> <accesslevel | membership>");
			return;
		}

		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));
		if (player == null) {
			PacketSendUtility.sendMessage(admin, "The specified player is not online.");
			return;
		}
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendLsControlPacket(player.getAcountName(), player.getName(), admin.getName(), 0, type);
	}

	/**
	 * 参数错误时输出用法。
	 * Prints usage when arguments are invalid.
	 *
	 * @param player 接收提示的玩家 / player receiving the message
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //revoke <characterName> <accesslevel | membership>");
	}
}