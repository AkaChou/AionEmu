package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 通知客户端将玩家传送至指定房屋的服务端包。
 * Server packet notifying the client to teleport a player to a given house.
 */
@AllArgsConstructor
public class SM_HOUSE_TELEPORT extends AionServerPacket {
	int address;
	int playerId;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(address);
		writeD(playerId);
	}
}
