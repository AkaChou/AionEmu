package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 切换管理员是否接受密语的命令。
 * Admin command to toggle accepting whispers.
 */
public class Whisper extends AdminCommand {

	/**
	 * 构造 whisper 命令。
	 * Creates the whisper command.
	 */
	public Whisper() {
		super("whisper");
	}

	/**
	 * on 开启接受密语，off 关闭。
	 * on accepts whispers; off rejects them.
	 *
	 * @param admin 执行 GM / Admin player
	 * @param params on|off。 / on|off
	 */
	@Override
	public void execute(Player admin, String... params) {

		if(params[0].equalsIgnoreCase("off")) {
			admin.setUnWispable();
			PacketSendUtility.sendMessage(admin, "Accepting Whisper : OFF");
		}
		else if (params[0].equalsIgnoreCase("on")) {
			admin.setWispable();
			PacketSendUtility.sendMessage(admin, "Accepting Whisper : ON");
		}
	}

	/**
	 * 参数错误时的用法提示。
	 * Usage hint on invalid parameters.
	 *
	 * 玩家 / Player
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //whisper [on for wispable / off for unwispable]");
	}
}
