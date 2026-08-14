package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

import java.util.Iterator;

/**
 * 管理员全服公告命令：以匿名或实名向全服居中广播。
 * Admin announce command: center-broadcasts a message server-wide, anonymously or named.
 *
 * @author Ben, Ritsu
 */
public class Announce extends AdminCommand {

	/**
	 * 注册 {@code //announce} 命令。
	 * Registers the {@code //announce} command.
	 */
	public Announce() {
		super("announce");
	}

	/**
	 * 执行全服公告：按 anonymous/name 前缀组装消息并广播。
	 * Executes server announce: builds the message from anonymous/name prefix and broadcasts.
	 *
	 */
	@Override
	public void execute(Player player, String... params) {
		String message;

		if (("anonymous").startsWith(params[0].toLowerCase())) {
			message = "Announce: ";
		}
		else if (("name").startsWith(params[0].toLowerCase())) {
			message = player.getName() + ": ";
		}
		else {
			PacketSendUtility.sendMessage(player, "Syntax: //announce <anonymous|name> <message>");
			return;
		}

		// 带空格添加 / Add with space
		for (int i = 1; i < params.length - 1; i++)
			message += params[i] + " ";

		// 添加最后一项，末尾不加空格 / Add the last without the end space
		message += params[params.length - 1];

		Iterator<Player> iter = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getPlayersIterator();

		while (iter.hasNext()) {
			PacketSendUtility.sendBrightYellowMessageOnCenter(iter.next(), message);
		}
	}

	/**
	 * 参数错误时输出 {@code //announce} 用法。
	 * Prints {@code //announce} usage on invalid arguments.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //announce <anonymous|name> <message>");
	}
}