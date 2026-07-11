package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：显示账号当前 Toll 余额。
 * Player command: shows the account's current Toll balance.
 *
 * @author Kill3r
 */
public class cmd_toll extends PlayerCommand {

	/**
	 * 注册命令别名 {@code toll}。
	 * Registers the command alias {@code toll}.
	 */
    public cmd_toll() {
        super("toll");
    }

	/**
	 * 查询并发送账号 Toll 数量。
	 * Queries and sends the account Toll amount.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 未使用的参数 / unused parameters
	 */
    @Override
    public void execute(Player player, String... params) {
        long toll = player.getPlayerAccount().getToll();
        PacketSendUtility.sendMessage(player, "You, my friend have " + toll + " toll(s) in your account!");
    }

}
