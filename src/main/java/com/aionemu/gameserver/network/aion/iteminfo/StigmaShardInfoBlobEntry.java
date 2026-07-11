package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 尘晶碎片信息 Blob。
 * 当前写入占位数据。
 * Blob for stigma-shard info.
 * Currently writes a placeholder value.
 *
 * @author Rolandas
 */
public class StigmaShardInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造尘晶碎片 Blob 条目。
	 * Constructs a stigma-shard blob entry.
	 */
	public StigmaShardInfoBlobEntry() {
		super(ItemBlobType.STIGMA_SHARD);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		writeD(buf, 0);
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
