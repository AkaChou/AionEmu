package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.DynamicRiftService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 动态裂隙启停管理命令（{@code //dynamicrift}）。
 * Admin command to start or stop Dynamic Rift events ({@code //dynamicrift}).
 */
public class DynamicRift extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";

	/**
	 * 注册命令名为 {@code dynamicrift}。
	 * Registers the command name {@code dynamicrift}.
	 */
	public DynamicRift() {
		super("dynamicrift");
	}

	/**
	 * 执行动态裂隙启停。
	 * Executes Dynamic Rift start/stop.
	 *
	 * @param params 参数：start|stop 与地点 ID / start|stop and location id
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			showHelp(player);
			return;
		} if (COMMAND_STOP.equalsIgnoreCase(params[0]) || COMMAND_START.equalsIgnoreCase(params[0])) {
			handleStartStopDynamic(player, params);
		}
	}

	/**
	 * 处理指定地点的动态裂隙开始或停止。
	 * Handles starting or stopping Dynamic Rift at a location.
	 *
	 */
	protected void handleStartStopDynamic(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int dynamicRiftId = NumberUtils.toInt(params[1]);
		if (!isValidDynamicRiftLocationId(player, dynamicRiftId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.dynamicRiftService().isDynamicRiftInProgress(dynamicRiftId)) {
				PacketSendUtility.sendMessage(player, "<Dynamic Rift> " + dynamicRiftId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<Dynamic Rift> " + dynamicRiftId + " started!");
				GameLocationBootstrapServices.dynamicRiftService().startDynamicRift(dynamicRiftId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.dynamicRiftService().isDynamicRiftInProgress(dynamicRiftId)) {
				PacketSendUtility.sendMessage(player, "<Dynamic Rift> " + dynamicRiftId + " is not start!");
			} else {
				PacketSendUtility.sendMessage(player, "<Dynamic Rift> " + dynamicRiftId + " stopped!");
				GameLocationBootstrapServices.dynamicRiftService().stopDynamicRift(dynamicRiftId);
			}
		}
	}

	/**
	 * 校验动态裂隙地点 ID 是否有效。
	 * Validates whether the Dynamic Rift location id is valid.
	 *
	 *
	 * @return 若 valid 则为 true / true if valid
	 */
	protected boolean isValidDynamicRiftLocationId(Player player, int dynamicRiftId) {
		if (!GameLocationBootstrapServices.dynamicRiftService().getDynamicRiftLocations().keySet().contains(dynamicRiftId)) {
			PacketSendUtility.sendMessage(player, "Id " + dynamicRiftId + " is invalid");
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
		PacketSendUtility.sendMessage(player, "AdminCommand //dynamicrift start|stop <Id>");
	}
}
