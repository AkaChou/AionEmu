package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步竞价/出价相关数据。
 * Server packet synchronizing bid-related data to the client.
 */
@AllArgsConstructor
public class SM_RECEIVE_BIDS extends AionServerPacket {

	int unk;

	protected void writeImpl(AionConnection con) {
		writeD(unk);
	}
}
