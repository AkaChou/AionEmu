package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameStaticDataServices;

import com.aionemu.gameserver.cache.HTMLCache;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 管理员帮助命令：以 HTML 展示可用管理命令列表。
 * Admin help command: shows the available admin-command list via HTML.
 *
 * @author Phantom, ATracer
 */
public class Admin extends AdminCommand {

	/**
	 * 注册 {@code //admin} 命令。
	 * Registers the {@code //admin} command.
	 */
	public Admin() {
		super("admin");
	}

	/**
	 * 执行帮助展示：打开 commands.xhtml 页面。
	 * Executes help display: opens the commands.xhtml page.
	 *
	 * @param params 参数（未使用） / unused params
	 */
	@Override
	public void execute(Player player, String... params) {
		HTMLService.showHTML(player, GameStaticDataServices.htmlCache().getHTML("commands.xhtml"));
	}

}