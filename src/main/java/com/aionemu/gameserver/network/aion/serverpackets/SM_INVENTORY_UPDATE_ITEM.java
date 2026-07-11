package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;
import com.aionemu.gameserver.services.item.ItemPacketService.ItemUpdateType;

/**
 * 向客户端同步背包中单个物品状态变更的服务端包。
 * Server packet that synchronizes a single inventory item update to the client.
 */
public class SM_INVENTORY_UPDATE_ITEM extends AionServerPacket {
	private final Player player;
	private final Item item;
	private final ItemUpdateType updateType;

	/**
	 * 以默认使用消耗类型构造物品更新包。
	 * Creates an item-update packet with the default item-use decrease type.
	 *
	 * target player
	 * @param item 待更新物品 / item to update
	 */
	public SM_INVENTORY_UPDATE_ITEM(Player player, Item item) {
		this(player, item, ItemUpdateType.DEC_ITEM_USE);
	}

	/**
	 * 以指定更新类型构造物品更新包。
	 * Creates an item-update packet with the given update type.
	 *
	 * target player
	 * @param item 待更新物品 / item to update
	 * @param updateType 物品更新类型 / item update type
	 */
	public SM_INVENTORY_UPDATE_ITEM(Player player, Item item, ItemUpdateType updateType) {
		this.player = player;
		this.item = item;
		this.updateType = updateType;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		ItemTemplate itemTemplate = item.getItemTemplate();
		writeD(item.getObjectId());
		writeNameId(itemTemplate.getNameId());
		ItemInfoBlob itemInfoBlob;
		switch (updateType) {
		case EQUIP_UNEQUIP:
			itemInfoBlob = new ItemInfoBlob(player, item);
			itemInfoBlob.addBlobEntry(ItemBlobType.EQUIPPED_SLOT);
			break;
		case CHARGE:
			itemInfoBlob = new ItemInfoBlob(player, item);
			itemInfoBlob.addBlobEntry(ItemBlobType.CONDITIONING_INFO);
		default:
			itemInfoBlob = ItemInfoBlob.getFullBlob(player, item);
			break;
		}
		itemInfoBlob.writeMe(getBuf());
		if (updateType.isSendable()) {
			writeH(updateType.getMask());
		}
	}
}
