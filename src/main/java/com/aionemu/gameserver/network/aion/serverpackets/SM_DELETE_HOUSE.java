package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 删除房屋包：按地址通知客户端移除房屋。
 * Server packet that removes a house from the client by address.
 */
public class SM_DELETE_HOUSE extends AionServerPacket {

	private int address;

	public SM_DELETE_HOUSE(int address) {
		this.address = address;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(address);
	}
}
