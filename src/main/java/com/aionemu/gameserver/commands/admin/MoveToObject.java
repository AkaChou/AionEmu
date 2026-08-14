package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 按世界对象 ID 传送到该可见对象刷怪点的管理员命令。
 * Admin command to teleport to a visible object spawn by world object id.
 *
 * @author Rolandas
 */
public class MoveToObject extends AdminCommand {

	/**
	 * 以别名 {@code movetoobj} 构造命令。
	 * Construct the command with alias {@code movetoobj}.
	 */
	public MoveToObject() {
		super("movetoobj");
	}

	/**
	 * 按 object id 查找可见对象并传送到其刷怪坐标。
	 * Find a visible object by object id and teleport to its spawn coordinates.
	 *
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length != 1) {
			PacketSendUtility.sendMessage(admin, "Syntax : //movetoobj <object id>");
			return;
		}

		int objectId = 0;

		try {
			objectId = Integer.valueOf(params[0]);
		}
		catch (NumberFormatException e) {
			PacketSendUtility.sendMessage(admin, "Only numbers please!!!");
		}

		VisibleObject object = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(objectId);
		if (object == null) {
			PacketSendUtility.sendMessage(admin, "Cannot find object for spawn #" + objectId);
			return;
		}

		VisibleObject spawn = (VisibleObject) object;

		TeleportService2.teleportTo(admin, spawn.getWorldId(), spawn.getSpawn().getX(), spawn.getSpawn().getY(), spawn
			.getSpawn().getZ());
		admin.getController().stopProtectionActiveTask();
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax : //movetoobj <object id>");
	}
}
