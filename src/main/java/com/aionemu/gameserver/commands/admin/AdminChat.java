package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 管理员频道聊天命令：向所有在线 GM 广播消息。
 * Admin staff-chat command: broadcasts a message to all online GMs.
 *
 * @author Imaginary
 */
public class AdminChat extends AdminCommand {

	/**
	 * 注册 {@code //s} 命令。
	 * Registers the {@code //s} command.
	 */
	public AdminChat() {
		super("s");
	}

	/**
	 * 执行 GM 频道发言：校验权限与禁言后广播。
	 * Executes staff chat: checks GM level/gag status, then broadcasts.
	 *
	 * @param params 参数：消息内容 / message text
	 */
	@Override
	public void execute(Player admin, String... params)
	{
		if(!admin.isGM()) {
			PacketSendUtility.sendMessage(admin, "Vous devez etre au moins rang " + AdminConfig.GM_LEVEL + " pour utiliser cette commande");
			return;
		}
		if(admin.isGagged()) {
			PacketSendUtility.sendMessage(admin, "Vous avez ete reduit au silence ...");
			return;
		}

		StringBuilder sbMessage = new StringBuilder("[Admin] " + admin.getName() + " : ");

		for(String p : params)
			sbMessage.append(p + " ");
		String message = sbMessage.toString().trim();
		for(Player a : com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getAllPlayers()) {
			if(a.isGM())
				PacketSendUtility.sendWhiteMessageOnCenter(a, message);
		}
	}
	
	/**
	 * 参数错误时输出 {@code //s} 用法。
	 * Prints {@code //s} usage on invalid arguments.
	 *
	 */
	@Override
	public void onFail(Player admin, String message) {
		PacketSendUtility.sendMessage(admin, "Syntax: //s <message>");
	}
}