package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.BeritraService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 贝里特拉入侵启停管理命令（{@code //beritra}）。
 * Admin command to start or stop Beritra invasions ({@code //beritra}).
 */
public class Beritra extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";

	/**
	 * 注册命令名为 {@code beritra}。
	 * Registers the command name {@code beritra}.
	 */
	public Beritra() {
		super("beritra");
	}

	/**
	 * 执行贝里特拉入侵启停。
	 * Executes Beritra invasion start/stop.
	 *
	 * @param params 参数：start|stop 与地点 ID / start|stop and location id
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			showHelp(player);
			return;
		} if (COMMAND_STOP.equalsIgnoreCase(params[0]) || COMMAND_START.equalsIgnoreCase(params[0])) {
			handleStartStopInvasion(player, params);
		}
	}

	/**
	 * 处理指定地点的入侵开始或停止。
	 * Handles starting or stopping invasion at a location.
	 *
	 */
	protected void handleStartStopInvasion(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int beritraId = NumberUtils.toInt(params[1]);
		if (!isValidBeritraLocationId(player, beritraId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.beritraService().isInvasionInProgress(beritraId)) {
				PacketSendUtility.sendMessage(player, "<Beritra Location> " + beritraId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<Beritra Location> " + beritraId + " started!");
				GameLocationBootstrapServices.beritraService().startBeritraInvasion(beritraId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.beritraService().isInvasionInProgress(beritraId)) {
				PacketSendUtility.sendMessage(player, "<Beritra Location> " + beritraId + " is not start!");

			} else {
				PacketSendUtility.sendMessage(player, "<Beritra Location> " + beritraId + " stopped!");
				GameLocationBootstrapServices.beritraService().stopBeritraInvasion(beritraId);
			}
		}
	}

	/**
	 * 校验贝里特拉地点 ID 是否有效。
	 * Validates whether the Beritra location id is valid.
	 *
	 *
	 * @return 若 valid 则为 true / true if valid
	 */
	protected boolean isValidBeritraLocationId(Player player, int beritraId) {
		if (!GameLocationBootstrapServices.beritraService().getBeritraLocations().keySet().contains(beritraId)) {
			PacketSendUtility.sendMessage(player, "Id " + beritraId + " is invalid");
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
		PacketSendUtility.sendMessage(player, "AdminCommand //beritra start|stop <Id>");
	}
}
