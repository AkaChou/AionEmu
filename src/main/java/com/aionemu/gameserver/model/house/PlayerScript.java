package com.aionemu.gameserver.model.house;

import com.aionemu.commons.taskmanager.AbstractLockManager;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 玩家 Script，用于房屋相关逻辑。
 * Player Script for house logic.
 */

@NoArgsConstructor
public final class PlayerScript extends AbstractLockManager {
	public PlayerScript(byte[] compressedBytes, int uncompressedSize) {
		this.compressedBytes = compressedBytes;
		this.uncompressedSize = uncompressedSize;
	}

	/** 返回 uncompressed size / Returns the uncompressed size */
	@Getter
	private int uncompressedSize = -1;
	/** 返回 compressed bytes / Returns the compressed bytes */
	@Getter
	private byte[] compressedBytes = null;

	/** 设置数据。 / Sets the data. */
	public void setData(byte[] compressedBytes, int uncompressedSize) {
		writeLock();
		this.compressedBytes = compressedBytes;
		this.uncompressedSize = uncompressedSize;
		writeUnlock();
	}
}
