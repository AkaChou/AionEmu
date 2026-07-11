package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.ItemId;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 管理员基纳发放命令：向自身、指定或目标玩家添加基纳（道具 182400001）。
 * Admin kinah grant command: add kinah to self, a named player or target (item 182400001).
 *
 * @author Sarynth Simple admin assistance command for adding kinah to self, named player or target player. Based on //add command. Kinah Item Id - 182400001 (Using ItemId.KINAH.value())
 */
public class Kinah extends AdminCommand {

	public Kinah() {
		super("kinah");
	}

	/**
	 * 向自身或指定玩家发放指定数量基纳。
	 * Grant the given amount of kinah to self or a named player.
	 *
	 * @param admin 执行命令的管理员 / Admin executing the command
	 * @param params [玩家名] 数量 / [player] quantity
	 */
	@Override
	public void execute(Player admin, String... params) {
		long kinahCount;
		Player receiver;

        if (params.length == 0) {
            onFail(admin, null);
            return;
        }

		if (params.length == 1) {
			receiver = admin;
			try {
				kinahCount = Long.parseLong(params[0]);
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "Kinah value must be an integer.");
				return;
			}
		}
		else {
			receiver = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));

			if (receiver == null) {
				PacketSendUtility.sendMessage(admin, "Could not find a player by that name.");
				return;
			}

			try {
				kinahCount = Long.parseLong(params[1]);
			}
			catch (NumberFormatException e) {
				PacketSendUtility.sendMessage(admin, "Kinah value must be an integer.");
				return;
			}
		}

		long count = ItemService.addItem(receiver, ItemId.KINAH.value(), kinahCount);

		if (count == 0) {
			PacketSendUtility.sendMessage(admin, "Kinah given successfully.");
			PacketSendUtility.sendMessage(receiver, "An admin gives you some kinah.");
		}
		else {
			PacketSendUtility.sendMessage(admin, "Kinah couldn't be given.");
		}
	}

	/**
	 * 参数错误时显示命令语法。
	 * Show command syntax on invalid arguments.
	 *
	 * @param player 接收提示的玩家 / Player receiving the hint
	 * Failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //kinah [player] <quantity>");
	}
}
