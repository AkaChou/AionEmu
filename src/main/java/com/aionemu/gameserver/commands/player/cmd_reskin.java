package com.aionemu.gameserver.commands.player;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RequestResponseHandler;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.PlayerCommand;

/**
 * 玩家命令：确认后将目标外观应用到已装备武器。
 * Player command: after confirmation, applies a target look to an equipped weapon.
 *
 * @author Chuck
 */
public class cmd_reskin extends PlayerCommand {

	public cmd_reskin() {
		super("cmd_reskin");
	}

	/**
	 * 发起改模确认对话框并处理消耗。
	 * Opens a remodel confirmation dialog and handles costs.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * command parameters
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length != 2) {
			PacketSendUtility.sendMessage(player, "syntax .cmd_reskin <Old Item> <New Item>");
			return;
		}

		Player target = player;
		VisibleObject creature = player.getTarget();
		if (player.getTarget() instanceof Player) {
			target = (Player) creature;
		}

		int oldItemId = 0;
		int newItemId = 0;

		try {
			String item = params[0];

			if (item.startsWith("[item:")) {
				Pattern id = Pattern.compile("\\[item:(\\d{9})");
				Matcher result = id.matcher(item);

				if (result.find()) {
					oldItemId = Integer.parseInt(result.group(1));
				}
				else {
					oldItemId = Integer.parseInt(params[0]);
				}
				item = params[1];
				if (item.startsWith("[item:")) {
					id = Pattern.compile("\\[item:(\\d{9})");
					result = id.matcher(item);

					if (result.find()) {
						newItemId = Integer.parseInt(result.group(1));
					}
					else {
						newItemId = Integer.parseInt(params[0]);
					}
				}
				else {
					PacketSendUtility.sendMessage(player, "syntax .cmd_reskin <Old Item> <New Item>");
					return;
				}
			}
			else {
				PacketSendUtility.sendMessage(player, "syntax .cmd_reskin <Old Item> <New Item>");
				return;
			}
		}
		catch (NumberFormatException e) {
			PacketSendUtility.sendMessage(player, "syntax .cmd_reskin <Old Item> <New Item>");
			return;
		}

		Storage storage = player.getInventory();
		List<Item> oldItems = player.getInventory().getItemsByItemId(oldItemId);
		List<Item> newItems = player.getInventory().getItemsByItemId(newItemId);
		// 迭代器古代物品 / Iterator Ancien Item
		Iterator<Item> oldIter = oldItems.iterator();
		Item oldItem = oldIter.next();
		// 迭代器新物品 / Iterator Nouveau Item
		Iterator<Item> newIter = newItems.iterator();
		Item newItem = newIter.next();
		// 验证旧物品是否在背包中 / verification que l'ancien item est dans l'inventaire
		if (oldItems.isEmpty()) {
			PacketSendUtility.sendMessage(player, "You do not have this item in your inventory.");
			return;
		}
		// 验证物品是否为同一类型。 / verification que les items sont du même type.
		if (newItem.getItemTemplate().isWeapon() && oldItem.getItemTemplate().isWeapon()) {
			if (newItem.getItemTemplate().getWeaponType() != oldItem.getItemTemplate().getWeaponType()) {
				PacketSendUtility.sendMessage(player, "You can not remodel different types of item.");
				return;
			}
		}
		else if (newItem.getItemTemplate().isArmor() && oldItem.getItemTemplate().isArmor()) {
			if (newItem.getItemTemplate().getItemSlot() == oldItem.getItemTemplate().getItemSlot()) {
				if (newItem.getItemTemplate().getArmorType() != oldItem.getItemTemplate().getArmorType()) {
					PacketSendUtility.sendMessage(player, "You can not remodel different types of item.");
					return;
				}
			}
			else {
				PacketSendUtility.sendMessage(player, "You can not remodel different types of item.");
				return;
			}
		}

		final int tollPrice = 750;
		final long tolls = player.getClientConnection().getAccount().getToll();
		RequestResponseHandler responseHandler = new RequestResponseHandler(player) {

			@Override
			public void acceptRequest(Creature p2, Player p) {
				if (tolls < tollPrice) {
					PacketSendUtility.sendMessage(p, "You don't have enought Vote Points (" + tolls + "). You need : " + tollPrice + " Vote Points.");
					return;
				}
				p.getClientConnection().getAccount().setToll(tolls - tollPrice);

			}

			@Override
			public void denyRequest(Creature p2, Player p) {
			}
		};

		boolean requested = player.getResponseRequester().putRequest(902247, responseHandler);
		if (requested) {
			oldItem.setItemSkinTemplate(DataManager.ITEM_DATA.getItemTemplate(newItemId));
			storage.decreaseByItemId(newItemId, storage.getItemCountByItemId(newItemId));
			PacketSendUtility.sendBrightYellowMessage(player, "Your item " + params[0] + " just take the appearance of the item " + params[1] + ".");
			PacketSendUtility.sendMessage(player, "For changing the skin, you have use " + tollPrice + " Vote Points!");
		}
	}

	/**
	 * 参数错误时提示用法。
	 * Shows usage when arguments are invalid.
	 *
	 * @param player 执行命令的玩家 / invoking player
	 * failure message
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax : .cmd_reskin <Old Item> <New Item>");
	}
}