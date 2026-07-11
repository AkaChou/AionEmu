package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.items.ItemSlot;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 武器槽位信息 Blob。
 * 写入武器可装备的主/副槽位掩码；双手武器会合并掩码。
 * Blob sent for weapons.
 * Writes primary/secondary slot masks; two-hand weapons combine masks.
 *
 * @author -Nemesiss-
 * @modified Rolandas
 */
public class WeaponInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造武器槽位 Blob 条目。
	 * Constructs a weapon-slot blob entry.
	 */
	WeaponInfoBlobEntry() {
		super(ItemBlobType.SLOTS_WEAPON);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		Item item = ownerItem;

		ItemSlot[] slots = ItemSlot.getSlotsFor(item.getItemTemplate().getItemSlot());
		if (slots.length == 1) {
			writeQ(buf, slots[0].getSlotIdMask());
			writeQ(buf, item.hasFusionedItem() ? 0x00 : 0x02);
			return;
		}
		if (item.getItemTemplate().isTwoHandWeapon()) {
			writeQ(buf, slots[0].getSlotIdMask() | slots[1].getSlotIdMask());
			writeQ(buf, 0);
		} else {
			writeQ(buf, slots[0].getSlotIdMask());
			writeQ(buf, slots[1].getSlotIdMask());
		}
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
