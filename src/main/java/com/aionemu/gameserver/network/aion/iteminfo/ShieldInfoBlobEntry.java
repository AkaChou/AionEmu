package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 盾牌槽位信息 Blob。
 * 写入盾牌可装备的槽位掩码。
 * Blob sent for shields.
 * Writes slot masks the shield can be equipped to.
 *
 * @author -Nemesiss-
 * @modified Rolandas
 */
public class ShieldInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造盾牌槽位 Blob 条目。
	 * Constructs a shield-slot blob entry.
	 */
	ShieldInfoBlobEntry() {
		super(ItemBlobType.SLOTS_SHIELD);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		Item item = ownerItem;

		writeQ(buf, ItemSlot.getSlotFor(item.getItemTemplate().getItemSlot()).getSlotIdMask());
		writeQ(buf, 0); // no secondary slot
		writeC(buf, item.getItemTemplate().isItemDyePermitted() ? 1 : 0);
		writeC(buf, (item.getItemColor() & 0xFF0000) >> 16);
		writeC(buf, (item.getItemColor() & 0xFF00) >> 8);
		writeC(buf, item.getItemColor() & 0xFF);
	}

	/**
	 * 返回本 Blob 负载的字节长度。
	 * Returns the payload size of this blob in bytes.
	 */
	@Override
	public int getSize() {
		return 20;
	}
}
