package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员重置命令：通过 {@code //reset instance} 重置执行者自己的副本（包括个人副本、个人单独进入的组队副本及当前队伍副本）。
 * Admin reset command: {@code //reset instance} resets the executor's instances (personal, solo-entered group, or current team instances).
 */
public class Reset extends AdminCommand {

	private static final String INSTANCE_SUBCOMMAND = "instance";

	/**
	 * 注册 {@code //reset} 命令。
	 * Registers the {@code //reset} command.
	 */
	public Reset() {
		super("reset");
	}

	/**
	 * 处理 {@code instance} 子命令；副本内玩家会被传送到对应出口。
	 * Handles the {@code instance} subcommand; players inside are moved to the matching exit.
	 * @param player 执行命令的管理员 / admin executing the command
	 * @param params 子命令参数 / subcommand arguments
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length != 1 || !INSTANCE_SUBCOMMAND.equalsIgnoreCase(params[0])) {
			PacketSendUtility.sendMessage(player, "Usage: //reset instance");
			return;
		}
		int resetCount = InstanceService.resetPlayerInstances(player);
		if (resetCount == 0) {
			PacketSendUtility.sendMessage(player, "No active instances owned by you or your team were found.");
			return;
		}
		PacketSendUtility.sendMessage(player, "Reset " + resetCount + " instance(s) owned by you or your team.");
	}
}
