package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.loginserver.LoginServer;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 提升在线玩家访问等级或会员等级的管理员命令。
 * Admin command to promote an online player's access level or membership.
 *
 * @author Cyrakuse
 * @modified By Aionchs-Wylovech
 */
public class Promote extends AdminCommand {

	/**
	 * 以别名 {@code promote} 构造命令。
	 * Construct the command with alias {@code promote}.
	 */
	public Promote() {
		super("promote");
	}

	/**
	 * 向登录服发送 LS 控制包以设置 accesslevel（0–5）或 membership（0–10）。
	 * Send an LS control packet to set accesslevel (0–5) or membership (0–10).
	 *
	 * @param admin 执行 GM / Admin player
	 * @param params 角色名、类型与等级掩码 / Character name, type and level mask
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length != 3) {
			PacketSendUtility.sendMessage(admin, "syntax //promote <characterName> <accesslevel | membership> <mask> ");
			return;
		}

		int mask = 0;
		try {
			mask = Integer.parseInt(params[2]);
		}
		catch (NumberFormatException e) {
			PacketSendUtility.sendMessage(admin, "Only number!");
			return;
		}

		int type = 0;
		if (params[1].toLowerCase().equals("accesslevel")) {
			type = 1;
			if (mask > 5 || mask < 0) {
				PacketSendUtility.sendMessage(admin, "accesslevel can be 0 - 5");
				return;
			}
		}
		else if (params[1].toLowerCase().equals("membership")) {
			type = 2;
			if (mask > 10 || mask < 0) {
				PacketSendUtility.sendMessage(admin, "membership can be 0 - 10");
				return;
			}
		}
		else {
			PacketSendUtility.sendMessage(admin, "syntax //promote <characterName> <accesslevel | membership> <mask>");
			return;
		}

		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));
		if (player == null) {
			PacketSendUtility.sendMessage(admin, "The specified player is not online.");
			return;
		}
		com.aionemu.gameserver.lifecycle.GameServerNetworkServices.loginServer().sendLsControlPacket(player.getAcountName(), player.getName(), admin.getName(), mask, type);

	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 * 玩家 / Player
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //promote <characterName> <accesslevel | membership> <mask> ");
	}
}
