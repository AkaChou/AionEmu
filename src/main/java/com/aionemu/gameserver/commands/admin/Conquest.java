package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.ConquestService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 征服活动启停管理命令（{@code //conquest}）。
 * Admin command to start or stop Conquest events ({@code //conquest}).
 */
public class Conquest extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";

	/**
	 * 注册命令名为 {@code conquest}。
	 * Registers the command name {@code conquest}.
	 */
	public Conquest() {
		super("conquest");
	}

	/**
	 * 执行征服活动启停。
	 * Executes Conquest start/stop.
	 *
	 * @param params 参数：start|stop 与地点 ID / start|stop and location id
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			showHelp(player);
			return;
		} if (COMMAND_STOP.equalsIgnoreCase(params[0]) || COMMAND_START.equalsIgnoreCase(params[0])) {
			handleStartStop(player, params);
		}
	}

	/**
	 * 处理指定地点的征服活动开始或停止。
	 * Handles starting or stopping Conquest at a location.
	 *
	 */
	protected void handleStartStop(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int conquestId = NumberUtils.toInt(params[1]);
		if (!isValidConquestLocationId(player, conquestId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.conquestService().isConquestInProgress(conquestId)) {
				PacketSendUtility.sendMessage(player, "<Conquest> " + conquestId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<Conquest> " + conquestId + " started!");
				GameLocationBootstrapServices.conquestService().startConquest(conquestId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.conquestService().isConquestInProgress(conquestId)) {
				PacketSendUtility.sendMessage(player, "<Conquest> " + conquestId + " is not start!");
			} else {
				PacketSendUtility.sendMessage(player, "<Conquest> " + conquestId + " stopped!");
				GameLocationBootstrapServices.conquestService().stopConquest(conquestId);
			}
		}
	}

	/**
	 * 校验征服地点 ID 是否有效。
	 * Validates whether the Conquest location id is valid.
	 *
	 *
	 * @return 若 valid 则为 true / true if valid
	 */
	protected boolean isValidConquestLocationId(Player player, int conquestId) {
		if (!GameLocationBootstrapServices.conquestService().getConquestLocations().keySet().contains(conquestId)) {
			PacketSendUtility.sendMessage(player, "Id " + conquestId + " is invalid");
			return false;
		}
		return true;
	}

	/**
	 * 显示命令用法帮助。
	 * Shows command usage help.
	 *
	 */
	protected void showHelp(Player player) {
		PacketSendUtility.sendMessage(player, "AdminCommand //conquest start|stop <Id>");
	}
}
