package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.Announcement;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.lifecycle.GameRuntimeServices;
import com.aionemu.gameserver.services.AnnouncementService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.util.Set;

/**
 * 管理员循环公告管理命令：列出、新增或删除数据库公告。
 * Admin scheduled-announcement command: lists, adds, or deletes DB-backed announcements.
 *
 * @author Divinity
 */
public class Announcements extends AdminCommand {

	private AnnouncementService announceService;

	/**
	 * 注册 {@code //announcements} 命令并获取公告服务。
	 * Registers the {@code //announcements} command and obtains the announcement service.
	 */
	public Announcements() {
		super("announcements");
		announceService = GameRuntimeServices.announcementService();
	}

	/**
	 * 执行公告管理：list/add/delete 子命令。
	 * Executes announcement management: list/add/delete subcommands.
	 *
	 * @param params 参数：list|add|delete 及附加参数 / list|add|delete and extra args
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params[0].equals("list")) {
			Set<Announcement> announces = announceService.getAnnouncements();
			PacketSendUtility.sendMessage(player, "ID  |  FACTION  |  CHAT TYPE  |  DELAY  |  MESSAGE");
			PacketSendUtility.sendMessage(player, "-------------------------------------------------------------------");

			for (Announcement announce : announces)
				PacketSendUtility.sendMessage(player, announce.getId() + "  |  " + announce.getFaction() + "  |  " + announce.getType() + "  |  " + announce.getDelay() + "  |  " + announce.getAnnounce());
		}
		else if (params[0].equals("add")) {
			if ((params.length < 5)) {
				onFail(player, null);
				return;
			}

			int delay;

			try {
				delay = Integer.parseInt(params[3]);
			}
			catch (NumberFormatException e) {
				// 15 分钟，默认 / 15 minutes, default
				delay = 900;
			}

			String message = "";

			// 带空格添加 / Add with space
			for (int i = 4; i < params.length - 1; i++)
				message += params[i] + " ";

			// 添加最后一项，末尾不加空格 / Add the last without the end space
			message += params[params.length - 1];

			// 创建公告 / Create the announce
			Announcement announce = new Announcement(message, params[1], params[2], delay);

			// 在数据库中添加公告 / Add the announce in the database
			announceService.addAnnouncement(announce);

			// 重新加载全部公告 / Reload all announcements
			announceService.reload();

			PacketSendUtility.sendMessage(player, "The announcement has been created with successful !");
		}
		else if (params[0].equals("delete")) {
			if ((params.length < 2)) {
				onFail(player, null);
				return;
			}

			int id;

			try {
				id = Integer.parseInt(params[1]);
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(player, "The announcement's ID is wrong !");
				onFail(player, e.getMessage());
				return;
			}

			// 从数据库删除公告 / Delete the announcement from the database
			announceService.delAnnouncement(id);

			// 重新加载全部公告 / Reload all announcements
			announceService.reload();

			PacketSendUtility.sendMessage(player, "The announcement has been deleted with successful !");
		}
		else {
			onFail(player, null);
		}
	}

	/**
	 * 参数错误时输出 {@code //announcements} 用法。
	 * Prints {@code //announcements} usage on invalid arguments.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		String syntaxCommand = "Syntax: //announcements list - Obtain all announcements in the database.\n";
		syntaxCommand += "Syntax: //announcements add <faction: ELYOS | ASMODIANS | ALL> <type: SYSTEM | WHITE | ORANGE | SHOUT | YELLOW> <delay in seconds> <message> - Add an announcements in the database.\n";
		syntaxCommand += "Syntax: //announcements delete <id (see //announcements list to find all id> - Delete an announcements from the database.";
		PacketSendUtility.sendMessage(player, syntaxCommand);
	}
}
