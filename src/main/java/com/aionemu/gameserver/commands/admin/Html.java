package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameStaticDataServices;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员 HTML 缓存命令：重载缓存或向玩家展示指定 XHTML 页面。
 * Admin HTML cache command: reload the cache or show a named XHTML page to the player.
 *
 * @author lord_rex
 */
public class Html extends AdminCommand {

	public Html() {
		super("html");
	}

	/**
	 * 处理 reload（重载缓存）或 show（展示页面）子命令。
	 * Handle reload (refresh cache) or show (display page) subcommands.
	 *
	 * @param player 执行命令的管理员 / Admin executing the command
	 * @param params 子命令与可选文件名 / Subcommand and optional filename
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params == null || params.length < 1) {
			PacketSendUtility.sendMessage(player, "Usage: //html <reload|show>");
			return;
		}

		if (params[0].equals("reload")) {
			GameStaticDataServices.htmlCache().reload(true);
			PacketSendUtility.sendMessage(player, GameStaticDataServices.htmlCache().toString());
		}
		else if (params[0].equals("show"))
			if (params.length >= 2)
				HTMLService.showHTML(player, GameStaticDataServices.htmlCache().getHTML(params[1] + ".xhtml"));
			else
				PacketSendUtility.sendMessage(player, "Usage: //html show <filename>");
	}

	/**
	 * 参数错误时显示命令语法。
	 * Show command syntax on invalid arguments.
	 *
	 * @param player 接收提示的玩家 / Player receiving the hint
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "Usage: //html <reload|show>");
	}
}
