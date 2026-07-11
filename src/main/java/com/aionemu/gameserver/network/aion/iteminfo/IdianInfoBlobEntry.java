package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.model.items.IdianStone;
import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 艾帝安（抛光）充能信息 Blob。
 * 写入艾帝安石当前抛光充能值。
 * Blob for Idian (polish) charge info.
 * Writes the current polish charge of the Idian stone.
 *
 * @author Ranastic
 */
public class IdianInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造艾帝安信息 Blob 条目。
	 * Constructs an Idian-info blob entry.
	 */
	IdianInfoBlobEntry() {
		super(ItemBlobType.IDIAN_INFO);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		IdianStone stone = ownerItem.getIdianStone();
		writeD(buf, stone == null ? 0 : stone.getPolishCharge());
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
