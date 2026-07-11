package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 客户端心跳（Ping）应答服务端包。
 * Server packet that answers a client ping.
 *
 * @author dragoon112
 */
public class SM_PING_RESPONSE extends AionServerPacket {

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(0x04);
	}
}
