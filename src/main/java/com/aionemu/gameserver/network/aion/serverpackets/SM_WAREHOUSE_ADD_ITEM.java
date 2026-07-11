package com.aionemu.gameserver.network.aion.serverpackets;

import java.util.Collections;
import java.util.List;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemAddType;

/**
 * 仓库新增物品的服务端包。
 * Server packet for adding an item into a warehouse.
 *
 * @author kosyachok
 * @author -Nemesiss-
 */
public class SM_WAREHOUSE_ADD_ITEM extends AionServerPacket {

	private int warehouseType;
	private List<Item> items;
	private Player player;
	private ItemAddType addType;

	/**
	 * @param item          新增物品 / added item
	 * warehouse type
	 * 玩家 / player
	 */
	public SM_WAREHOUSE_ADD_ITEM(Item item, int warehouseType, Player player) {
		this.player = player;
		this.warehouseType = warehouseType;
		this.items = Collections.singletonList(item);
		this.addType = ItemAddType.ALL_SLOT;
	}

	/**
	 * @param item          新增物品 / added item
	 * warehouse type
	 * 玩家 / player
	 * add type
	 */
	public SM_WAREHOUSE_ADD_ITEM(Item item, int warehouseType, Player player, ItemAddType addType) {
		this(item, warehouseType, player);
		this.addType = addType;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(warehouseType);
		writeH(addType.getMask());
		writeH(items.size());
		for (Item item : items) {
			writeItemInfo(item);
		}
	}

	/**
	 * 写出仓库物品信息。
	 * Writes warehouse item info.
	 *
	 * item
	 */
	private void writeItemInfo(Item item) {
		ItemTemplate itemTemplate = item.getItemTemplate();
		writeD(item.getObjectId());
		writeD(itemTemplate.getTemplateId());
		writeC(0); // some item info (4 - weapon, 7 - armor, 8 - rings, 17 - bottles)
		writeNameId(itemTemplate.getNameId());
		ItemInfoBlob itemInfoBlob = ItemInfoBlob.getFullBlob(player, item);
		itemInfoBlob.writeMe(getBuf());
		writeH((int) (item.getEquipmentSlot() & 0xFFFF));
	}
}
