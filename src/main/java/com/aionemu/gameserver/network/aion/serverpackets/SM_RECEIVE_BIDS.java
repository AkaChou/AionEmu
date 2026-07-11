package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步竞价/出价相关数据。
 * Server packet synchronizing bid-related data to the client.
 */
public class SM_RECEIVE_BIDS extends AionServerPacket {

	int unk;

	/**
	 * 使用给定参数构造 SM_RECEIVE_BIDS 包。
	 * Creates a SM_RECEIVE_BIDS packet with the given parameters.
	 *
	 * @param unk 未知字段 / unknown field
	 */
	public SM_RECEIVE_BIDS(int unk) {
		this.unk = unk;
	}

	protected void writeImpl(AionConnection con) {
		writeD(unk);
	}
}
