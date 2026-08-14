package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.itemset.ItemPart;
import com.aionemu.gameserver.model.templates.itemset.ItemSetTemplate;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.Util;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;
import com.aionemu.gameserver.world.World;

/**
 * 管理员添加套装命令：按套装模板 ID 发放整套装备。
 * Admin add-set command: grants a full item set by set-template ID.
 *
 * @author Antivirus
 */
public class AddSet extends AdminCommand {

	/**
	 * 注册 {@code //addset} 命令。
	 * Registers the {@code //addset} command.
	 */
	public AddSet() {
		super("addset");
	}

	/**
	 * 执行添加套装：解析玩家与套装 ID 后逐件发放。
	 * Executes add-set: resolves player and set id, then grants each part.
	 *
	 * @param params 参数：玩家名（可选）、套装 ID / optional player name, item-set id
	 */
	@Override
	public void execute(Player player, String... params) {
		if (params.length == 0 || params.length > 2) {
			onFail(player, null);
			return;
		}

		int itemSetId = 0;
		Player receiver = null;

		try {
			itemSetId = Integer.parseInt(params[0]);
			receiver = player;
		}
		catch (NumberFormatException e) {
			receiver = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(Util.convertName(params[0]));

			if (receiver == null) {
				PacketSendUtility.sendMessage(player, "Could not find a player by that name.");
				return;
			}

			try {
				itemSetId = Integer.parseInt(params[1]);
			}
			catch (NumberFormatException ex) {

				PacketSendUtility.sendMessage(player, "You must give number to itemset ID.");
				return;
			}
			catch (Exception ex2) {
				PacketSendUtility.sendMessage(player, "Occurs an error.");
				return;
			}
		}

		ItemSetTemplate itemSet = DataManager.ITEM_SET_DATA.getItemSetTemplate(itemSetId);
		if (itemSet == null) {
			PacketSendUtility.sendMessage(player, "ItemSet does not exist with id " + itemSetId);
			return;
		}

		if (receiver.getInventory().getFreeSlots() < itemSet.getItempart().size()) {
			PacketSendUtility
				.sendMessage(player, "Inventory needs at least " + itemSet.getItempart().size() + " free slots.");
			return;
		}

		for (ItemPart setPart : itemSet.getItempart()) {
			long count = ItemService.addItem(receiver, setPart.getItemid(), 1);

			if (count != 0) {
				PacketSendUtility.sendMessage(player, "Item " + setPart.getItemid() + " couldn't be added");
				return;
			}
		}

		PacketSendUtility.sendMessage(player, "Item Set added successfully");
		PacketSendUtility.sendMessage(receiver, "admin gives you an item set");
	}

	/**
	 * 参数错误时输出 {@code //addset} 用法。
	 * Prints {@code //addset} usage on invalid arguments.
	 *
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //addset <player> <itemset ID>");
		PacketSendUtility.sendMessage(player, "syntax //addset <itemset ID>");
	}
}