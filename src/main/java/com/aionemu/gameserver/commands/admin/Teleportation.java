package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 切换管理员点击传送（teleportation）模式。
 * Toggles admin click-to-teleport mode.
 *
 * @author cura
 */
public class Teleportation extends AdminCommand {

	/**
	 * 构造 teleportation 命令。
	 * Creates the teleportation command.
	 */
	public Teleportation() {
		super("teleportation");
	}

	/**
	 * 启用或禁用管理员传送状态。
	 * Enables or disables admin teleportation state.
	 *
	 * 执行 GM / Admin player
	 * Unused
	 */
	@Override
	public void execute(Player player, String... params) {
		boolean isTeleportation = player.getAdminTeleportation();

		if (isTeleportation) {
			PacketSendUtility.sendMessage(player, "Teleported state is disabled.");
			player.setAdminTeleportation(false);
		}
		else {
			PacketSendUtility.sendMessage(player, "Teleported state.");
			player.setAdminTeleportation(true);
		}
	}

}
