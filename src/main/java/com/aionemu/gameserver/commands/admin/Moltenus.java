package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.MoltenusService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 复活熔岩领主（Resurrected Moltenus）活动启停管理员命令。
 * Admin command to start or stop Resurrected Moltenus events.
 */
public class Moltenus extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";

	/**
	 * 以别名 {@code moltenus} 构造命令。
	 * Construct the command with alias {@code moltenus}.
	 */
	public Moltenus() {
		super("moltenus");
	}

	/**
	 * 执行启停：无参数时显示帮助；{@code start|stop <Id>} 时切换对应熔岩领主活动。
	 * Execute start/stop: show help with no args; toggle the Moltenus event for {@code start|stop <Id>}.
	 *
	 * 执行 GM / Admin player
	 * Command parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0) {
			showHelp(player);
			return;
		} if (COMMAND_STOP.equalsIgnoreCase(params[0]) || COMMAND_START.equalsIgnoreCase(params[0])) {
			handleStartStopMoltenus(player, params);
		}
	}

	/**
	 * 按地点 ID 启动或停止熔岩领主活动，并向全服广播开始消息。
	 * Start or stop a Moltenus event by location id and broadcast the start notice.
	 *
	 * @param player 执行 GM / Admin player
	 * @param params {@code start|stop} and location id。 / {@code start|stop} and location id
	 */
	protected void handleStartStopMoltenus(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int moltenusId = NumberUtils.toInt(params[1]);
		if (!isValidMoltenusLocationId(player, moltenusId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.moltenusService().isMoltenusInProgress(moltenusId)) {
				PacketSendUtility.sendMessage(player, "<Resurrected Moltenus> " + moltenusId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<Resurrected Moltenus> " + moltenusId + " started!");
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys3Message(player, "\uE005", "<Resurrected Moltenus> appear in the abyss !!!");
					}
				});
				GameLocationBootstrapServices.moltenusService().startMoltenus(moltenusId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.moltenusService().isMoltenusInProgress(moltenusId)) {
				PacketSendUtility.sendMessage(player, "<Resurrected Moltenus> " + moltenusId + " is not start!");
			} else {
				PacketSendUtility.sendMessage(player, "<Resurrected Moltenus> " + moltenusId + " stopped!");
				GameLocationBootstrapServices.moltenusService().stopMoltenus(moltenusId);
			}
		}
	}

	/**
	 * 校验熔岩领主地点 ID 是否已注册。
	 * Validate whether the Moltenus location id is registered.
	 *
	 * 执行 GM / Admin player
	 * Location id
	 *
	 * @return 若 valid 则为 true / True if valid
	 */
	protected boolean isValidMoltenusLocationId(Player player, int moltenusId) {
		if (!GameLocationBootstrapServices.moltenusService().getMoltenusLocations().keySet().contains(moltenusId)) {
			PacketSendUtility.sendMessage(player, "Id " + moltenusId + " is invalid");
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
		PacketSendUtility.sendMessage(player, "AdminCommand //moltenus start|stop <Id>");
	}
}
