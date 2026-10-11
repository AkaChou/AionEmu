package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家帮助命令：列出可用的点号命令。
 * Player help command: lists available dot-commands.
 * @author Nimwey
 */
public class cmd_help extends PlayerCommand {

	/**
	 * 注册命令别名 {@code help}。
	 * Registers the command alias {@code help}.
	 */
    public cmd_help() {
        super("help");
    }

	/**
	 * 向天族/魔族玩家发送可用命令列表。
	 * Sends the available command list to Elyos/Asmodian players.
	 * @param player 执行命令的玩家 / invoking player
	 * @param params 应为空；非空则触发失败提示 / must be empty; otherwise failure help is shown
	 */
    @Override
    public void execute(Player player, String... params){
        if (params.length != 0) {
            onFail(player, null);
            return;
        }


        if (player.getRace() == Race.ASMODIANS ||player.getRace() == Race.ELYOS){
			// “.pk：进行 PK 变身\n” + / " .pk : to make pk xform\n" +
			// “.mixfight：加入或离开混合战。\n” + / " .mixfight : to join of leave mixfights.\n" +
			PacketSendUtility.sendMessage(player, """

				==============================
				Available .[dot] Commands for Players!
				==============================
				 .skills : refresh or get new skills.
				 .givestigma add : to get your class stigma's.
				 .ffa : to join or leave free for all
				 .pvp : brings you to the pvp map
				 .siege : brings you to the siege map
				 .clean <item id/link> : to delete an item
				 .toll : shows current toll you have in your account.
				 .insanepack info : informs you with some important information about how to get gears!
				 .dye <color> : to dye yourself.
				 .augmentme : to augment or condition your whole equipment.
				 .unstuck : go to obelisk location
				 .skin : will remove your candy look,
				 .reskinvip : reskin two handed weapons with use of tiamat bloody tear [VIP ONLY]""");
                    PacketSendUtility.sendMessage(player,
						"""
							.faction : asmodian/elyos world chat
							 .world : open world chat
							 .enchant 15 : will enchant your equipment to 15.
							 .gmlist : shows available gm's\s
							 .pet add : adds You a scroll Buffer Pet.
							 .job : Makes all craft available
							 .queue : registers you in an on-going event hosted by a gm.
							 .remodel : cross remodel with use of tiamat bloody tears
							""");

        }

    }

	/**
	 * 参数错误时提示用法。
	 * Shows usage when arguments are invalid.
	 * @param player 执行命令的玩家 / invoking player
	 * @param msg 失败消息 / failure message
	 */
    public void onFail(Player player, String msg){
        PacketSendUtility.sendMessage(player, "Syntax : .help");
    }
}
