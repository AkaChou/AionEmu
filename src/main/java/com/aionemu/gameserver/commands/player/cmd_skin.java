package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_TRANSFORM;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：移除糖果/变形外观（保留属性）。
 * Player command: removes candy/transform look while keeping stats.
 *
 * @author Kill3r
 */
public class cmd_skin extends PlayerCommand {

	/**
	 * 注册命令别名 {@code skin}。
	 * Registers the command alias {@code skin}.
	 */
    public cmd_skin(){
        super("skin");
    }

	/**
	 * 将变形模型重置为 0 并广播外观更新。
	 * Resets transform model id to 0 and broadcasts the appearance update.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 未使用的参数 / unused parameters
	 */
    public void execute(Player player, String...params){
        int skin = 0;
        player.getTransformModel().setModelId(skin);
        PacketSendUtility.broadcastPacketAndReceive(player, new SM_TRANSFORM(player, true));

        PacketSendUtility.sendMessage(player, "You have removed the candy form but you will still have the stats.");

    }
}
