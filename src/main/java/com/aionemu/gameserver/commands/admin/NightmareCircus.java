package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.NightmareCircusService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 噩梦马戏团（Nightmare Circus）活动启停管理员命令。
 * Admin command to start or stop Nightmare Circus events.
 */
public class NightmareCircus extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";

	/**
	 * 以别名 {@code circus} 构造命令。
	 * Construct the command with alias {@code circus}.
	 */
	public NightmareCircus() {
		super("circus");
	}

	/**
	 * 执行启停：无参数时显示帮助；{@code start|stop <Id>} 时切换对应马戏团活动。
	 * Execute start/stop: show help with no args; toggle the circus event for {@code start|stop <Id>}.
	 *
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			showHelp(player);
			return;
		} if (COMMAND_STOP.equalsIgnoreCase(params[0]) || COMMAND_START.equalsIgnoreCase(params[0])) {
			handleStartStopInstance(player, params);
		}
	}

	/**
	 * 按地点 ID 启动或停止马戏团活动，并向全服广播开始消息。
	 * Start or stop a Nightmare Circus event by location id and broadcast the start notice.
	 *
	 * @param player 执行 GM / Admin player
	 * @param params {@code start|stop} and location id。
	 */
	protected void handleStartStopInstance(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int nightmareId = NumberUtils.toInt(params[1]);
		if (!isValidNightmareCircusLocationId(player, nightmareId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.nightmareCircusService().isNightmareCircusInProgress(nightmareId)) {
				PacketSendUtility.sendMessage(player, "<Nightmare Circus> " + nightmareId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<Nightmare Circus> " + nightmareId + " started!");
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys3Message(player, "\uE04C", "<Nightmare Circus 4.3> is now open !!!");
					}
				});
				GameLocationBootstrapServices.nightmareCircusService().startNightmareCircus(nightmareId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.nightmareCircusService().isNightmareCircusInProgress(nightmareId)) {
				PacketSendUtility.sendMessage(player, "<Nightmare Circus> " + nightmareId + " is not start!");
			} else {
				PacketSendUtility.sendMessage(player, "<Nightmare Circus> " + nightmareId + " stopped!");
				GameLocationBootstrapServices.nightmareCircusService().stopNightmareCircus(nightmareId);
			}
		}
	}

	/**
	 * 校验马戏团地点 ID 是否已注册。
	 * Validate whether the Nightmare Circus location id is registered.
	 *
	 *
	 * @return 若 valid 则为 true / True if valid
	 */
	protected boolean isValidNightmareCircusLocationId(Player player, int nightmareId) {
		if (!GameLocationBootstrapServices.nightmareCircusService().getNightmareCircusLocations().keySet().contains(nightmareId)) {
			PacketSendUtility.sendMessage(player, "Id " + nightmareId + " is invalid");
			return false;
		}
		return true;
	}

	/**
	 * 向管理员显示命令帮助。
	 * Show command help to the admin.
	 *
	 * @param player 执行 GM / Admin player
	 */
	protected void showHelp(Player player) {
		PacketSendUtility.sendMessage(player, "AdminCommand //circus start|stop <Id>");
	}
}
