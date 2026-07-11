package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.Equipment;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.storage.Storage;
import com.aionemu.gameserver.services.item.ItemPacketService;
import com.aionemu.gameserver.model.gameobjects.PersistentState;

/**
 * GM 指令：设置物品强化等级或赋能等级。
 * GM command handler that sets an item's enchant or authorize level.
 *
 * @author Angry Catster
 */
public class CmdSetEnchantCount extends AbstractGMHandler {

	/**
	 * 创建处理器并立即设置强化/赋能等级。
	 * Creates the handler and immediately sets enchant/authorize levels.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params 物品对象 ID、强化增量、赋能增量 / item object id, enchant delta, authorize delta
	 */
	public CmdSetEnchantCount(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 在背包或装备中定位物品并增加强化/赋能等级。
	 * Locates the item in inventory or equipment and increases enchant/authorize level.
	 */
	private void run() {
		String[] p = params.split(" ");
		Integer objid = Integer.parseInt(p[0]);
		Integer enchlvl = Integer.parseInt(p[1]);
		Integer authorizelvl = Integer.parseInt(p[2]);
		
		Storage inventory = admin.getInventory();
		Equipment equip = admin.getEquipment();
		
		Item targetItem = inventory.getItemByObjId(objid);
		if (targetItem==null)
			targetItem = equip.getEquippedItemByObjId(objid);
		if ((targetItem != null)){
			if ((targetItem.getItemTemplate().getMaxEnchantLevel()!=0) && (targetItem.getEnchantLevel()<=254))
				targetItem.setEnchantLevel(targetItem.getEnchantLevel() + enchlvl);
			else if ((targetItem.getItemTemplate().getMaxAuthorize()!=0))
				targetItem.setAuthorize(targetItem.getAuthorize() + authorizelvl);

			equip.setPersistentState(PersistentState.UPDATE_REQUIRED);
			inventory.setPersistentState(PersistentState.UPDATE_REQUIRED);
			admin.getGameStats().updateStatsAndSpeedVisually();
			ItemPacketService.updateItemAfterInfoChange(admin, targetItem);
		}
	}
}
