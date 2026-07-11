package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 向客户端同步玩家个人商店名称。
 * Server packet synchronizing a player's private-store name to the client.
 *
 * @author Simple
 */
public class SM_PRIVATE_STORE_NAME extends AionServerPacket {

	/**
	 * Private store Information *
	 */
	private int playerObjId;
	private String name;

	/**
	 * 使用给定参数构造 SM_PRIVATE_STORE_NAME 包。
	 * Creates a SM_PRIVATE_STORE_NAME packet with the given parameters.
	 *
	 * player object id
	 * name
	 */
	public SM_PRIVATE_STORE_NAME(int playerObjId, String name) {
		this.playerObjId = playerObjId;
		this.name = name;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(playerObjId);
		writeS(name);
	}
}
