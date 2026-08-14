package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 翅膀槽位信息 Blob。
 * 写入翅膀可装备的槽位掩码（无副槽位）。
 * Blob sent for wings.
 * Writes the wing slot mask (no secondary slot).
 *
 * @author -Nemesiss-
 * @modified Rolandas
 */
public class WingInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造翅膀槽位 Blob 条目。
	 * Constructs a wing-slot blob entry.
	 */
	WingInfoBlobEntry() {
		super(ItemBlobType.SLOTS_WING);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		Item item = ownerItem;

		writeQ(buf, ItemSlot.getSlotFor(item.getItemTemplate().getItemSlot()).getSlotIdMask());
		writeQ(buf, 0); // 无副槽位 / no secondary slot
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
