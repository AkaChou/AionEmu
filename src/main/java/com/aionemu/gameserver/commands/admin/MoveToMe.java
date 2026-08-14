package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 将指定在线玩家传送到管理员身边的管理员命令。
 * Admin command to teleport a named online player to the admin.
 *
 * @author Cyrakuse
 */
public class MoveToMe extends AdminCommand {

	/**
	 * 以别名 {@code movetome} 构造命令。
	 * Construct the command with alias {@code movetome}.
	 */
	public MoveToMe() {
		super("movetome");
	}

	/**
	 * 将 {@code characterName} 传送到管理员当前位置。
	 * Teleport {@code characterName} to the admin's current location.
	 *
	 * @param player 执行 GM / Admin player
	 * @param params 目标角色名 / Target character name
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(player, "syntax //movetome <characterName>");
			return;
		}

		Player playerToMove = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));
		if (playerToMove == null) {
			PacketSendUtility.sendMessage(player, "The specified player is not online.");
			return;
		}

		if (playerToMove == player) {
			PacketSendUtility.sendMessage(player, "Cannot use this command on yourself.");
			return;
		}

		TeleportService2.teleportTo(playerToMove, player.getWorldId(), player.getInstanceId(), player.getX(), player.getY(),
			player.getZ(), player.getHeading());
		PacketSendUtility.sendMessage(player, "Teleported player " + playerToMove.getName() + " to your location.");
		PacketSendUtility.sendMessage(playerToMove, "You have been teleported by " + player.getName() + ".");
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //movetome <characterName>");
	}
}
