package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

import java.util.concurrent.Future;

/**
 * 管理员禁言命令：对玩家施加聊天禁言，可指定分钟数并在到期后自动解除。
 * Admin gag command: mute a player's chat, optionally for a number of minutes with auto-ungag.
 *
 * @author Watson
 */
public class Gag extends AdminCommand {

	public Gag() {
		super("gag");
	}

	/**
	 * 对指定在线玩家禁言；可选时长（分钟），到期自动解除。
	 * Gag the named online player; optional duration in minutes, auto-ungag when expired.
	 *
	 * @param admin 执行命令的管理员 / Admin executing the command
	 * @param params 玩家名与可选禁言分钟数 / Player name and optional minutes
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(admin, "Syntax: //gag <player> [time in minutes]");
			return;
		}

		String name = Util.convertName(params[0]);
		final Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(name);
		if (player == null) {
			PacketSendUtility.sendMessage(admin, "Player " + name + " was not found!");
			PacketSendUtility.sendMessage(admin, "Syntax: //gag <player> [time in minutes]");
			return;
		}

		int time = 0;
		if (params.length > 1) {
			try {
				time = Integer.parseInt(params[1]);
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "Syntax: //gag <player> [time in minutes]");
				return;
			}
		}

		player.setGagged(true);
		if (time != 0) {
			Future<?> task = player.getController().getTask(TaskId.GAG);
			if (task != null)
				player.getController().cancelTask(TaskId.GAG);
			player.getController().addTask(TaskId.GAG, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

				@Override
				public void run() {
					player.setGagged(false);
					PacketSendUtility.sendMessage(player, "You have been ungagged");
				}
			}, time * 60000L));
		}
		PacketSendUtility.sendMessage(player, "You have been gagged" + (time != 0 ? " for " + time + " minutes" : ""));

		PacketSendUtility.sendMessage(admin, "Player " + name + " gagged" + (time != 0 ? " for " + time + " minutes" : ""));
	}

	/**
	 * 参数错误时显示命令语法。
	 * Show command syntax on invalid arguments.
	 *
	 * @param player 接收提示的玩家 / Player receiving the hint
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //gag <player> [time in minutes]");
	}
}
