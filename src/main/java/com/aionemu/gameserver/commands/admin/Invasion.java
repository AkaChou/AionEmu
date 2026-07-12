package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameLocationBootstrapServices;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.VortexService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;
import org.apache.commons.lang3.math.NumberUtils;

/**
 * 管理员次元入侵活动命令：按漩涡地点 ID 启动或停止 Invasion。
 * Admin dimensional invasion command: start or stop by vortex location id.
 */
public class Invasion extends AdminCommand
{
	private static final String COMMAND_START = "start";
	private static final String COMMAND_STOP = "stop";

	public Invasion() {
		super("invasion");
	}

	/**
	 * 分发 start/stop 子命令。
	 * Dispatch start/stop subcommands.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * @param params 子命令与漩涡 ID / Subcommand and vortex id
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
	 * 启动或停止指定 Id 的次元入侵，并向对应种族广播系统消息。
	 * Start or stop invasion for the given id and broadcast race-specific system messages.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * @param params start|stop and vortex id。
	 */
	protected void handleStartStopInvasion(Player player, String... params) {
		if (params.length != 2 || !NumberUtils.isDigits(params[1])) {
			showHelp(player);
			return;
		}
		int vortexId = NumberUtils.toInt(params[1]);
		if (!isValidVortexLocationId(player, vortexId)) {
			showHelp(player);
			return;
		} if (COMMAND_START.equalsIgnoreCase(params[0])) {
			if (GameLocationBootstrapServices.vortexService().isInvasionInProgress(vortexId)) {
				PacketSendUtility.sendMessage(player, "<Vortex Location> " + vortexId + " is already start");
			} else {
				PacketSendUtility.sendMessage(player, "<Vortex Location> " + vortexId + " started!");
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						if (player.getCommonData().getRace() == Race.ELYOS) {
						    // 通往布鲁斯特霍宁的次元漩涡已出现。 / A Dimensional Vortex leading to Brusthonin has appeared.
						    PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DARK_SIDE_INVADE_DIRECT_PORTAL_OPEN);
						}
					}
				});
				com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
					@Override
					public void visit(Player player) {
						if (player.getCommonData().getRace() == Race.ASMODIANS) {
						    // 通往西奥波莫斯的次元漩涡已出现。 / A Dimensional Vortex leading to Theobomos has appeared.
						    PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_LIGHT_SIDE_INVADE_DIRECT_PORTAL_OPEN);
						}
					}
				});
				GameLocationBootstrapServices.vortexService().startInvasion(vortexId);
			}
		} else if (COMMAND_STOP.equalsIgnoreCase(params[0])) {
			if (!GameLocationBootstrapServices.vortexService().isInvasionInProgress(vortexId)) {
				PacketSendUtility.sendMessage(player, "<Vortex Location> " + vortexId + " is not start!");
			} else {
				PacketSendUtility.sendMessage(player, "<Vortex Location> " + vortexId + " stopped!");
				GameLocationBootstrapServices.vortexService().stopInvasion(vortexId);
			}
		}
	}

	/**
	 * 校验漩涡地点 ID 是否有效。
	 * Validate whether the vortex location id exists.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * Vortex location id
	 *
	 * @return 若 valid 则为 true / True if valid
	 */
	protected boolean isValidVortexLocationId(Player player, int vortexId) {
		if (!GameLocationBootstrapServices.vortexService().getVortexLocations().keySet().contains(vortexId)) {
			PacketSendUtility.sendMessage(player, "Id " + vortexId + " is invalid");
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
		PacketSendUtility.sendMessage(player, "AdminCommand //invasion start|stop <Id>");
	}
}
