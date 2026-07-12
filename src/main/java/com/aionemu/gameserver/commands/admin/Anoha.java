package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.AnohaService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 管理员阿诺哈活动命令：启动或停止指定阿诺哈地点。
 * Admin Anoha event command: starts or stops a specified Anoha location.
 */
public class Anoha extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";
	
	/**
	 * 注册 {@code //anoha} 命令。
	 * Registers the {@code //anoha} command.
	 */
	public Anoha() {
		super("anoha");
	}
	
	/**
	 * 执行阿诺哈控制：解析 start/stop 与地点 ID。
	 * Executes Anoha control: parses start/stop and location id.
	 *
	 * admin
	 * start|stop, location id。
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
	 * 处理阿诺哈活动的开始/停止逻辑。
	 * Handles start/stop logic for an Anoha event.
	 *
	 * admin
	 * @param params 参数：动作与地点 ID / action and location id
	 */
	protected void handleStartStop(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int anohaId = NumberUtils.toInt(params[1]);
		if (!isValidAnohaLocationId(player, anohaId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.anohaService().isAnohaInProgress(anohaId)) {
				PacketSendUtility.sendMessage(player, "<Anoha> " + anohaId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<Anoha> " + anohaId + " started!");
				GameLocationBootstrapServices.anohaService().startAnoha(anohaId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.anohaService().isAnohaInProgress(anohaId)) {
				PacketSendUtility.sendMessage(player, "<Anoha> " + anohaId + " is not start!");
			} else {
				PacketSendUtility.sendMessage(player, "<Anoha> " + anohaId + " stopped!");
				GameLocationBootstrapServices.anohaService().stopAnoha(anohaId);
			}
		}
	}
	
	/**
	 * 校验阿诺哈地点 ID 是否有效。
	 * Validates whether the Anoha location id exists.
	 *
	 * admin
	 * location id
	 *
	 * @return {@code true} if valid。
	 */
	protected boolean isValidAnohaLocationId(Player player, int anohaId) {
		if (!GameLocationBootstrapServices.anohaService().getAnohaLocations().keySet().contains(anohaId)) {
			PacketSendUtility.sendMessage(player, "Id " + anohaId + " is invalid");
			return false;
		}
		return true;
	}
	
	/**
	 * 向管理员输出 {@code //anoha} 用法。
	 * Sends {@code //anoha} usage help to the admin.
	 *
	 * admin
	 */
	protected void showHelp(Player player) {
		PacketSendUtility.sendMessage(player, "AdminCommand //anoha start|stop <Id>");
	}
}