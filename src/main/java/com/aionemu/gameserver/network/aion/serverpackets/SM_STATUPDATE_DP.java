package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端更新当前 DP（神圣点数）值。
 * Server packet updating the current DP (divine points) value on the client.
 * @author Luno
 */
@AllArgsConstructor
public class SM_STATUPDATE_DP extends AionServerPacket {

	private final int currentDp;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeH(currentDp);
	}
}
