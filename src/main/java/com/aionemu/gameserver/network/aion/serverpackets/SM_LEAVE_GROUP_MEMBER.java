package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端离开队伍（组队成员离开）的服务端包。
 * Server packet notifying the client that a group member has left the party.
 */
public class SM_LEAVE_GROUP_MEMBER extends AionServerPacket {
	@Override
	protected void writeImpl(AionConnection con) {
		writeD(0x00);
		writeC(0x00);
		writeD(0xFF);
		writeD(0x00);
		writeH(0x00);
	}
}
