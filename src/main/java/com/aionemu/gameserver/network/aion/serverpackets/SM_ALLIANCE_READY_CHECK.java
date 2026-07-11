package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步联盟就绪检查（Ready Check）结果的服务端包。
 * Server packet synchronizing alliance ready-check status to the client.
 *
 * @author Sarynth (Thx Rhys2002 for Packets)
 */
public class SM_ALLIANCE_READY_CHECK extends AionServerPacket {

	private int playerObjectId;
	private int statusCode;

	/**
	 * 构造就绪检查状态包。
	 * Creates a ready-check status packet.
	 *
	 * player object id
	 * @param statusCode 就绪状态码 / ready status code
	 */
	public SM_ALLIANCE_READY_CHECK(int playerObjectId, int statusCode) {
		this.playerObjectId = playerObjectId;
		this.statusCode = statusCode;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerObjectId);
		writeC(statusCode);
	}
}
