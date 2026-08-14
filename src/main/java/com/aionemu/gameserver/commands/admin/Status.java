package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team2.alliance.PlayerAllianceService;
import com.aionemu.gameserver.model.team2.group.PlayerGroupService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 查询队伍/联盟服务状态的管理员命令。
 * Admin command to dump party or alliance service status.
 *
 * @author KID
 */
public class Status extends AdminCommand {

	/**
	 * 构造 status 命令。
	 * Creates the status command.
	 */
	public Status() {
		super("status");
	}

	/**
	 * 输出联盟或队伍服务状态字符串。
	 * Prints alliance or group service status string.
	 *
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params[0].equalsIgnoreCase("alliance")) {
			PacketSendUtility.sendMessage(admin, PlayerAllianceService.getServiceStatus());
		}
		else if (params[0].equalsIgnoreCase("group")) {
			PacketSendUtility.sendMessage(admin, PlayerGroupService.getServiceStatus());
		}
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "<usage //status alliance | group");
	}
}
