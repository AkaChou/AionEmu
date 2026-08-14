package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.InstanceRiftService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 管理员副本裂隙活动命令：按地点 ID 启动或停止 Instance Rift。
 * Admin instance-rift event command: start or stop by location id.
 */
public class InstanceRift extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";

	public InstanceRift() {
		super("instance");
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
			handleStartStopInstance(player, params);
		}
	}

	/**
	 * 启动或停止指定 Id 的副本裂隙。
	 * Start or stop instance rift for the given location id.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * @param params start|stop and location id。
	 */
	protected void handleStartStopInstance(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int instanceRiftId = NumberUtils.toInt(params[1]);
		if (!isValidInstanceRiftLocationId(player, instanceRiftId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.instanceRiftService().isInstanceRiftInProgress(instanceRiftId)) {
				PacketSendUtility.sendMessage(player, "<Instance Rift> " + instanceRiftId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<Instance Rift> " + instanceRiftId + " started!");
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						PacketSendUtility.sendSys3Message(player, "", "<Instance Rift> is now open !!!");
					}
				});
				GameLocationBootstrapServices.instanceRiftService().startInstanceRift(instanceRiftId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.instanceRiftService().isInstanceRiftInProgress(instanceRiftId)) {
				PacketSendUtility.sendMessage(player, "<Instance Rift> " + instanceRiftId + " is not start!");
			} else {
				PacketSendUtility.sendMessage(player, "<Instance Rift> " + instanceRiftId + " stopped!");
				GameLocationBootstrapServices.instanceRiftService().stopInstanceRift(instanceRiftId);
			}
		}
	}

	/**
	 * 校验副本裂隙地点 ID 是否有效。
	 * Validate whether the instance rift location id exists.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 *
	 * @return 若 valid 则为 true / True if valid
	 */
	protected boolean isValidInstanceRiftLocationId(Player player, int instanceRiftId) {
		if (!GameLocationBootstrapServices.instanceRiftService().getInstanceRiftLocations().keySet().contains(instanceRiftId)) {
			PacketSendUtility.sendMessage(player, "Id " + instanceRiftId + " is invalid");
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
		PacketSendUtility.sendMessage(player, "AdminCommand //instance start|stop <Id>");
	}
}
