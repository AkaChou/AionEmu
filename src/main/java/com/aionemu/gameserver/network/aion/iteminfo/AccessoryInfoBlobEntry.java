package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 饰品（戒指、耳环、腰带等）槽位信息 Blob。
 * 写入该物品可装备的主/副槽位掩码。
 * Blob sent for accessory items (ring, earring, waist, etc.).
 * Writes primary/secondary slot masks the item can be equipped to.
 *
 * @author -Nemesiss-
 * @modified Rolandas
 */
public class AccessoryInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造饰品槽位 Blob 条目。
	 * Constructs an accessory-slot blob entry.
	 */
	AccessoryInfoBlobEntry() {
		super(ItemBlobType.SLOTS_ACCESSORY);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		Item item = ownerItem;

		ItemSlot[] slots = ItemSlot.getSlotsFor(item.getItemTemplate().getItemSlot());
		writeQ(buf, slots[0].getSlotIdMask());
		writeQ(buf, slots.length > 1 ? slots[1].getSlotIdMask() : 0);
	}

	/**
	 * 返回本 Blob 负载的字节长度。
	 * Returns the payload size of this blob in bytes.
	 */
	@Override
	public int getSize() {
		return 16;
	}
}
