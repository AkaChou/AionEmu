package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步黄金竞技场（Arena of Gold）排名信息的服务端包。
 * Server packet synchronizing Arena of Gold ranking information to the client.
 *
 * @author wanke
 */
public class SM_ARENA_OF_GOLD_RANK extends AionServerPacket {
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(1);
		writeD(0);
		writeD(125);
		writeD(0);
		writeD(0);
		writeD(0);
		writeD(0);
	}
}
