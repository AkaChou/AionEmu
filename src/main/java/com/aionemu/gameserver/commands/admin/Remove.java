package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 物品移除指令；从在线玩家背包中按物品 ID 删除指定数量。
 * Admin command that removes a quantity of items by ID from an online player's inventory.
 *
 * @author Phantom, ATracer
 */
public class Remove extends AdminCommand {

	public Remove() {
		super("remove");
	}

	/**
	 * 执行该管理指令。
	 * Executes this admin command.
	 *
	 * @param admin 执行指令的管理员 / admin executing the command
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length < 2) {
			PacketSendUtility.sendMessage(admin, "syntax //remove <player> <item ID> <quantity>");
			return;
		}

		int itemId = 0;
		long itemCount = 1;
		Player target = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));
		if (target == null) {
			PacketSendUtility.sendMessage(admin, "Player isn't online.");
			return;
		}

		try {
			itemId = Integer.parseInt(params[1]);
			if (params.length == 3) {
				itemCount = Long.parseLong(params[2]);
			}
		}
		catch (NumberFormatException e) {
			PacketSendUtility.sendMessage(admin, "Parameters need to be an integer.");
			return;
		}

		Storage bag = target.getInventory();

		long itemsInBag = bag.getItemCountByItemId(itemId);
		if (itemsInBag == 0) {
			PacketSendUtility.sendMessage(admin, "Items with that id are not found in the player's bag.");
			return;
		}

		Item item = bag.getFirstItemByItemId(itemId);
		bag.decreaseByObjectId(item.getObjectId(), itemCount);

		PacketSendUtility.sendMessage(admin, "Item(s) removed succesfully");
		PacketSendUtility.sendMessage(target, "Admin removed an item from your bag");
	}

	/**
	 * 参数错误时输出用法。
	 * Prints usage when arguments are invalid.
	 *
	 * @param player 接收提示的玩家 / player receiving the message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //remove <player> <item ID> <quantity>");
	}
}