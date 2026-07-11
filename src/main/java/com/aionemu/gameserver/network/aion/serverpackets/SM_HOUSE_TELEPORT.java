package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端将玩家传送至指定房屋的服务端包。
 * Server packet notifying the client to teleport a player to a given house.
 */
public class SM_HOUSE_TELEPORT extends AionServerPacket {
	int address;
	int playerId;

	/**
	 * 使用房屋地址与玩家 ID 构造传送包。
	 * Creates a teleport packet for the given house address and player id.
	 *
	 * house address id
	 * player object id
	 */
	public SM_HOUSE_TELEPORT(int houseAddress, int playerId) {
		this.address = houseAddress;
		this.playerId = playerId;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(address);
		writeD(playerId);
	}
}
