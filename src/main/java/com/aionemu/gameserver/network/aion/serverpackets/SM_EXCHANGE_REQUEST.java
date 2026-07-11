package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 交易请求包：向目标展示发起交易的玩家名。
 * Exchange request packet: shows the requester name to the target.
 *
 * @author -Avol-
 */
public class SM_EXCHANGE_REQUEST extends AionServerPacket {

	private String receiver;

	public SM_EXCHANGE_REQUEST(String receiver) {
		this.receiver = receiver;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeS(receiver);
	}
}
