package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端下发军团加入申请目标信息（军团 ID 与名称）的服务端包。
 * Server packet delivering target legion info (legion id and name) for a join request to the client.
 */
@AllArgsConstructor
public class SM_LEGION_REQUEST_INFO extends AionServerPacket {

	private final int legionId;
	private final String legionName;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(legionId);
		writeS(legionName);
	}
}
