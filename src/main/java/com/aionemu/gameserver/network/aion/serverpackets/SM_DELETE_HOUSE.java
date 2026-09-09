package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 删除房屋包：按地址通知客户端移除房屋。
 * Server packet that removes a house from the client by address.
 */
@AllArgsConstructor
public class SM_DELETE_HOUSE extends AionServerPacket {

	private final int address;

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(address);
	}
}
