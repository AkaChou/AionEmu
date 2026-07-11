package com.aionemu.gameserver.network.aion.serverpackets;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 通知客户端房屋租金支付结果的服务端包。
 * Server packet that notifies the client of a house rent payment result.
 */
public class SM_HOUSE_PAY_RENT extends AionServerPacket {
	private int weeksPaid;

	/**
	 * 构造房屋租金支付结果包。
	 * Creates a house rent payment result packet.
	 *
	 * @param weeksPaid 已支付周数 / number of weeks paid
	 */
	public SM_HOUSE_PAY_RENT(int weeksPaid) {
		this.weeksPaid = weeksPaid;
	}

	@Override
	protected void writeImpl(AionConnection con) {
		writeC(0);
		writeC(weeksPaid);
	}
}
