package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAY_MOVIE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

/**
 * 向管理员客户端播放指定类型与 ID 过场动画的命令。
 * Admin command to play a cutscene of the given type and id on the admin client.
 *
 * @author d3v1an
 */
public class Movie extends AdminCommand {

	/**
	 * 以别名 {@code movie} 构造命令。
	 * Construct the command with alias {@code movie}.
	 */
	public Movie() {
		super("movie");
	}

	/**
	 * 发送 {@link SM_PLAY_MOVIE} 播放过场动画。
	 * Send {@link SM_PLAY_MOVIE} to play the cutscene.
	 *
	 * @param player 执行 GM / Admin player
	 * @param params 类型与动画 ID / Type and movie id
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length < 1) {
			onFail(player, null);
		}
		else {
			PacketSendUtility.sendPacket(player, new SM_PLAY_MOVIE(Integer.parseInt(params[0]), Integer.parseInt(params[1])));
		}
	}

	/**
	 * 参数错误时显示语法。
	 * Show syntax when parameters are invalid.
	 *
	 * 玩家 / Player
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "//movie <type> <id>");
	}
}
