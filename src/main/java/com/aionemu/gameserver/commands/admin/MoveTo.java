package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.WorldMapType;

/**
 * 将管理员传送到指定世界坐标的管理员命令。
 * Admin command to teleport the admin to given world coordinates.
 *
 * @author KID
 */
public class MoveTo extends AdminCommand {

	/**
	 * 以别名 {@code moveto} 构造命令。
	 * Construct the command with alias {@code moveto}.
	 */
	public MoveTo() {
		super("moveto");
	}

	/**
	 * 传送到 {@code worldId X Y Z}。
	 * Teleport to {@code worldId X Y Z}.
	 *
	 * @param admin 执行 GM / Admin player
	 * @param params 世界 ID 与坐标 / World id and coordinates
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 4) {
			PacketSendUtility.sendMessage(admin, "syntax //moveto worldId X Y Z");
			return;
		}

		int worldId;
		float x, y, z;

		try {
			worldId = Integer.parseInt(params[0]);
			x = Float.parseFloat(params[1]);
			y = Float.parseFloat(params[2]);
			z = Float.parseFloat(params[3]);
		}
		catch (NumberFormatException e) {
			PacketSendUtility.sendMessage(admin, "All the parameters should be numbers");
			return;
		}

		if (WorldMapType.getWorld(worldId) == null) {
			PacketSendUtility.sendMessage(admin, "Illegal WorldId %d " + worldId);
		}
		else {
			TeleportService2.teleportTo(admin, worldId, x, y, z);
			PacketSendUtility.sendMessage(admin, "Teleported to " + x + " " + y + " " + z + " [" + worldId + "]");
		}
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
		PacketSendUtility.sendMessage(player, "syntax //moveto worldId X Y Z");
	}
}
