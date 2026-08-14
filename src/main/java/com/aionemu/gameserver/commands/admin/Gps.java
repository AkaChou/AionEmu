package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员 GPS 坐标查询命令：输出自身 X/Y/Z、朝向与世界 ID。
 * Admin GPS command: print own X/Y/Z, heading and world id.
 */
public class Gps extends AdminCommand
{
	public Gps() {
		super("gps");
	}

	/**
	 * 向管理员输出当前坐标信息。
	 * Print the admin's current coordinates.
	 *
	 * @param admin 执行命令的管理员 / Admin executing the command
	 */
	@Override
	public void execute(Player admin, String... params) {
		PacketSendUtility.sendMessage(admin, "== GPS Coordinates ==");
		PacketSendUtility.sendMessage(admin, "X: = " + admin.getX());
		PacketSendUtility.sendMessage(admin, "Y: = " + admin.getY());
		PacketSendUtility.sendMessage(admin, "Z: = " + admin.getZ());
		PacketSendUtility.sendMessage(admin, "H: = " + admin.getHeading());
		PacketSendUtility.sendMessage(admin, "World: = " + admin.getWorldId());
		PacketSendUtility.sendMessage(admin, "=====================");
	}

	/**
	 * 失败回调（本命令无额外语法提示）。
	 * Failure callback (no extra syntax for this command).
	 *
	 * @param player 接收提示的玩家 / Player receiving the hint
	 */
	@Override
	public void onFail(Player player, String message) {
	}
}
