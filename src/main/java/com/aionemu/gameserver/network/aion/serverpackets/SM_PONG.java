package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 对客户端 Ping 的应答包，用于连接保活。
 * Server packet responding to a client ping for connection keep-alive.
 */
public class SM_PONG extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(0x00);
		writeC(0x00);
	}
}
