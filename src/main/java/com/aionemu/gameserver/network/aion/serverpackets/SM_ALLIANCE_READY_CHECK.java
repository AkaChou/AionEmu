package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步联盟就绪检查（Ready Check）结果的服务端包。
 * Server packet synchronizing alliance ready-check status to the client.
 * @author Sarynth (Thx Rhys2002 for Packets)
 */
@AllArgsConstructor
public class SM_ALLIANCE_READY_CHECK extends AionServerPacket {

	private final int playerObjectId;
	private final int statusCode;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerObjectId);
		writeC(statusCode);
	}
}
