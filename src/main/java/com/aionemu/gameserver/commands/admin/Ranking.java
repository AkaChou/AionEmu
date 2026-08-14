package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameEventServices;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.abyss.AbyssRankUpdateService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 欧比斯排名管理指令；手动触发欧比斯排名更新。
 * Admin command to manually trigger abyss ranking updates.
 *
 * @author ATracer
 */
public class Ranking extends AdminCommand {

	public Ranking() {
		super("ranking");
	}

	/**
	 * 执行排名指令；支持 {@code update} 子命令触发全服欧比斯排名刷新。
	 * Executes the ranking command; {@code update} triggers a full abyss rank refresh.
	 *
	 * @param admin 执行指令的管理员 / admin executing the command
	 * @param params 参数，通常为 {@code update} / args, typically {@code update}
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0) {
			onFail(admin, null);
		}
		else if ("update".equalsIgnoreCase(params[0])) {
			GameEventServices.abyssRankUpdateService().performUpdate();
		}
	}

	/**
	 * 参数错误时输出用法。
	 * Prints usage when arguments are invalid.
	 *
	 * @param player 接收提示的玩家 / player receiving the message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //ranking update");
	}
}