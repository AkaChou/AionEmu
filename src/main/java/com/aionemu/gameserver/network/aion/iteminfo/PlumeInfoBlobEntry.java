package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 羽饰槽位信息 Blob。
 * 写入羽饰可装备的槽位掩码。
 * Blob for plume slot info.
 * Writes slot masks the plume can be equipped to.
 *
 * @author Ranastic (Encom)
 */
public class PlumeInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造羽饰信息 Blob 条目。
	 * Constructs a plume-info blob entry.
	 */
	PlumeInfoBlobEntry() {
		super(ItemBlobType.PLUME_INFO);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		Item item = ownerItem;
		writeQ(buf, ItemSlot.getSlotFor(item.getItemTemplate().getItemSlot()).getSlotIdMask());
		writeQ(buf, ItemSlot.getSlotFor(item.getItemTemplate().getItemSlot()).getSlotIdMask());
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
