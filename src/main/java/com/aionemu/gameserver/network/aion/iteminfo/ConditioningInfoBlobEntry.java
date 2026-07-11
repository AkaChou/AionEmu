package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 调校（充能）信息 Blob。
 * 写入物品当前充能点数。
 * Blob that sends conditioning (charge) info.
 * Writes the item's current charge points.
 *
 * @author -Nemesiss-
 * @modified Rolandas
 */
public class ConditioningInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造调校信息 Blob 条目。
	 * Constructs a conditioning-info blob entry.
	 */
	ConditioningInfoBlobEntry() {
		super(ItemBlobType.CONDITIONING_INFO);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		Item item = ownerItem;

		writeD(buf, item.getChargePoints());
	}

	/**
	 * 返回本 Blob 负载的字节长度。
	 * Returns the payload size of this blob in bytes.
	 */
	@Override
	public int getSize() {
		return 4;
	}
}
