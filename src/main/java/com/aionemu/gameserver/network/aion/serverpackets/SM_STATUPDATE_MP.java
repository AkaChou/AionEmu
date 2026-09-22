package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端更新当前/最大 MP 值。
 * Server packet updating current and max MP values on the client.
 * @author Luno
 */
@AllArgsConstructor
public class SM_STATUPDATE_MP extends AionServerPacket {

	private final int currentMp;
	private final int maxMp;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(currentMp);
		writeD(maxMp);
	}
}
