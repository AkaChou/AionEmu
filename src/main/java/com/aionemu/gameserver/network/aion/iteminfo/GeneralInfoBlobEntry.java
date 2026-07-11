package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 通用物品信息 Blob。
 * 几乎所有物品都会附带；对不可装备物品通常是唯一块，对可装备物品则为最后一块。
 * 包含掩码、数量、制作者、消失时间、临时交易时间与封印状态等。
 * General item-info blob sent with almost all items.
 * It is the only block for non-equipable items, and the last block for equipable ones.
 * Includes mask, count, creator, expire time, temporary exchange time, and seal state.
 *
 * @author -Nemesiss-
 * @modified Rolandas
 */
public class GeneralInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造通用信息 Blob 条目。
	 * Constructs a general-info blob entry.
	 */
	GeneralInfoBlobEntry() {
		super(ItemBlobType.GENERAL_INFO);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		Item item = ownerItem;
		writeH(buf, item.getItemMask(owner));
		writeQ(buf, item.getItemCount());
		writeS(buf, item.getItemCreator());// Creator name
		writeC(buf, 0);
		writeD(buf, item.getExpireTimeRemaining()); // Disappears time
		writeD(buf, 0);
		writeD(buf, item.getTemporaryExchangeTimeRemaining());
		writeH(buf, item.getUnSeal());
		writeD(buf, 0);
	}

	/**
	 * 返回本 Blob 负载的字节长度（含制作者名字节数）。
	 * Returns the payload size of this blob in bytes (including creator name).
	 */
	@Override
	public int getSize() {
		return 29 + ownerItem.getItemCreator().length() * 2 + 2;
	}
}
