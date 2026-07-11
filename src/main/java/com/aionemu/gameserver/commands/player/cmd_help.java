package com.aionemu.gameserver.commands.player;

import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家帮助命令：列出可用的点号命令。
 * Player help command: lists available dot-commands.
 *
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
	 *
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
            PacketSendUtility.sendMessage(player, "" +
					"\n" +
                    "==============================\n" +
                    "Available .[dot] Commands for Players!" +
                    "\n==============================\n" +
                    " .skills : refresh or get new skills.\n" +
                    " .givestigma add : to get your class stigma's.\n" +
                    " .ffa : to join or leave free for all\n" +
					// “.pk：进行 PK 变身\n” + / " .pk : to make pk xform\n" +
					" .pvp : brings you to the pvp map\n" +
					// “.mixfight：加入或离开混合战。\n” + / " .mixfight : to join of leave mixfights.\n" +
					" .siege : brings you to the siege map\n" +
                    " .clean <item id/link> : to delete an item\n" +
                    " .toll : shows current toll you have in your account.\n" +
                    " .insanepack info : informs you with some important information about how to get gears!\n" +
                    " .dye <color> : to dye yourself.\n" +
					" .augmentme : to augment or condition your whole equipment.\n" +
                    " .unstuck : go to obelisk location\n" +
                    " .skin : will remove your candy look,\n" +
					" .reskinvip : reskin two handed weapons with use of tiamat bloody tear [VIP ONLY]");
                    PacketSendUtility.sendMessage(player,
                    ".faction : asmodian/elyos world chat\n" +
                    " .world : open world chat\n" +
                    " .enchant 15 : will enchant your equipment to 15.\n" +
					" .gmlist : shows available gm's \n" +
                    " .pet add : adds You a scroll Buffer Pet.\n" +
                    " .job : Makes all craft available\n" +
                    " .queue : registers you in an on-going event hosted by a gm.\n" +
					" .remodel : cross remodel with use of tiamat bloody tears\n");

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
        PacketSendUtility.sendMessage(player, "Syntax : .help");
    }
}
