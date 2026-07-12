package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.ZorshivDredgionService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 启动或停止 Zorshiv Dredgion 活动的管理员命令。
 * Admin command to start or stop Zorshiv Dredgion events.
 */
public class ZorshivDredgion extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";

	/**
	 * 构造 zorshiv 命令。
	 * Creates the zorshiv command.
	 */
	public ZorshivDredgion() {
		super("zorshiv");
	}

	/**
	 * 按 start/stop 与地点 ID 控制 Zorshiv Dredgion。
	 * Controls Zorshiv Dredgion by start/stop and location id.
	 *
	 * 执行 GM / Admin player
	 * start|stop &lt;Id&gt;。
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
	 * 处理 start/stop 分支。
	 * Handles start/stop branches.
	 *
	 * 执行 GM / Admin player
	 * Command parameters
	 */
	protected void handleStartStop(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int zorshivDredgionId = NumberUtils.toInt(params[1]);
		if (!isValidZorshivDredgionLocationId(player, zorshivDredgionId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.zorshivDredgionService().isZorshivDredgionInProgress(zorshivDredgionId)) {
				PacketSendUtility.sendMessage(player, "<Zorshiv Dredgion> " + zorshivDredgionId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<Zorshiv Dredgion> " + zorshivDredgionId + " started!");
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys3Message(player, "\uE050", "The <Zorshiv Dredgion> to lands at levinshor !!!");
					}
				});
				GameLocationBootstrapServices.zorshivDredgionService().startZorshivDredgion(zorshivDredgionId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.zorshivDredgionService().isZorshivDredgionInProgress(zorshivDredgionId)) {
				PacketSendUtility.sendMessage(player, "<Zorshiv Dredgion> " + zorshivDredgionId + " is not start!");
			} else {
				PacketSendUtility.sendMessage(player, "<Zorshiv Dredgion> " + zorshivDredgionId + " stopped!");
				GameLocationBootstrapServices.zorshivDredgionService().stopZorshivDredgion(zorshivDredgionId);
			}
		}
	}

	/**
	 * 校验 Zorshiv Dredgion 地点 ID 是否存在。
	 * Validates that the Zorshiv Dredgion location id exists.
	 *
	 * 执行 GM / Admin player
	 * Location id
	 *
	 * @return 若 valid 则为 true / True if valid
	 */
	protected boolean isValidZorshivDredgionLocationId(Player player, int zorshivDredgionId) {
		if (!GameLocationBootstrapServices.zorshivDredgionService().getZorshivDredgionLocations().keySet().contains(zorshivDredgionId)) {
			PacketSendUtility.sendMessage(player, "Id " + zorshivDredgionId + " is invalid");
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
		PacketSendUtility.sendMessage(player, "AdminCommand //zorshiv start|stop <Id>");
	}
}
