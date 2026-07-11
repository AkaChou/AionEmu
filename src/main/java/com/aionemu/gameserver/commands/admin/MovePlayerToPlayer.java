package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 将一名在线玩家传送到另一名在线玩家位置的管理员命令。
 * Admin command to teleport one online player to another online player's location.
 *
 * @author Tanelorn
 */
public class MovePlayerToPlayer extends AdminCommand {

	/**
	 * 以别名 {@code moveplayertoplayer} 构造命令。
	 * Construct the command with alias {@code moveplayertoplayer}.
	 */
	public MovePlayerToPlayer() {
		super("moveplayertoplayer");
	}

	/**
	 * 将 {@code characterNameToMove} 传送到 {@code characterNameDestination} 所在坐标。
	 * Teleport {@code characterNameToMove} to the position of {@code characterNameDestination}.
	 *
	 * @param admin 执行 GM / Admin player
	 * @param params 被传送玩家名与目标玩家名 / Source and destination character names
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 2) {
			PacketSendUtility.sendMessage(admin,
				"syntax //moveplayertoplayer <characterNameToMove> <characterNameDestination>");
			return;
		}

		Player playerToMove = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));
		if (playerToMove == null) {
			PacketSendUtility.sendMessage(admin, "The specified player is not online.");
			return;
		}

		Player playerDestination = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[1]));
		if (playerDestination == null) {
			PacketSendUtility.sendMessage(admin, "The destination player is not online.");
			return;
		}

		if (playerToMove.getObjectId() == playerDestination.getObjectId()) {
			PacketSendUtility.sendMessage(admin, "Cannot move the specified player to their own position.");
			return;
		}

		TeleportService2.teleportTo(playerToMove, playerDestination.getWorldId(), playerDestination.getInstanceId(),
			playerDestination.getX(), playerDestination.getY(), playerDestination.getZ(), playerDestination.getHeading());

		PacketSendUtility.sendMessage(admin, "Teleported player " + playerToMove.getName() + " to the location of player "
			+ playerDestination.getName() + ".");
		PacketSendUtility.sendMessage(playerToMove, "You have been teleported by an administrator.");
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 * 玩家 / Player
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //moveplayertoplayer <characterNameToMove> <characterNameDestination>");
	}
}
