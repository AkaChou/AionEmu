package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

import java.util.Iterator;

/**
 * 向全服在线玩家广播居中亮黄通知的管理员命令。
 * Admin command to broadcast a bright-yellow center notice to all online players.
 *
 * @author Jenose Updated By Darkwolf
 */
public class Notice extends AdminCommand {

	/**
	 * 以别名 {@code notice} 构造命令。
	 * Construct the command with alias {@code notice}.
	 */
	public Notice() {
		super("notice");
	}

	/**
	 * 拼接参数为消息并向所有在线玩家发送居中通知。
	 * Join parameters into a message and send a center notice to every online player.
	 *
	 */
	@Override
	public void execute(Player player, String... params) {

		String message = "";

		try {
			for (int i = 0; i < params.length; i++) {
				message += " " + params[i];
			}
		}
		catch (NumberFormatException e) {
			PacketSendUtility.sendMessage(player, "Parameters should be text or number !");
			return;
		}
		Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();

		while (iter.hasNext()) {
			PacketSendUtility.sendBrightYellowMessageOnCenter(iter.next(), "Information: " + message);
		}
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //notice <message>");
	}
}
