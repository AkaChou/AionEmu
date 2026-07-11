package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.SvsService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 启动或停止 S.v.s 活动地点的管理员命令。
 * Admin command to start or stop S.v.s event locations.
 */
public class Svs extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";

	/**
	 * 构造 svs 命令。
	 * Creates the svs command.
	 */
	public Svs() {
		super("svs");
	}

	/**
	 * 按 start/stop 与地点 ID 控制 S.v.s。
	 * Controls S.v.s by start/stop and location id.
	 *
	 * 执行 GM / Admin player
	 * start|stop &lt;Id&gt;。 / start|stop &lt;Id&gt;
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			showHelp(player);
			return;
		} if (COMMAND_STOP.equalsIgnoreCase(params[0]) || COMMAND_START.equalsIgnoreCase(params[0])) {
			handleStartStopSvs(player, params);
		}
	}

	/**
	 * 处理 start/stop 分支。
	 * Handles start/stop branches.
	 *
	 * 执行 GM / Admin player
	 * Command parameters
	 */
	protected void handleStartStopSvs(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int svsId = NumberUtils.toInt(params[1]);
		if (!isValidSvsLocationId(player, svsId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.svsService().isSvsInProgress(svsId)) {
				PacketSendUtility.sendMessage(player, "<S.v.s> " + svsId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<S.v.s> " + svsId + " started!");
				GameLocationBootstrapServices.svsService().startSvs(svsId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.svsService().isSvsInProgress(svsId)) {
				PacketSendUtility.sendMessage(player, "<S.v.s> " + svsId + " is not start!");
			} else {
				PacketSendUtility.sendMessage(player, "<S.v.s> " + svsId + " stopped!");
				GameLocationBootstrapServices.svsService().stopSvs(svsId);
			}
		}
	}

	/**
	 * 校验 S.v.s 地点 ID 是否存在。
	 * Validates that the S.v.s location id exists.
	 *
	 * 执行 GM / Admin player
	 * Location id
	 *
	 * @return 若 valid 则为 true / True if valid
	 */
	protected boolean isValidSvsLocationId(Player player, int svsId) {
		if (!GameLocationBootstrapServices.svsService().getSvsLocations().keySet().contains(svsId)) {
			PacketSendUtility.sendMessage(player, "Id " + svsId + " is invalid");
			return false;
		}
		return true;
	}

	/**
	 * 显示用法帮助。
	 * Shows usage help.
	 *
	 * @param player 执行 GM / Admin player
	 */
	protected void showHelp(Player player) {
		PacketSendUtility.sendMessage(player, "AdminCommand //svs start|stop <Id>");
	}
}
