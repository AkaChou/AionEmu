package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.RvrService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 阵营对战（RvR）管理指令；按地点 ID 启动或停止 RvR 活动。
 * Admin command that starts or stops race-versus-race events by location ID.
 */
public class Rvr extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";
	
	public Rvr() {
		super("rvr");
	}
	
	/**
	 * 执行该管理指令。
	 * Executes this admin command.
	 *
	 * @param player 执行指令的管理员 / admin executing the command
	 * command arguments
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			showHelp(player);
			return;
		} if (COMMAND_STOP.equalsIgnoreCase(params[0]) || COMMAND_START.equalsIgnoreCase(params[0])) {
			handleStartStopRvr(player, params);
		}
	}
	
	protected void handleStartStopRvr(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int rvrId = NumberUtils.toInt(params[1]);
		if (!isValidRvrLocationId(player, rvrId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.rvrService().isRvrInProgress(rvrId)) {
				PacketSendUtility.sendMessage(player, "<R.v.r> " + rvrId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<R.v.r> " + rvrId + " started!");
				GameLocationBootstrapServices.rvrService().startRvr(rvrId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.rvrService().isRvrInProgress(rvrId)) {
				PacketSendUtility.sendMessage(player, "<R.v.r> " + rvrId + " is not start!");
			} else {
				PacketSendUtility.sendMessage(player, "<R.v.r> " + rvrId + " stopped!");
				GameLocationBootstrapServices.rvrService().stopRvr(rvrId);
			}
		}
	}
	
	protected boolean isValidRvrLocationId(Player player, int rvrId) {
		if (!GameLocationBootstrapServices.rvrService().getRvrLocations().keySet().contains(rvrId)) {
			PacketSendUtility.sendMessage(player, "Id " + rvrId + " is invalid");
			return false;
		}
		return true;
	}
	
	protected void showHelp(Player player) {
		PacketSendUtility.sendMessage(player, "AdminCommand //rvr start|stop <Id>");
	}
}