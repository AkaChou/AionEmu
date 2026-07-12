package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameRuntimeServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.FindGroupService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 清理组队/联盟/找队缓存的管理命令（{@code //clear}）。
 * Admin command that clears group, alliance or find-group caches ({@code //clear}).
 *
 * @author KID
 */
public class Clear extends AdminCommand {

	/**
	 * 注册命令名为 {@code clear}。
	 * Registers the command name {@code clear}.
	 */
	public Clear() {
		super("clear");
	}

	/**
	 * 按类型清理缓存：groups、allys 或 findgroup。
	 * Clears caches by type: groups, allys or findgroup.
	 *
	 * admin
	 * groups|allys|findgroup。
	 */
	@Override
	public void execute(Player admin, String... params) {
		if(params[0].equalsIgnoreCase("groups")) {
			PacketSendUtility.sendMessage(admin, "Not implemented, if need this - pm to AT");
		}
		else if(params[0].equalsIgnoreCase("allys")) {
			PacketSendUtility.sendMessage(admin, "Not implemented, if need this - pm to AT");
		}
		else if(params[0].equalsIgnoreCase("findgroup")){
			GameRuntimeServices.findGroupService().clean();
		}
	}

	/**
	 * 执行失败时的语法提示。
	 * Syntax hint on failure.
	 *
	 * admin
	 * error message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "<usage //clear groups | allys | findgroup");
	}
}
