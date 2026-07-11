package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.IuService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 管理员 IU 演唱会活动命令：按地点 ID 启动或停止 Concert。
 * Admin IU concert event command: start or stop by location id.
 */
public class Iu extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";

	public Iu() {
		super("iu");
	}

	/**
	 * 分发 start/stop 子命令。
	 * Dispatch start/stop subcommands.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * @param params 子命令与演唱会地点 ID / Subcommand and concert location id
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			showHelp(player);
			return;
		} if (COMMAND_STOP.equalsIgnoreCase(params[0]) || COMMAND_START.equalsIgnoreCase(params[0])) {
			handleStartStopConcert(player, params);
		}
	}

	/**
	 * 启动或停止指定 Id 的演唱会。
	 * Start or stop concert for the given location id.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * @param params start|stop and location id。 / start|stop and location id
	 */
	protected void handleStartStopConcert(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int iuId = NumberUtils.toInt(params[1]);
		if (!isValidConcertLocationId(player, iuId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.iuService().isConcertInProgress(iuId)) {
				PacketSendUtility.sendMessage(player, "<Concert> " + iuId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<Concert> " + iuId + " started!");
				GameLocationBootstrapServices.iuService().startConcert(iuId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.iuService().isConcertInProgress(iuId)) {
				PacketSendUtility.sendMessage(player, "<Concert> " + iuId + " is not start!");
			} else {
				PacketSendUtility.sendMessage(player, "<Concert> " + iuId + " stopped!");
				GameLocationBootstrapServices.iuService().stopConcert(iuId);
			}
		}
	}

	/**
	 * 校验演唱会地点 ID 是否有效。
	 * Validate whether the concert location id exists.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * @param iuId 演唱会地点 ID / Concert location id
	 * @return 若 valid 则为 true / True if valid
	 */
	protected boolean isValidConcertLocationId(Player player, int iuId) {
		if (!GameLocationBootstrapServices.iuService().getIuLocations().keySet().contains(iuId)) {
			PacketSendUtility.sendMessage(player, "Id " + iuId + " is invalid");
			return false;
		}
		return true;
	}

	/**
	 * 显示命令帮助。
	 * Show command help.
	 *
	 * @param player 接收提示的玩家 / Player receiving the hint
	 */
	protected void showHelp(Player player) {
		PacketSendUtility.sendMessage(player, "AdminCommand //iu start|stop <Id>");
	}
}
