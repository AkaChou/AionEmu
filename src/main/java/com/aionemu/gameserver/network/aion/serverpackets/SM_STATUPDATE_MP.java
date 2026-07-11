package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端更新当前/最大 MP 值。
 * Server packet updating current and max MP values on the client.
 *
 * @author Luno
 */
public class SM_STATUPDATE_MP extends AionServerPacket {

	private int currentMp;
	private int maxMp;

	/**
	 * 使用给定参数构造 SM_STATUPDATE_MP 包。
	 * Creates a SM_STATUPDATE_MP packet with the given parameters.
	 *
	 * current mp
	 * max mp
	 */
	public SM_STATUPDATE_MP(int currentMp, int maxMp) {
		this.currentMp = currentMp;
		this.maxMp = maxMp;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(currentMp);
		writeD(maxMp);
	}
}
