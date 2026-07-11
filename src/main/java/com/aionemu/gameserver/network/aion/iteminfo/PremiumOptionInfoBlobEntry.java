package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 高级随机选项信息 Blob。
 * 写入加成编号、随机次数等字段。
 * Blob for premium/random option info.
 * Writes bonus number, random count, and related fields.
 *
 * @author Rolandas
 */
public class PremiumOptionInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造高级选项 Blob 条目。
	 * Constructs a premium-option blob entry.
	 */
	public PremiumOptionInfoBlobEntry() {
		super(ItemBlobType.PREMIUM_OPTION);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		writeC(buf, ownerItem.getBonusNumber());
		writeC(buf, ownerItem.getRandomCount());
		writeC(buf, 0);
	}

	/**
	 * 返回本 Blob 负载的字节长度。
	 * Returns the payload size of this blob in bytes.
	 */
	@Override
	public int getSize() {
		return 3;
	}
}
