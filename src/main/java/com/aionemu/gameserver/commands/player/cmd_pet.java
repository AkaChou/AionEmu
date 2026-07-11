package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;
import com.aionemu.gameserver.services.item.ItemService;

/**
 * 玩家命令：领取增益型宠物道具。
 * Player command: grants a buffer pet item.
 *
 * @author Aion-Unique
 */
public class cmd_pet extends PlayerCommand {
	/**
	 * 注册命令别名 {@code pet}。
	 * Registers the command alias {@code pet}.
	 */
	public cmd_pet() {
		super("pet");
	}

	/**
	 * 处理 {@code .pet add}，向背包添加宠物卷轴。
	 * Handles {@code .pet add} and adds a pet scroll to inventory.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param param 子命令参数 / sub-command parameters
	 */
		@Override
		public void execute(final Player player, String...param){
        if(param.length < 1){
            PacketSendUtility.sendMessage(player, "syntax : .pet add -- To Add a Buffer Pet");
            return;
			}
	if(param[0].equals("add")){
      		ItemService.addItem(player,190000000, 1); //Pet
			PacketSendUtility.sendMessage(player, "\uE020 You Just Added a Buffer Pet! \uE020");
		}
	 }

	/**
	 * 参数错误时提示用法。
	 * Shows usage when arguments are invalid.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * @param msg 失败消息 / failure message
	 */
    public void onFail(Player player, String msg){
        PacketSendUtility.sendMessage(player, " " +
                "syntax : .pet add  -- To Add a Buffer Pet\n");
    }
}
