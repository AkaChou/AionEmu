package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 通知客户端玩家获得或失去房屋的服务端包。
 * Server packet that notifies the client of a player acquiring or losing a house.
 */
@AllArgsConstructor
public class SM_HOUSE_ACQUIRE extends AionServerPacket {

	private final int playerId;
	private final int address;
	private final boolean acquire;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerId);
		writeD(address);
		writeD(acquire ? 1 : 0);
	}
}
