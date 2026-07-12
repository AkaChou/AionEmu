package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;

/**
 * GM 指令：按物品 ID 与数量发放物品。
 * GM command handler that grants items by template id and quantity.
 *
 * @author Antraxx
 */
public final class CmdWishId extends AbstractGMHandler {

	/**
	 * 创建处理器并立即按 ID 发放物品。
	 * Creates the handler and immediately grants items by id.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * {@code quantity itemId}。
	 */
	public CmdWishId(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 解析数量与物品 ID，向目标玩家发放物品（可堆叠/赋能分支）。
	 * Parses quantity and item id, then grants the item to the target (stackable/authorize branches).
	 */
	public void run() {
		Player t = target != null ? target : admin;

		String[] p = params.split(" ");
		if (p.length != 2) {
			PacketSendUtility.sendMessage(admin, "not enough parameters");
			return;
		}

		Integer qty = Integer.parseInt(p[0]);
		Integer itemId = Integer.parseInt(p[1]);
		ItemTemplate it = DataManager.ITEM_DATA.getItemTemplate(itemId);
		long count =0;
		if (qty > 0 && itemId > 0) {
			if (it == null) {
				PacketSendUtility.sendMessage(admin, "Item id is incorrect: " + itemId);
			} else {
				if (it.getMaxAuthorize()!=0)
					count = ItemService.addItemAndEnchant(t, it.getTemplateId(),1, qty);
				else if (it.isStackable())
					count = ItemService.addItem(t, itemId, qty);
				else 
					count = ItemService.addItem(t, itemId, 1);
				if (count == 0) {
					PacketSendUtility.sendMessage(admin,
							"You successfully gave " + qty + " x [item:" + itemId + "] to " + t.getName() + ".");
				} else {
					PacketSendUtility.sendMessage(admin, "Item couldn't be added");
				}
			}
		}
	}
}
