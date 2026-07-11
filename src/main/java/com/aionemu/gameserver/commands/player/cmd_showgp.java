package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：显示当前荣耀点数（GP）。
 * Player command: shows the player's current Glory Points (GP).
 *
 * @author Kill3r
 */
public class cmd_showgp extends PlayerCommand {

    /**
     * 注册命令别名 {@code showgp}。
     * Registers the command alias {@code showgp}.
     */
    public cmd_showgp() {
        super("showgp");
    }

    /**
     * 向玩家发送当前 GP 总量。
     * Sends the player's total GP amount.
     *
     * @param player 执行命令的玩家 / invoking player
     * @param params 未使用的参数 / unused parameters
     */
    public void execute(Player player, String...params){
        int gp = player.getAbyssRank().getGp();

        PacketSendUtility.sendMessage(player, "You have "+gp+" in total!");
    }
}
