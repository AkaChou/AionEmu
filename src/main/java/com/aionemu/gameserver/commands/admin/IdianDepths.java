package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.IdianDepthsService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 管理员伊迪安深渊活动命令：按地点 ID 启动或停止 Idian Depths。
 * Admin Idian Depths event command: start or stop by location id.
 */
public class IdianDepths extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";

	public IdianDepths() {
		super("idian");
	}

	/**
	 * 分发 start/stop 子命令。
	 * Dispatch start/stop subcommands.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * @param params 子命令与地点 ID / Subcommand and location id
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			showHelp(player);
			return;
		} if (COMMAND_STOP.equalsIgnoreCase(params[0]) || COMMAND_START.equalsIgnoreCase(params[0])) {
			handleStartStopIdian(player, params);
		}
	}

	/**
	 * 启动或停止指定 Id 的伊迪安深渊活动。
	 * Start or stop Idian Depths for the given location id.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * @param params start|stop and location id。
	 */
	protected void handleStartStopIdian(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int idianDepthsId = NumberUtils.toInt(params[1]);
		if (!isValidIdianDepthsLocationId(player, idianDepthsId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.idianDepthsService().isIdianDepthsInProgress(idianDepthsId)) {
				PacketSendUtility.sendMessage(player, "<Idian Depths> " + idianDepthsId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<Idian Depths> " + idianDepthsId + " started!");
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys3Message(player, "", "<Idian Depths> is now open !!!");
					}
				});
				GameLocationBootstrapServices.idianDepthsService().startIdianDepths(idianDepthsId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.idianDepthsService().isIdianDepthsInProgress(idianDepthsId)) {
				PacketSendUtility.sendMessage(player, "<Idian Depths> " + idianDepthsId + " is not start!");
			} else {
				PacketSendUtility.sendMessage(player, "<Idian Depths> " + idianDepthsId + " stopped!");
				GameLocationBootstrapServices.idianDepthsService().stopIdianDepths(idianDepthsId);
			}
		}
	}

	/**
	 * 校验伊迪安深渊地点 ID 是否有效。
	 * Validate whether the Idian Depths location id exists.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 *
	 * @return 若 valid 则为 true / True if valid
	 */
	protected boolean isValidIdianDepthsLocationId(Player player, int idianDepthsId) {
		if (!GameLocationBootstrapServices.idianDepthsService().getIdianDepthsLocations().keySet().contains(idianDepthsId)) {
			PacketSendUtility.sendMessage(player, "Id " + idianDepthsId + " is invalid");
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
		PacketSendUtility.sendMessage(player, "AdminCommand //idian start|stop <Id>");
	}
}
