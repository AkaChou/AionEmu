package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端更新当前 DP（神圣点数）值。
 * Server packet updating the current DP (divine points) value on the client.
 *
 * @author Luno
 */
public class SM_STATUPDATE_DP extends AionServerPacket {

	private int currentDp;

	/**
	 * 使用给定参数构造 SM_STATUPDATE_DP 包。
	 * Creates a SM_STATUPDATE_DP packet with the given parameters.
	 *
	 * current dp
	 */
	public SM_STATUPDATE_DP(int currentDp) {
		this.currentDp = currentDp;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeH(currentDp);
	}
}
