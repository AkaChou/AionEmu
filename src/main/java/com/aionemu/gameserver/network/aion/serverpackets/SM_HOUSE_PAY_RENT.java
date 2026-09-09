package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;
import lombok.AllArgsConstructor;

/**
 * 通知客户端房屋租金支付结果的服务端包。
 * Server packet that notifies the client of a house rent payment result.
 */
@AllArgsConstructor
public class SM_HOUSE_PAY_RENT extends AionServerPacket {
	private final int weeksPaid;

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(0);
		writeC(weeksPaid);
	}
}
