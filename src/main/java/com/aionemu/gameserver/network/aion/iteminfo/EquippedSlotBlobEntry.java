package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 当前装备槽位 Blob。
 * 对所有可装备物品发送；已装备时写槽位 ID，否则写 0。
 * Blob sent for all equipable items.
 * Writes the equipped slot id when worn, otherwise 0.
 *
 * @author -Nemesiss-
 * @modified Rolandas
 */
public class EquippedSlotBlobEntry extends ItemBlobEntry {

	/**
	 * 构造装备槽位 Blob 条目。
	 * Constructs an equipped-slot blob entry.
	 */
	EquippedSlotBlobEntry() {
		super(ItemBlobType.EQUIPPED_SLOT);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		Item item = ownerItem;

		writeQ(buf, item.isEquipped() ? item.getEquipmentSlot() : 0x00);
	}

	/**
	 * 返回本 Blob 负载的字节长度。
	 * Returns the payload size of this blob in bytes.
	 */
	@Override
	public int getSize() {
		return 8;
	}
}
