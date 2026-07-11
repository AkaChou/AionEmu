package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端下发军团加入申请目标信息（军团 ID 与名称）的服务端包。
 * Server packet delivering target legion info (legion id and name) for a join request to the client.
 */
public class SM_LEGION_REQUEST_INFO extends AionServerPacket {

	private int legionId;
	private String legionName;

	/**
	 * 构造军团申请目标信息包。
	 * Creates a packet with target legion request info.
	 *
	 * legion id
	 * legion name
	 */
	public SM_LEGION_REQUEST_INFO(int legionId, String legionName) {
		this.legionId = legionId;
		this.legionName = legionName;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(legionId);
		writeS(legionName);
	}
}
