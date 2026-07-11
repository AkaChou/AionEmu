package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：切换是否接收经验奖励。
 * Player command: toggles receiving experience rewards.
 *
 * @author Wakizashi
 */
public class cmd_noexp extends PlayerCommand {

	/**
	 * 注册命令别名 {@code noexp}。
	 * Registers the command alias {@code noexp}.
	 */
	public cmd_noexp() {
		super("noexp");
	}

	/**
	 * 在启用/禁用经验获取之间切换。
	 * Toggles experience gain on or off for the player.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 未使用的参数 / unused parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		if (player.getCommonData().getNoExp()) {
			player.getCommonData().setNoExp(false);
			PacketSendUtility.sendMessage(player, "Experience rewards are reactivated !");
		}
		else {
			player.getCommonData().setNoExp(true);
			PacketSendUtility.sendMessage(player, "Experience rewards are desactivated !");
		}
	}

}
