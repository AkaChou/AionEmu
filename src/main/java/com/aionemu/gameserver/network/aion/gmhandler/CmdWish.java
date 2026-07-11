package com.aionemu.gameserver.network.aion.gmhandler;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.npc.NpcTemplate;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.services.item.ItemService;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;

import com.aionemu.commons.utils.collections.IntObjectHashMap;

/**
 * GM 指令：按名称生成物品（可附强化）或召唤 NPC。
 * GM command handler that wishes an item by name (with enchant) or spawns an NPC by name.
 *
 * @author Kill3r
 */
public final class CmdWish extends AbstractGMHandler {

	/**
	 * 创建处理器并立即执行许愿逻辑。
	 * Creates the handler and immediately runs the wish logic.
	 *
	 * @param admin 执行指令的管理员 / the admin executing the command
	 * @param params NPC 名，或 {@code 物品名 强化等级} / NPC name, or {@code itemName enchantLevel}
	 */
	public CmdWish(Player admin, String params) {
		super(admin, params);
		run();
	}

	/**
	 * 单参数时按名称召唤 NPC；双参数时按名称发放并强化物品。
	 * With one param spawns an NPC by name; with two params grants an enchanted item by name.
	 */
	public void run() {
		String[] p = params.split(" ");
		if (p.length != 2) {

			String npcName = params;
			IntObjectHashMap<NpcTemplate> npcTemp = DataManager.NPC_DATA.getNpcData();

			float x = admin.getX();
			float y = admin.getY();
			float z = admin.getZ();
			byte heading = admin.getHeading();
			int worldId = admin.getWorldId();

			for (NpcTemplate nTemp : npcTemp.values()) {
				if (nTemp.getNamedesc() != null && nTemp.getNamedesc().equalsIgnoreCase(npcName)) {
					SpawnTemplate spawn = SpawnEngine.addNewSpawn(worldId, nTemp.getTemplateId(), x, y, z, heading, 0);
					VisibleObject visibleObject = SpawnEngine.spawnObject(spawn, admin.getInstanceId());
					PacketSendUtility.sendMessage(admin, " spawned (ID:" + nTemp.getTemplateId() + ")");
				}
			}
			return;
		}
		// 完美工作 / WORKING PERFECTLY
		IntObjectHashMap<ItemTemplate> itemTemp = DataManager.ITEM_DATA.getItemData();
		String[] itemN = params.split(" ");

		String itemName = itemN[0];
		Integer enchant = Integer.parseInt(itemN[1]);

		for (ItemTemplate it : itemTemp.values()) {
			if (it.getNamedesc() != null && it.getNamedesc().equalsIgnoreCase(itemName)) {
				int maxauthorize = it.getMaxAuthorize();
				if ((maxauthorize!=0)&&(enchant>maxauthorize))
					ItemService.addItemAndEnchant(admin, it.getTemplateId(),1, maxauthorize);
				else
					ItemService.addItemAndEnchant(admin, it.getTemplateId(),1, enchant);
			}
		}
	}
}
