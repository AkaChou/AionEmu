package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 将管理员传送到指定在线玩家位置的管理员命令。
 * Admin command to teleport the admin to a named online player.
 *
 * @author Tanelorn
 */
public class MoveToPlayer extends AdminCommand {

	/**
	 * 以别名 {@code movetoplayer} 构造命令。
	 * Construct the command with alias {@code movetoplayer}.
	 */
	public MoveToPlayer() {
		super("movetoplayer");
	}

	/**
	 * 传送到 {@code characterName} 的当前位置。
	 * Teleport to the current location of {@code characterName}.
	 *
	 * @param admin 执行 GM / Admin player
	 * @param params 目标角色名 / Target character name
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(admin, "syntax //movetoplayer characterName");
			return;
		}

		Player player = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));
		if (player == null) {
			PacketSendUtility.sendMessage(admin, "The specified player is not online.");
			return;
		}

		if (player == admin) {
			PacketSendUtility.sendMessage(admin, "Cannot use this command on yourself.");
			return;
		}

		TeleportService2.teleportTo(admin, player.getWorldId(), player.getInstanceId(), player.getX(), player.getY(),
			player.getZ(), player.getHeading());
		PacketSendUtility.sendMessage(admin, "Teleported to player " + player.getName() + ".");
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //movetoplayer characterName");
	}
}
