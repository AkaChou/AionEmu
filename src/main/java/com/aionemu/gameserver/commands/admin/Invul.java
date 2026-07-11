package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员无敌切换命令。
 * Admin invulnerability toggle command.
 *
 * @author Andy
 * @author Divinity - update
 */
public class Invul extends AdminCommand {

	public Invul() {
		super("invul");
	}

	/**
	 * 切换执行者的无敌状态。
	 * Toggle the invoker's invulnerability.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * Unused
	 */
	@Override
	public void execute(Player player, String... params) {
		if (player.isInvul()) {
			player.setInvul(false);
			PacketSendUtility.sendMessage(player, "You are now mortal.");
		}
		else {
			player.setInvul(true);
			PacketSendUtility.sendMessage(player, "You are now immortal.");
		}
	}

}
