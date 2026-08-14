package com.aionemu.gameserver.commands.admin;

import com.aionemu.commons.database.dao.DAOManager;
import com.aionemu.gameserver.dao.PlayerDAO;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 查询数据库中在线玩家数量的管理员命令。
 * Admin command to query the online player count from the database.
 *
 * @author VladimirZ
 */
public class Online extends AdminCommand {

	/**
	 * 以别名 {@code online} 构造命令。
	 * Construct the command with alias {@code online}.
	 */
	public Online() {
		super("online");
	}

	/**
	 * 从 {@link PlayerDAO} 读取在线人数并回复管理员。
	 * Read the online count from {@link PlayerDAO} and reply to the admin.
	 *
	 */
	@Override
	public void execute(Player admin, String... params) {

		int playerCount = DAOManager.getDAO(PlayerDAO.class).getOnlinePlayerCount();

		if (playerCount == 1) {
			PacketSendUtility.sendMessage(admin, "There is " + (playerCount) + " player online !");
		}
		else {
			PacketSendUtility.sendMessage(admin, "There are " + (playerCount) + " players online !");
		}
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Syntax: //online");
	}
}
