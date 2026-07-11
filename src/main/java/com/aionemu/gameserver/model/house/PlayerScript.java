package com.aionemu.gameserver.model.house;

import com.aionemu.commons.taskmanager.AbstractLockManager;

/**
 * 玩家 Script，用于房屋相关逻辑。
 * Player Script for house logic.
 */

public final class PlayerScript extends AbstractLockManager {
	public PlayerScript() {
	}

	public PlayerScript(byte[] compressedBytes, int uncompressedSize) {
		this.compressedBytes = compressedBytes;
		this.uncompressedSize = uncompressedSize;
	}

	private int uncompressedSize = -1;
	private byte[] compressedBytes = null;

	/** 返回 uncompressed size / Returns the uncompressed size */
	public int getUncompressedSize() {
		return uncompressedSize;
	}

	/** 返回 compressed bytes / Returns the compressed bytes */
	public byte[] getCompressedBytes() {
		return compressedBytes;
	}

	/** 设置数据。 / Sets the data. */
	public void setData(byte[] compressedBytes, int uncompressedSize) {
		writeLock();
		this.compressedBytes = compressedBytes;
		this.uncompressedSize = uncompressedSize;
		writeUnlock();
	}
}
