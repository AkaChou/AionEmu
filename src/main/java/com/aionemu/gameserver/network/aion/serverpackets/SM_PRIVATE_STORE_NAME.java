package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 向客户端同步玩家个人商店名称。
 * Server packet synchronizing a player's private-store name to the client.
 *
 * @author Simple
 */
@AllArgsConstructor
public class SM_PRIVATE_STORE_NAME extends AionServerPacket {

	/**
	 * Private store Information *
	 */
	private final int playerObjId;
	private final String name;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerObjId);
		writeS(name);
	}
}
