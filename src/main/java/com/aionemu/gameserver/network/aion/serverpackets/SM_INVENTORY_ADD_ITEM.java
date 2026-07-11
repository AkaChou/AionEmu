package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.items.storage.ItemStorage;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType;

/**
 * 向客户端发送背包新增物品的服务端包。
 * Server packet that sends newly added inventory items to the client.
 */
public class SM_INVENTORY_ADD_ITEM extends AionServerPacket {
	private final List<Item> items;
	private Player player;
	private ItemAddType addType;

	/**
	 * 以默认收集类型构造新增物品包。
	 * Creates an add-item packet with the default item-collect type.
	 *
	 * @param items 新增物品列表 / list of newly added items
	 * target player
	 */
	public SM_INVENTORY_ADD_ITEM(List<Item> items, Player player) {
		this.player = player;
		this.items = items;
		this.addType = ItemAddType.ITEM_COLLECT;
	}

	/**
	 * 以指定添加类型构造新增物品包。
	 * Creates an add-item packet with the given add type.
	 *
	 * @param items 新增物品列表 / list of newly added items
	 * target player
	 * @param addType 物品添加类型 / item add type
	 */
	public SM_INVENTORY_ADD_ITEM(List<Item> items, Player player, ItemAddType addType) {
		this(items, player);
		this.addType = addType;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		int mask = addType.getMask();
		if (addType == ItemAddType.ITEM_COLLECT && items.size() == 1
				&& items.get(0).getEquipmentSlot() != ItemStorage.FIRST_AVAILABLE_SLOT) {
			mask = ItemAddType.PARTIAL_WITH_SLOT.getMask();
		}
		writeC(mask);
		writeC(0);
		writeH(items.size());
		for (Item item : items) {
			writeItemInfo(item);
		}
	}

	private void writeItemInfo(Item item) {
		ItemTemplate itemTemplate = item.getItemTemplate();
		writeD(item.getObjectId());
		writeD(itemTemplate.getTemplateId());
		writeNameId(itemTemplate.getNameId());
		ItemInfoBlob itemInfoBlob = ItemInfoBlob.getFullBlob(player, item);
		itemInfoBlob.writeMe(getBuf());
		writeH(item.isEquipped() ? 255 : item.getEquipmentSlotInteger());
		writeC(0x00);
	}
}
