package com.aionemu.gameserver.network.aion.iteminfo;

import java.nio.ByteBuffer;

import com.aionemu.gameserver.network.aion.iteminfo.ItemInfoBlob.ItemBlobType;

/**
 * 包装/拆封次数信息 Blob。
 * 已包装时写剩余可包装次数；未包装时写负数；否则写 0。
 * Blob for wrap/unwrap count info.
 * Writes remaining wrap count when packed, a negative count when unpacked, otherwise 0.
 *
 * @author Ranastic
 */
public class WrappInfoBlobEntry extends ItemBlobEntry {

	/**
	 * 构造包装信息 Blob 条目。
	 * Constructs a wrap-info blob entry.
	 */
	WrappInfoBlobEntry() {
		super(ItemBlobType.WRAPP_INFO);
	}

	/**
	 * 将本 Blob 的具体内容写入缓冲区。
	 * Writes this blob's concrete payload into the buffer.
	 */
	@Override
	public void writeThisBlob(ByteBuffer buf) {
		if (ownerItem.getItemTemplate().getWrappableCount() > 0 && ownerItem.isPacked()) {
			writeC(buf, ownerItem.getWrappableCount());
		} else if (!ownerItem.isPacked()) {
			writeC(buf, ownerItem.getWrappableCount() * -1);
		} else {
			writeC(buf, 0);
		}
	}

	/**
	 * 返回本 Blob 负载的字节长度。
	 * Returns the payload size of this blob in bytes.
	 */
	@Override
	public int getSize() {
		return 1;
	}
}
