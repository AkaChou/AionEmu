package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

import java.util.concurrent.Future;

/**
 * 解除玩家禁言的管理员命令。
 * Admin command to remove a player's gag.
 *
 * @author Watson
 */
public class UnGag extends AdminCommand {

	/**
	 * 构造 ungag 命令。
	 * Creates the ungag command.
	 */
	public UnGag() {
		super("ungag");
	}

	/**
	 * 取消目标玩家禁言状态与定时任务。
	 * Clears gag flag and cancels gag task on the target player.
	 *
	 * @param admin 执行 GM / Admin player
	 * @param params &lt;player&gt;。 / &lt;player&gt;
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(admin, "Syntax: //ungag <player>");
			return;
		}

		String name = Util.convertName(params[0]);
		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(name);
		if (player == null) {
			PacketSendUtility.sendMessage(admin, "Player " + name + " was not found!");
			PacketSendUtility.sendMessage(admin, "Syntax: //ungag <player>");
			return;
		}

		player.setGagged(false);
		Future<?> task = player.getController().getTask(TaskId.GAG);
		if (task != null)
			player.getController().cancelTask(TaskId.GAG);
		PacketSendUtility.sendMessage(player, "You have been ungagged");

		PacketSendUtility.sendMessage(admin, "Player " + name + " ungagged");
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 *
	 * 玩家 / Player
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //ungag <player>");
	}
}
