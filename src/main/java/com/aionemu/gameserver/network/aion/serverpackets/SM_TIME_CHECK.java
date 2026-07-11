package com.aionemu.gameserver.network.aion.serverpackets;

import java.sql.Timestamp;

import com.aionemu.gameserver.network.aion.AionConnection;
import com.aionemu.gameserver.network.aion.AionServerPacket;

/**
 * 客户端时间校验应答服务端包（服务器时间戳 + 客户端纳秒时间）。
 * Server packet for client time-check response (server timestamp + client nano time).
 *
 * @author -Nemesiss-
 */
public class SM_TIME_CHECK extends AionServerPacket {

	// 别被空类骗了 :D / Don't be fooled with empty class :D
	// 此包仅发送操作码，无内容 / This packet is just sending opcode, without any content

	// 1.5.x 发送 8 字节 / 1.5.x sending 8 bytes

	private int nanoTime;
	private int time;
	private Timestamp dateTime;

	/**
	 * @param nanoTime 客户端上报的纳秒时间 / client-reported nano time
	 */
	public SM_TIME_CHECK(int nanoTime) {
		this.dateTime = new Timestamp((new java.util.Date()).getTime());
		this.nanoTime = nanoTime;
		this.time = (int) dateTime.getTime();
	}

	/**
	 * {@inheritDoc}
	 */

	@Override
	protected void writeImpl(AionConnection con) {
		writeD(time);
		writeD(nanoTime);

	}
}
