package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 向管理员自身发送指定 ID 的系统消息包。
 * Sends a system-message packet with a given id to the admin.
 *
 * @author Wnkrz
 */
public class SysMessage extends AdminCommand
{
	/**
	 * 构造 message 命令。
	 * Creates the message command.
	 */
	public SysMessage() {
		super("message");
	}

	/**
	 * 解析消息 ID 并发送 SM_SYSTEM_MESSAGE。
	 * Parses message id and sends SM_SYSTEM_MESSAGE.
	 *
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(admin, "Syntax //message <id>");
			return;
		}
		int id = 0;
		try {
			id = Integer.parseInt(params[0]);
		} catch (NumberFormatException e) {
			PacketSendUtility.sendMessage(admin, "id should number");
			return;
		}
		PacketSendUtility.sendPacket(admin, new SM_SYSTEM_MESSAGE(id));
	}
}
